(ns lcmap.mastodon.cljs.core
(:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [lcmap.mastodon.cljs.http :as http]
            [lcmap.mastodon.cljc.ard  :as ard]
            [lcmap.mastodon.cljs.dom  :as dom]
            [lcmap.mastodon.cljc.util :as util]
            [clojure.string :as string]
            [cljs.core.async :refer [<! >! chan]]
            [cljs.reader :refer [read-string]]))


(def ard-data-chan (chan 1))       ;; channel holding ARD resource locations
(def ard-to-ingest-chan (chan 1))  ;; channel used to handle ARD to ingest
(def ingest-status-chan (chan 1))  ;; channel used to handle ingest status

(def ard-miss-atom (atom {})) ;; atom containing list of ARD not yet ingested
(def iwd-miss-atom (atom {})) ;; atom containing list of ARD only found in IWDS

(def ard-resource-atom  (atom {:path ""}))   ;; atom containing ARD host
(def iwds-resource-atom (atom {:path ""}))   ;; atom containing IWDS host
(def ingest-resource-atom (atom {:path ""})) ;; atom containing Ingest host

(defn keep-host-info
  "Function for persisting ARD and IWDS host information within Atoms

   ^String :ard-host: name of ARD host
   ^String :iwds-host: name of IWDS host"
  [ard-host iwds-host ingest-host]
  (swap! ard-resource-atom  assoc :path ard-host)
  (swap! iwds-resource-atom assoc :path iwds-host)
  (swap! ingest-resource-atom assoc :path ingest-host))

(defn report-assessment
  [ard-channel dom-map & [dom-func]]
  (go
    (let [ard-status (<! ard-channel)
          dom-update (or dom-func dom/update-for-ard-check)
          report-map (hash-map :ingested-count (:ingested ard-status)
                               :ard-missing-count (count (:missing ard-status))
                               :iwds-missing   []
                               :dom-map dom-map)]
      (util/log (str "ARD Status Report: " report-map))
      (swap! ard-miss-atom assoc :tifs (:missing ard-status))
      (dom-update report-map (count (:missing ard-status))))))

(defn ^:export assess-ard
  [ard-host tile-id bsy-div ing-btn ing-ctr mis-ctr iwds-miss-list error-ctr error-div & [ard-req-fn]]
  (let [ard-request-handler    (or ard-req-fn http/get-request)
        ard-inventory-resource (util/ard-url-format ard-host  tile-id)
        dom-map  (hash-map :ing-ctr ing-ctr :mis-ctr mis-ctr :bsy-div bsy-div :ing-btn ing-btn :iwds-miss-list iwds-miss-list :error-ctr error-ctr :error-div error-div)]
    (report-assessment ard-data-chan dom-map) ;; park func on ard-data-chan to update dom
    (go (>! ard-data-chan (<! (ard-request-handler ard-inventory-resource))))))

(defn make-chipmunk-requests 
  "Function which makes the requests to an lcmap-chipmunk instance for ARD ingest. This
   is parked on the ard-to-ingest-chan channel, waiting for a list of URLs for ARD to 
   be placed on that channel.

   ^Core.Async Channel :ingest-channel: The channel holding the list of ARD to ingest
   ^String             :iwds-resource:  The IWDS instance to post ingest requests to
   ^Core.Async Channel :status-channel: The channel holding the ingest request status
   ^String             :busy-div:       The name of the div containing the busy image
   ^String             :ingesting-div:  The name of the div displaying the ARD being ingested

   Returns Core.Async Channel. Request responses are placed on the status-channel"
  [ingest-channel iwds-resource status-channel busy-div ingesting-div partition-level]
  (go
    (let [tifs (<! ingest-channel)
          partifs (partition partition-level partition-level "" tifs)
          ard-resource (str (:path @ard-resource-atom) "/bulk-ingest")]
      (doseq [t partifs]
        (>! status-channel (<! (http/post-request ard-resource {"urls" (string/join "," t) }))))
      (dom/update-for-ingest-completion busy-div ingesting-div))))

(defn ingest-status-handler 
  "Function parked on the ingest-status-chan channel. Handles successful and
   unsuccessful ingest responses from an lcmap-chipmunk instance.

   ^Core.Async Channel :status-channel: The channel this function is parked on
   ^hash-map           :counter-map:    Hash map of DOM element names

   Returns a Core.Async Channel, while updating the DOM to reflect ingest actions."
  [status-channel counter-map]
  (go-loop []
    (let [response (<! status-channel)
          status   (:status response)
          body     (:body response)]
      (if (= 200 status)
          (do (util/log "status is 200")
              (doseq [ard_resp body]
                (if (= 200 (first (vals ard_resp)))
                  (dom/update-for-ingest-success counter-map)
                  (do (util/log (str "status is NOT 200, ingest failed. message: " body))
                      (dom/update-for-ingest-fail counter-map)))))
          (do (util/log (str "non-200 response: " response)))))
    (recur)))

(defn ^:export ingest 
  "Top level function for initiating the ARD ingest process.  Pulls list of ARD to ingest
   from the ard-miss-atom atom, updates the DOM to reflect work to be done, parks the 
   make-chipmunk-requests function on the ard-to-ingest-chan, and then puts the list
   of ARD onto the ard-to-ingest-chan.

   ^String :inprogress-div: Name of div indicating number of ARD waiting to be ingested
   ^String :missing-div:    Name of div indicating number of missing ARD
   ^String :ingested-div:   Name of div indicating number of ARD already ingested
   ^String :busy-div:       Name of div containing the busy image
   ^String :error-div:      Name of div indicating ingest error count
   ^String :ingesting-div:  Name of div containing name of ARD being ingested

   Returns Core.Async channel. Parks ingest-status-handler on ingest-status-chan. 
   Parks make-chipmunk-requests on ard-to-ingest-chan, and puts ard-sources on the
   ard-to-ingest-chan."
  [inprogress-div missing-div ingested-div busy-div error-div ingesting-div par-level]
  (let [ard-resource-path    (:path @ard-resource-atom)
        iwds-resource-path   (:path @iwds-resource-atom)
        ingest-resource-path (:path @ingest-resource-atom)
        ard-sources          (:tifs @ard-miss-atom)
        counter-map          (hash-map :progress inprogress-div :missing missing-div :ingested ingested-div :error error-div)
        ard-count            (count ard-sources)
        partition-level      (read-string par-level)]

    (dom/update-for-ingest-start (:progress counter-map) ard-count) 
    (ingest-status-handler ingest-status-chan counter-map) 
    (make-chipmunk-requests ard-to-ingest-chan iwds-resource-path ingest-status-chan busy-div ingesting-div partition-level)
    (go (>! ard-to-ingest-chan ard-sources))))

