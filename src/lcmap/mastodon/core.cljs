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

(def ing-chan (chan 30)) ;; channel used to handle ARD to ingest
(def sts-chan (chan 30)) ;; channel used to handle ingest status

(def ard-miss-atom (atom {})) ;; atom containing list of ARD not yet ingested
(def iwd-miss-atom (atom {})) ;; atom containing list of ARD only found in IDWS

(def ard-host-atom (atom {:path ""})) ;; atom containing ARD host
(def iwd-host-atom (atom {:path ""})) ;; atom containing IDW host

(defn keep-host-info 
  [ard-host idw-host]
  ;; could put these values in a hidden form on the index page as well
  (swap! ard-host-atom assoc :path ard-host)
  (swap! iwd-host-atom assoc :path (str idw-host "/inventory"))
)

(defn hv-map
  "Helper function.
   Return map for :h and :v given
   a tileid of hhhvvv e.g 052013

   ^String :id: 6 character string representing tile id
   ^Expression :regx: Optional expression to parse tile id

   Returns map with keys :h & :v"
  [id & [regx]]
  (let [match (re-seq (or regx #"[0-9]{3}") id)]
    (hash-map :h (first match)
              :v (last match)))
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

(defn compare-contrast [idw-c idw-url idw-rqt dom-map & [dom-func]]
  (go
    (let [ard-tifs (<! idw-c)
          iwd-tifs (ard/iwds-tifs (<! (idw-rqt idw-url)))
          ard-rprt (ard/ard-iwds-report ard-tifs iwd-tifs)
          dom-updt (or dom-func dom/update-for-ard-check)
          rptr-map (hash-map :ingested-count     (count (:ingested ard-rprt))
                             :ard-missing-count  (count (:ard-only ard-rprt))
                             :iwds-missing-count (count (:iwd-only ard-rprt))
                             :dom-map dom-map)]

          (util/log (str "rptr-map: " rptr-map))
          (swap! ard-miss-atom assoc :tifs (:ard-only ard-rprt))
          (swap! iwd-miss-atom assoc :tifs (:iwd-only ard-rprt))
          (dom-updt rptr-map)
    ))
)

(defn construct-diff
  "Diff function, comparing what source files the ARD source has available, 
   and what sources have been ingested into the data warehouse for a specific tile

   ^String :ardh: ARD Host
   ^String :idwh: IDW Host
   ^String :hv:   Tile ID
   ^String :reg:  Region

   Returns vector (things only in ARD, things only in IDW)
  "
  [ard-host idw-host tile-id region bsy-div ing-btn ing-ctr mis-ctr & [ard-req-fn idw-req-fn]]
    (let [ard-rqt (or ard-req-fn http/get-request)
          idw-rqt (or idw-req-fn http/get-request)
          ard-url (ard-url-format ard-host tile-id)
          idw-url (idw-url-format idw-host tile-id)
          dom-map (hash-map :ing-ctr ing-ctr :mis-ctr mis-ctr :bsy-div bsy-div :ing-btn ing-btn )]

         (keep-host-info ard-url idw-host)
         (compare-contrast ard-data-chan idw-url idw-rqt dom-map)
         (pipe ard-reqt-chan ard-data-chan)
         (go (>! ard-reqt-chan (-> (<! (ard-rqt ard-url))
                                   (util/collect-map-values :name :type "file")
                                   (util/with-suffix "tar")
                                   (ard/expand-tars)))))
)

(defn make-chipmunk-requests 
  [ing-c idw-path sts-c]
  (go
    (let [tifs (<! ing-c)]
      (util/log (str "tifs: " tifs))
      (doseq [t tifs]
        (util/log (str "ingest tif: " t))
        (>! sts-c  (<! (http/post-request idw-path {"url" t}))))))
)

(defn ingest []
  (let [ard-path (:path @ard-host-atom)
        iwd-path (:path @iwd-host-atom)
        paths (map #(ard/tif-path % ard-path) (:tifs @ard-miss-atom))] 
    
    (make-chipmunk-requests ing-chan iwd-path sts-chan)
    (go (>! ing-chan paths)))
)


