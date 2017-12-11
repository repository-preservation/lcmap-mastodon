(ns lcmap.mastodon.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.set :as set]
            [lcmap.mastodon.http :as http]
            [lcmap.mastodon.ard  :as ard]
            [lcmap.mastodon.dom  :as dom]
            [lcmap.mastodon.util :as util]
            [cljs.core.async :refer [<! >! chan pipe]]))


(def ard-data-chan (chan 1))         ;; channel holding ARD resource locations
(def ard-to-ingest-chan (chan 1))    ;; channel used to handle ARD to ingest
(def ingest-status-chan (chan 100))  ;; channel used to handle ingest status
(def ingest-error-chan  (chan 1000)) ;; channel used to handle ingest errors

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

(defn compare-iwds 
  "Compare the available ARD resources against whats available from IWDS. This
   function is parked on the ard-data-chan channel. When an ARD request response
   lands on the ard-data-chan, make an inventory request to the IWDS. Categorize
   the results, put them in Atoms, and update the DOM.

   ^Core.Async Channel :ard-channel:
   ^String             :iwds-url:
   ^Function           :iwds-request:
   ^Hash Map           :dom-map:

   Returns a Core.Async channel. Organizes ARD by status, placing lists
   in appropriate Atoms."
  [ard-channel iwds-url iwds-request dom-map & [dom-func]]
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
          (dom-update report-map)))
)

(defn assess-ard
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
         (compare-iwds ard-data-chan iwds-resource iwds-request-handler dom-map) ;; park compare-iwds on ard-data-chan
         (go (>! ard-data-chan (-> (<! (ard-request-handler ard-resource))       ;; request ard resources, place formatted 
                                   (util/collect-map-values :name :type "file")  ;; response on ard-data-chan
                                   (util/with-suffix "tar")
                                   (ard/expand-tars)))))
)

(defn make-chipmunk-requests 
  "Function which makes the requests to a Chipmunk instance for ARD ingest. This
   is parked on the ard-to-ingest-chan Channel, waiting for a list of URLs for
   at ARD to be placed on that channel.

   ^Core.Async Channel :ingest-channel:
   ^String             :iwds-resource:
   ^Core.Async Channel :status-channel:

   Returns Core.Async Channel. Request responses are placed on the status-channel"
  [ingest-channel iwds-resource status-channel busy-div]
  (go
    (let [tifs (<! ingest-channel)]
      (doseq [t tifs]
        (util/log (str "ingest ard: " t))
        (>! status-channel  (<! (http/post-request iwds-resource {"url" t}))))
      (dom/hide-div busy-div)))
)

(defn ingest-status-handler 
  "Function parked on the ingest-status-chan Channel. Handles successful and
   unsuccessful ingest responses from an lcmap-chipmunk instance.

   ^Core.Async Channel :status-channel:

   Returns a Core.Async Channel. Logging to the Developer Console in the browser
   and updating the DOM to reflect ingest actions."
  [status-channel counter-map]
  (go-loop []
    (let [response (<! status-channel)
          status (:status response)]
      (if (= 200 status)
          (do (util/log "status is 200")
              (dom/update-for-ingest-success counter-map))
          (do (util/log (str "status is NOT 200, ingest failed. message: " (:body response))))))
    (recur))
)

(defn ingest 
  "Top level function for initiating the ARD ingest process.  Pulls list of ARD to ingest
   from the ard-miss-atom Atom, updates the DOM to reflect work to be done, parks the 
   make-chipmunk-requests function on the ard-to-ingest-chan, and then puts the list
   of ARD onto the ard-to-ingest-chan.

   ^String :inprogress-div:
   ^String :missing-div:
   ^String :ingested-div:
   ^String :busy-div:

   Returns Core.Async channel
  "
  [inprogress-div missing-div ingested-div busy-div]
  (let [ard-resource-path  (:path @ard-resource-atom)
        iwds-resource-path (:path @iwds-resource-atom)
        ard-sources        (map #(ard/tif-path % ard-resource-path) (:tifs @ard-miss-atom))
        counter-map        (hash-map :progress inprogress-div :missing missing-div :ingested ingested-div)
        ard-count          (count ard-sources)]

    (dom/update-for-ingest-start (:progress counter-map) ard-count)
    ;; park ingest-status-handler on ingest-status-chan
    (ingest-status-handler ingest-status-chan counter-map) 
    ;; park make-chipmunk-requests on ard-to-ingest-chan
    (make-chipmunk-requests ard-to-ingest-chan iwds-resource-path ingest-status-chan busy-div)
    ;; put ard-sources on ard-to-ingest-chan
    (go (>! ard-to-ingest-chan ard-sources)))
)


