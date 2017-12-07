(ns lcmap.mastodon.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.set :as set]
            [lcmap.mastodon.http :as http]
            [lcmap.mastodon.ard  :as ard]
            [lcmap.mastodon.dom  :as dom]
            [lcmap.mastodon.util :as util]
            [cljs.core.async :refer [<! >! chan pipe]]))


(def ard-reqt-chan (chan 1)) ;; channel used for holding ARD request response
(def ard-data-chan (chan 1)) ;; channel holding formatted ARD data 



(def rpt-chan (chan 30)) ;; channel used for reporting ARD status
(def ing-chan (chan 30)) ;; channel used to handle ARD to ingest
(def sts-chan (chan 30)) ;; channel used to handle ingest status

(def ard-miss-atom (atom {})) ;; atom containing list of ARD not yet ingested
(def iwd-miss-atom (atom {})) ;; atom containing list of ARD only found in IDWS

(def ard-host-atom (atom {:path ""})) ;; atom containing ARD host
(def iwd-host-atom (atom {:path ""})) ;; atom containing IDW host

(defn hv-map
  "Helper function.
   Return map for :h and :v given
   a tileid of hhhvvv e.g 052013

   ^String :id: 6 character string representing tile id
   ^Expression :regx: Optional expression to parse tile id

   Returns map with keys :h & :v"
  [id & [regx]]
  (hash-map :h (first (re-seq (or regx #"[0-9]{3}") id)) 
            :v (last  (re-seq (or regx #"[0-9]{3}") id)))
)

(defn ard-url-format
  "URL generation function for requests to an ARD file access server

   ^String :host:    Host name
   ^String :tile-id: Tile ID
  "
  [host tile-id]
  (let [hvm (hv-map tile-id)]
    (str host "/" (:h hvm) (:v hvm) "/"))
)

(defn idw-url-format
  "URL generation function for requests to an LCMAP-Chipmunk instance

   ^String :host:    lcmap-chipmunk instance host
   ^String :tile-id: Tile ID
  "
  ;; $ curl -XGET http://localhost:5656/inventory?tile=027009
  [host tile-id]
  (str host "/inventory?tile=" tile-id)
)

(defn diff-handler [idw-c idw-url idw-rqt busy-div ingest-btn ing-ctr mis-ctr & [dom-func rchan]]
  (go
    (let [ard-tifs (<! idw-c)
          iwd-tifs (ard/iwds-tifs (<! (idw-rqt idw-url)))
          ard-only (set/difference ard-tifs iwd-tifs)
          iwd-only (set/difference iwd-tifs ard-tifs)
          ingested (set/intersection ard-tifs iwd-tifs)
          dom-updt (or dom-func dom/update-for-ard-check)
          rptr-chn (or rchan rpt-chan)
          rptr-map (hash-map :ing-ctr ing-ctr
                             :mis-ctr mis-ctr
                             :ing-cnt (count ingested)
                             :mis-cnt (count ard-only)
                             :iwd-cnt (count iwd-only)     
                             :bsy-div busy-div
                             :ing-btn ingest-btn)]
          (util/log (str "rptr-map: " ard-tifs))
          (swap! ard-miss-atom assoc :tifs ard-only)
          (swap! iwd-miss-atom assoc :tifs iwd-only)
          (dom-updt rptr-map)
          ;;(>! rptr-chn rptr-map)
    ))
)

(defn inventory-diff
  "Diff function, comparing what source files the ARD source has available, 
   and what sources have been ingested into the data warehouse for a specific tile

   ^String :ardh: ARD Host
   ^String :idwh: IDW Host
   ^String :hv:   Tile ID
   ^String :reg:  Region

   Returns vector (things only in ARD, things only in IDW)
  "
  [ard-host idw-host tile-id region bsy-div ing-btn ing-ctr mis-ctr & [ard-req-fn idw-req-fn div-fnc]]
    (let [ard-rqt (or ard-req-fn http/get-request)
          idw-rqt (or idw-req-fn http/get-request)
          ard-url (ard-url-format ard-host tile-id)
          idw-url (idw-url-format idw-host tile-id)
          bsy-fnc (or div-fnc dom/show-div)]

         ;; turn on busy signal
         (bsy-fnc bsy-div)
         ;; put ard and idw resource paths on atoms
         (swap! ard-host-atom assoc :path ard-url)
         (swap! iwd-host-atom assoc :path (str idw-host "/inventory"))
         ;; park func on ard-data-chan, when an item is put on this chan, do the diff work
         (diff-handler ard-data-chan idw-url idw-rqt bsy-div ing-btn ing-ctr mis-ctr)
         (pipe ard-reqt-chan ard-data-chan)
         ;; make the call for ARD, put filtered response on ard-reqt-chan
         (go (>! ard-reqt-chan (-> (<! (ard-rqt ard-url))
                                   (util/collect-map-values :name :type "file")
                                   (util/with-suffix "tar")
                                   (ard/expand-tars)))))
)

(defn ingest-handler 
  [ing-c idw-path sts-c]
  (go
    (let [tifs (<! ing-c)]
      (util/log (str "tifs: " tifs))
      (doseq [t tifs]
        (util/log (str "ingest tif: " t))
        (>! sts-c  (<! (http/post-request idw-path {"url" t}))))))
)

(defn status-handler 
  [sts-c]
  (go-loop []
    (let [resp (<! sts-c)]
      (util/log (str "response status" (:status resp)))
    )
  )
)

(defn ingest-fmt [tif rpath]
  (let [tar (ard/tar-name tif)]
    (str rpath tar "/" tif))
)

(defn ingest-req []
  (let [ard-path (:path @ard-host-atom)
        iwd-path (:path @iwd-host-atom)
        paths (map #(ingest-fmt % ard-path) (:tifs @ard-miss-atom))] 
    
    ;; park ingest func on ingest channel (ing-chan)
    (ingest-handler ing-chan iwd-path sts-chan)
    ;; park status func on status channel (sts-chan)
    (status-handler sts-chan)
    ;; put list of ard to ingest on ingest channel
    (go (>! ing-chan paths))

    (doseq [i paths]
      (util/log (str "some paths: " i))))
)


