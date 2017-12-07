(ns lcmap.mastodon.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.set :as set]
            [lcmap.mastodon.http :as http]
            [lcmap.mastodon.ard  :as ard]
            [lcmap.mastodon.dom  :as dom]
            [lcmap.mastodon.util :as util]
            [cljs.core.async :refer [<! >! chan]]))


(def ard-chan (chan 30)) ;; channel used for determing ARD status
(def idw-chan (chan 30)) ;; random channel 
(def rpt-chan (chan 30)) ;; channel used for reporting ARD status
(def ing-chan (chan 30)) ;; channel used to handle ARD to ingest
(def sts-chan (chan 30)) ;; channel used to handle ingest status

(def ard-miss-atom (atom {})) ;; atom containing list of ARD not yet ingested
(def idw-miss-atom (atom {})) ;; atom containing list of ARD only found in IDWS

(def ard-host-atom (atom {:path ""})) ;; atom containing ARD host
(def idw-host-atom (atom {:path ""})) ;; atom containing IDW host

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

(defn idw-handler [idw-c idw-url idw-rqt busy-div ingest-btn ing-ctr mis-ctr & [dom-func rchan]]
  (go
    (let [ard-tifs (<! idw-c)
          idw-resp (<! (idw-rqt idw-url))
          idw-tifs (set (util/collect-map-values idw-resp :source))
          ard-only (set/difference ard-tifs idw-tifs)
          idw-only (set/difference idw-tifs ard-tifs)
          ingested (set/intersection ard-tifs idw-tifs)
          dom-updt (or dom-func dom/update-for-ard-check)
          rptr-chn (or rchan rpt-chan)
          rptr-map (hash-map :ing-ctr ing-ctr
                             :mis-ctr mis-ctr
                             :ing-cnt (count ingested)
                             :mis-cnt (count ard-only)
                             :bsy-div busy-div
                             :ing-btn ingest-btn)]
          (util/log (str "ard-tifs: " ard-tifs))
          (util/log (str "idw-tifs: " idw-tifs))
          (util/log (str "ard-only: " ard-only))
          (swap! ard-miss-atom assoc :tifs ard-only)
          (swap! idw-miss-atom assoc :tifs idw-only)
          (util/log (str "missing count: " (count (:tifs (deref ard-miss-atom)))))
          (dom-updt rptr-map)
          (>! rptr-chn rptr-map)
    )
  )

)

(defn ard-status-check
  [ard-c idw-c]
  (go
    (let [ard-tars (<! ard-c)
          ard-tifs (set (flatten (map ard/ard-manifest ard-tars)))]

        ;; put idw response on idw-chan, make sure theres a func parked on idw-c
        ;; that handles the diff work of the original ard-status-check
        (>! idw-c ard-tifs)
        ;;(util/log (str "ard-tifs: " ard-tifs))
        )
  )
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
         (swap! idw-host-atom assoc :path (str idw-host "/inventory"))
         ;; park func on idw-chan, when an item is put on this chan, doe the diff work
         (idw-handler idw-chan idw-url idw-rqt bsy-div ing-btn ing-ctr mis-ctr)
         ;; park func on ard-chan, when an ard response is received, put it on the idw-chan
         (ard-status-check ard-chan idw-chan)
         ;; put items on ard-chan
         (go                 
           (>! ard-chan (-> (<! (ard-rqt ard-url))
                            (util/collect-map-values :name :type "file")
                            (util/with-suffix "tar")))))
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
        idw-path (:path @idw-host-atom)
        paths (map #(ingest-fmt % ard-path) (:tifs @ard-miss-atom))] 
    
    ;; park ingest func on ingest channel (ing-chan)
    (ingest-handler ing-chan idw-path sts-chan)
    ;; park status func on status channel (sts-chan)
    (status-handler sts-chan)
    ;; put list of ard to ingest on ingest channel
    (go (>! ing-chan paths))

    (doseq [i paths]
      (util/log (str "some paths: " i))))
)


