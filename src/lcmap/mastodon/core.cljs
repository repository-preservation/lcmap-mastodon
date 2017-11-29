(ns lcmap.mastodon.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [clojure.set :as set]
            [lcmap.mastodon.http :as http]
            [lcmap.mastodon.ard  :as ard]
            [lcmap.mastodon.dom  :as dom]
            [lcmap.mastodon.util :as util]
            [cljs.core.async :refer [<! >! chan]]))

(def ard-chan (chan 1))
(def ard-miss-atom (atom []))
(def idw-miss-atom (atom []))

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
    (str "/" host "/" (:h hvm) "/" (:v hvm) "/"))
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

(defn ard-status-check 
  "Check whether available ARD has been ingested. If it hasn't
   place it in the requested Atom.

   ^ core.async.chan :ard-c:
   ^ String :idw-url:
   ^ Func :idw-rqt:"
  [ard-c idw-url idw-rqt busy-div ingest-btn]
  (dom/reset-counter-divs ["ardingested-counter" "ardmissing-counter"])
  (go
    (let [tars (<! ard-c)
          tifs (set (flatten (map ard/ard-manifest tars))) 
          idw-resp (:result (<! (idw-rqt idw-url)))
          idw-tifs (set (util/collect-map-values idw-resp :source)) 
          ard-only (set/difference tifs idw-tifs)
          idw-only (set/difference idw-tifs tifs)
          ingested (set/intersection tifs idw-tifs)]

          (swap! ard-miss-atom conj ard-only)
          (swap! idw-miss-atom conj idw-only)
          (dom/inc-counter-div "ardingested-counter" (count ingested))
          (dom/inc-counter-div "ardmissing-counter" (count ard-only))
          (util/log (str "missing count: "  (count (first (deref ard-miss-atom)))))
          (dom/hide-div busy-div)
          (dom/enable-btn ingest-btn)))
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
  [ard-host idw-host tile-id region & [ard-req-fn idw-req-fn]]
    (let [ard-rqt (or ard-req-fn http/get-request)
          idw-rqt (or idw-req-fn http/get-request)
          ard-url (ard-url-format ard-host tile-id)
          idw-url (idw-url-format idw-host tile-id)
          bsy-div "busydiv"
          ing-btn "chpsubmit"]

         ;; turn on busy signal
         (dom/show-div bsy-div)
         ;; park functions on ard-chan
         (ard-status-check ard-chan idw-url idw-rqt bsy-div ing-btn)
         ;; put items on ard-chan
         (go                 
           (>! ard-chan (util/collect-map-values (<! (ard-rqt ard-url)) :name :type "file"))))
)

(defn ingest-req []

  (doseq [i @ard-miss-atom]
    (util/log (str "ingest: " i)))

)


