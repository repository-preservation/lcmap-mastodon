(ns mastodon.cljs.core
(:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [mastodon.cljs.http :as http]
            [mastodon.cljc.ard  :as ard]
            [mastodon.cljs.dom  :as dom]
            [mastodon.cljc.util :as util]
            [clojure.string :as string]
            [cljs.core.async :refer [<! >! chan]]
            [cljs.reader :refer [read-string]]))

(def ard-data-chan (chan 1))       ;; channel holding ARD resource locations
(def ard-to-ingest-chan (chan 1))  ;; channel used to handle ARD to ingest
(def ingest-status-chan (chan 1))  ;; channel used to handle ingest status

(def ard-miss-atom (atom {})) ;; atom containing list of ARD not yet ingested
(def iwd-miss-atom (atom {})) ;; atom containing list of ARD only found in IWDS

(defn report-assessment
  "Handle DOM update, store names of non-ingested ARD, based on Tile status check."
  [ard-channel dom-map & [dom-func dom-content hide-fn]]
  (go
    (let [ard-status (<! ard-channel)
          dom-update (or dom-func dom/update-for-ard-check)
          dom-set-fn (or dom-content dom/set-div-content)
          dom-hide   (or hide-fn dom/hide-div)
          ard-body   (:body ard-status)
          report-map (hash-map :ingested-count (:ingested ard-body)
                               :ard-missing-count (count (:missing ard-body))
                               :iwds-missing [] ; dependent on single iwds query
                               :dom-map dom-map)
          ard-error (:error ard-body)]

      (if (nil? ard-error)
        (do (util/log (str "ARD Status Report: " report-map))
            (swap! ard-miss-atom assoc :tifs (:missing ard-body))
            (dom-update report-map (count (:missing ard-body))))
        (do (util/log (str "Error reaching services: " (:body ard-status)))
            (dom-hide "busydiv")
            (dom-set-fn "error-container" [(str "Error reaching ARD server: " ard-error)]))))))

(defn make-chipmunk-requests 
  "Handle requests to lcmap-chipmunk for ARD ingest"
  [ingest-channel status-channel ard-host busy-div ingesting-div inprogress-div partition-level & [dom-func]]
  (go
    (let [tifs (<! ingest-channel)
          partifs (partition partition-level partition-level "" tifs)
          ard-resource (str ard-host "/bulk-ingest")
          dom-update (or dom-func dom/update-for-ingest-completion)]
      (doseq [t partifs]
        (>! status-channel (<! (http/post-request ard-resource {"urls" (string/join "," t) }))))
      (dom-update busy-div ingesting-div inprogress-div))))

(defn ingest-status-handler 
  "Handle ingest request responses."
  [status-channel counter-map]
  (go-loop []
    (let [response (<! status-channel)
          status   (:status response)
          body     (:body response)
          tifs     (-> body (#(reduce conj %)) (keys) (#(map name %)))]
      (if (= 200 status)
          (do (util/log "status is 200")
              (util/log (str "ingested: " tifs))
              (dom/set-div-content "ingesting-list" tifs)
              (doseq [ard_resp body]
                (if (= 200 (first (vals ard_resp)))
                  (do (dom/update-for-ingest-success counter-map)
                      (util/log (str "200 ard_resp: " ard_resp)))
                  (do (util/log (str "status is NOT 200, ingest failed. message: " body))
                      (dom/update-for-ingest-fail counter-map)))))
          (do (util/log (str "non-200 response: " response)))))
    (recur)))

(defn ^:export assess-ard
  "Exposed function for determining what ARD needs to be ingested."
  [ard-host tile-id bsy-div ing-btn ing-ctr mis-ctr iwds-miss-list error-ctr error-div & [ard-req-fn]]
  (let [ard-request-handler    (or ard-req-fn http/get-request)
        ard-inventory-resource (util/ard-url-format ard-host  tile-id)
        dom-map  (hash-map :ing-ctr ing-ctr :mis-ctr mis-ctr :bsy-div bsy-div :ing-btn ing-btn
                           :iwds-miss-list iwds-miss-list :error-ctr error-ctr :error-div error-div)]
    (report-assessment ard-data-chan dom-map) ;; park func on ard-data-chan to update dom
    (go (>! ard-data-chan (<! (ard-request-handler ard-inventory-resource))))))

(defn ^:export ingest 
  "Exposed function for initiating the ARD ingest process."
  [ard-host inprogress-div missing-div ingested-div busy-div error-div ingesting-div par-level]
  (let [ard-sources          (:tifs @ard-miss-atom)
        counter-map          (hash-map :progress inprogress-div :missing missing-div 
                                       :ingested ingested-div :error error-div)
        ard-count            (count ard-sources)
        partition-level      (read-string par-level)]

    (dom/update-for-ingest-start (:progress counter-map) ard-count) 
    (ingest-status-handler ingest-status-chan counter-map) 
    (make-chipmunk-requests ard-to-ingest-chan ingest-status-chan ard-host busy-div ingesting-div inprogress-div partition-level)
    (go (>! ard-to-ingest-chan ard-sources))))

