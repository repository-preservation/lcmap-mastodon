(ns lcmap.mastodon.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.set :as set]
            [lcmap.mastodon.http :as http]
            [lcmap.mastodon.ard  :as ard]
            [lcmap.mastodon.dom  :as dom]
            [lcmap.mastodon.util :as util]
            [cljs.core.async :refer [<! >! chan pipe]]))


(def ard-data-chan (chan 1)) ;; channel holding formatted ARD data 

(def ard-to-ingest-chan (chan 1)) ;; channel used to handle ARD to ingest
(def ingest-status-chan (chan 10000)) ;; channel used to handle ingest status

(def ard-miss-atom (atom {})) ;; atom containing list of ARD not yet ingested
(def iwd-miss-atom (atom {})) ;; atom containing list of ARD only found in IDWS

(def ard-resource-atom  (atom {:path ""})) ;; atom containing ARD host
(def iwds-resource-atom (atom {:path ""})) ;; atom containing IWDS host

(defn keep-host-info 
  [ard-host idw-host]
  ;; could put these values in a hidden form on the index page as well
  (swap! ard-resource-atom  assoc :path ard-host)
  (swap! iwds-resource-atom assoc :path (str idw-host "/inventory"))
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

(defn compare-contrast [ard-channel iwds-url iwds-request dom-map & [dom-func]]
  (go
    (let [ard-tifs   (<! ard-channel)
          iwds-tifs  (ard/iwds-tifs (<! (iwds-request iwds-url)))
          ard-report (ard/ard-iwds-report ard-tifs iwds-tifs)
          dom-update (or dom-func dom/update-for-ard-check)
          report-map (hash-map :ingested-count     (count (:ingested ard-report))
                               :ard-missing-count  (count (:ard-only ard-report))
                               :iwds-missing-count (count (:iwd-only ard-report))
                               :dom-map dom-map)]

          (util/log (str "ARD Status Report: " report-map))
          (swap! ard-miss-atom assoc :tifs (:ard-only ard-report))
          (swap! iwd-miss-atom assoc :tifs (:iwd-only ard-report))
          (dom-update report-map)
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
  [ard-host iwds-host tile-id region bsy-div ing-btn ing-ctr mis-ctr & [ard-req-fn idw-req-fn]]
    (let [ard-request-handler  (or ard-req-fn http/get-request)
          iwds-request-handler (or idw-req-fn http/get-request)
          ard-resource  (ard-url-format ard-host  tile-id)
          iwds-resource (idw-url-format iwds-host tile-id)
          dom-map  (hash-map :ing-ctr ing-ctr :mis-ctr mis-ctr :bsy-div bsy-div :ing-btn ing-btn )]

         (keep-host-info ard-resource iwds-resource)
         (compare-contrast ard-data-chan iwds-resource iwds-request-handler dom-map) ;; park compare-contrast on ard-data-chan
         (go (>! ard-data-chan (-> (<! (ard-request-handler ard-resource))           ;; request ard resources, place formatted and 
                                   (util/collect-map-values :name :type "file")      ;; filtered response on ard-data-chan
                                   (util/with-suffix "tar")
                                   (ard/expand-tars)))))
)

(defn make-chipmunk-requests 
  [ingest-channel iwds-resource status-channel]
  (go
    (let [tifs (<! ingest-channel)]
      (dom/inc-counter-div "ardinprogress-counter" (count tifs))
      (util/log (str "tifs: " tifs))
      (doseq [t tifs]
        (util/log (str "ingest tif: " t))
        (>! status-channel  (<! (http/post-request iwds-resource {"url" t}))))
      ;; turn off busy div
      (dom/hide-div "busydiv")
))
)

(defn ingest-status-handler 
  [status-channel]
  (go-loop []
    (let [response (<! status-channel)]
      (util/log (str "response status: " (:status response))))
    (recur)
  )
)

(defn ingest 
  [counter-div]
  (let [ard-resource-path   (:path @ard-resource-atom)
        iwds-resource-path  (:path @iwds-resource-atom)
        ard-sources         (map #(ard/tif-path % ard-resource-path) (:tifs @ard-miss-atom))] 
    ;; set in-progress to number of ard to be ingested
    (util/log (str "made it here: " (str counter-div ) ))
    (dom/reset-counter-divs ["ardinprogress-counter"])
    ;; (util/log (str "ard-sources count: " (count ard-sources)))
    ;; (dom/inc-counter-div counter-div (count ard-sources))
    (ingest-status-handler ingest-status-chan)
    ;; have func parked on ingest-status-chan do the following:
    ;; 1 - decrement the value in the in-progress div
    ;; 2 - increment the value in the ingested div   
    (make-chipmunk-requests ard-to-ingest-chan iwds-resource-path ingest-status-chan)
    (go (>! ard-to-ingest-chan ard-sources)))
)


