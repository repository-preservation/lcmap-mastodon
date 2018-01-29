(ns lcmap.mastodon.cljc.core
  #? (:cljs (:require-macros [cljs.core.async.macros :refer [go go-loop]]))
  (:require #? (:clj  [lcmap.mastodon.clj.http :as http]
                :cljs [lcmap.mastodon.cljs.http :as http])
            [lcmap.mastodon.clj.ard  :as ard]
            #? (:cljs [lcmap.mastodon.cljs.dom  :as dom]) 
            [lcmap.mastodon.cljc.util :as util]
            #? (:clj  [clojure.core.async :refer [<! >! chan go go-loop]]
                :cljs [cljs.core.async :refer [<! >! chan]])

            
  ))


(def ard-data-chan (chan 1))       ;; channel holding ARD resource locations
(def ard-to-ingest-chan (chan 1))  ;; channel used to handle ARD to ingest
(def ingest-status-chan (chan 1))  ;; channel used to handle ingest status

(def ard-miss-atom (atom {})) ;; atom containing list of ARD not yet ingested
(def iwd-miss-atom (atom {})) ;; atom containing list of ARD only found in IWDS

(def ard-resource-atom  (atom {:path ""}))   ;; atom containing ARD host
(def iwds-resource-atom (atom {:path ""}))   ;; atom containing IWDS host
(def ingest-resource-atom (atom {:path ""})) ;; atom containing Ingest host

(defn -main [] (prn "hey there"))

(defn keep-host-info
  "Function for persisting ARD and IWDS host information within Atoms

   ^String :ard-host: name of ARD host
   ^String :iwds-host: name of IWDS host"
  [ard-host iwds-host ingest-host]
  (swap! ard-resource-atom  assoc :path ard-host)
  (swap! iwds-resource-atom assoc :path iwds-host)
  (swap! ingest-resource-atom assoc :path ingest-host))

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
              :v (last match))))

(defn ard-url-format
  "URL generation function for requests to an ARD file access server

   ^String :host:    Host name
   ^String :tile-id: Tile ID

   Returns formatted url as a string for requesting source list from ARD server"
  [host tile-id]
  (let [hvm (hv-map tile-id)
        host-fmt (util/trailing-slash host)]
    (str host-fmt "ard/" (:h hvm) (:v hvm))))

(defn iwds-url-format
  "URL generation function for requests to an LCMAP-Chipmunk instance

   ^String :host:    lcmap-chipmunk instance host
   ^String :tile-id: Tile ID

   Returns formatted url as a string for requesting source list from IWDS"
  [host tile-id]
  (let [host-fmt (util/trailing-slash host)]
    (str host-fmt "inventory?only=source&tile=" tile-id)))

(defn compare-iwds 
  "Compare the available ARD resources against whats available from IWDS. This
   function is parked on the ard-data-chan channel. When an ARD request response
   lands on the ard-data-chan, make an inventory request to the IWDS. Categorize
   the results, put them in Atoms, and update the DOM.

   ^Core.Async Channel :ard-channel:  The channel containing list of ARD available for ingest
   ^String             :iwds-url:     The lcmap-chipmunk url used to check whats been ingested for a tile
   ^Function           :iwds-request: The function to use for checking what ARD the IWDS has ingested
   ^Hash Map           :dom-map:      Hash map of DOM elements to update after comparison has been made

   Returns a Core.Async channel. Organizes ARD by status, placing lists
   in appropriate Atoms."
  [ard-channel iwds-url iwds-request dom-map & [dom-func]]
  (go
    (let [ard-tifs   (<! ard-channel)
          iwds-tifs  (ard/iwds-tifs (<! (iwds-request iwds-url)))
          ard-report (ard/ard-iwds-report ard-tifs (:tifs iwds-tifs))
          dom-update #? (:cljs (or dom-func dom/update-for-ard-check) 
                         :clj nil)
          report-map (hash-map :ingested-count     (count (:ingested ard-report))
                               :ard-missing-count  (count (:ard-only ard-report))
                               :iwds-missing       (:iwd-only ard-report)
                               :dom-map dom-map)]

          (util/log (str "ARD Status Report: " report-map))
          (swap! ard-miss-atom assoc :tifs (:ard-only ard-report))
          (swap! iwd-miss-atom assoc :tifs (:iwd-only ard-report))
          
          #? (:cljs (dom-update report-map (count (:ard-only ard-report)) (:errors iwds-tifs))) 

)))

(defn ^:export assess-ard
  "Diff function, comparing what source files the ARD source has available, 
   and what sources have been ingested into the data warehouse for a specific tile

   ^String :ard-host:       ARD Host
   ^String :iwds-host:      IWDS Host
   ^String :ingest-host:    Ingest Host
   ^String :tile-id:        Tile ID
   ^String :region:         Region
   ^String :bsy-div:        busy image name
   ^String :ing-btn:        ingest button name
   ^String :ing-ctr:        ingest counter name
   ^String :mis-ctr:        missing counter name
   ^String :iwds-miss-list: missing ARD div name
   ^String :error-ctr:      error counter name
   ^String :error-div:      error container name

   Returns Core.Async Channel. Parks compare-iwds function on the ard-data-chan Channel,
   requests ARD inventory for a given tile."
  [ard-host iwds-host ingest-host tile-id region bsy-div ing-btn ing-ctr mis-ctr iwds-miss-list error-ctr error-div & [ard-req-fn iwds-req-fn]]
    (let [ard-request-handler    (or ard-req-fn http/get-request)
          iwds-request-handler   (or iwds-req-fn http/get-request)
          ard-inventory-resource (ard-url-format ard-host  tile-id)
          ard-download-resource  (str ard-host "/ardtars")
          iwds-resource          (iwds-url-format iwds-host tile-id)
          iwds-post-url          (str iwds-host "/inventory")
          dom-map  (hash-map :ing-ctr ing-ctr :mis-ctr mis-ctr :bsy-div bsy-div :ing-btn ing-btn :iwds-miss-list iwds-miss-list :error-ctr error-ctr :error-div error-div)]

          (keep-host-info ard-download-resource iwds-post-url ingest-host)
          (compare-iwds ard-data-chan iwds-resource iwds-request-handler dom-map)     
          (go (>! ard-data-chan (-> (<! (ard-request-handler ard-inventory-resource))
                                    (util/with-suffix "tar")
                                    (ard/expand-tars))))))

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
  [ingest-channel iwds-resource status-channel busy-div ingesting-div]
  (go
    (let [tifs (<! ingest-channel)]
      (doseq [t tifs]
        #? (:cljs (dom/set-div-content ingesting-div [(str "Ingesting: " t)]))
        (>! status-channel  (<! (http/post-request iwds-resource {"url" t}))))
      #? (:cljs (dom/update-for-ingest-completion busy-div ingesting-div)) 
)))

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
              #? (:cljs (dom/update-for-ingest-success counter-map)))
          (do (util/log (str "status is NOT 200, ingest failed. message: " body))
              #? (:cljs (dom/update-for-ingest-fail counter-map))  ) ))
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
  [inprogress-div missing-div ingested-div busy-div error-div ingesting-div]
  (let [ard-resource-path    (:path @ard-resource-atom)
        iwds-resource-path   (:path @iwds-resource-atom)
        ingest-resource-path (:path @ingest-resource-atom)
        ard-sources          (map #(ard/tif-path % ard-resource-path ingest-resource-path) (:tifs @ard-miss-atom))
        counter-map          (hash-map :progress inprogress-div :missing missing-div :ingested ingested-div :error error-div)
        ard-count            (count ard-sources)]

    #? (:cljs (dom/update-for-ingest-start (:progress counter-map) ard-count)) 
    (ingest-status-handler ingest-status-chan counter-map) 
    (make-chipmunk-requests ard-to-ingest-chan iwds-resource-path ingest-status-chan busy-div ingesting-div)
    (go (>! ard-to-ingest-chan ard-sources))))

