(ns mastodon.clj.server
   (:require [clojure.string           :as string]
             [clojure.tools.logging    :as log]
             [clojure.set              :as set]
             [compojure.core           :as compojure]
             [compojure.route          :as route]
             [environ.core             :as environ]
             [mastodon.cljc.data       :as data]
             [mastodon.cljc.util       :as util]
             [mastodon.clj.file        :as file]
             [mastodon.clj.persistance :as persist]
             [mastodon.clj.validation  :as validation]
             [ring.middleware.json     :as ring-json]
             [ring.middleware.keyword-params :as ring-keyword-params]
             [ring.middleware.defaults :as ring-defaults]
             [org.httpkit.client       :as http]
             [org.httpkit.server       :as http-server]))

(def iwds-host (:iwds-host environ/env))
(def aux-host  (:aux-host  environ/env))
(def ard-host  (:ard-host  environ/env))
(def ard-path  (:ard-path  environ/env))
(def server-type (:server-type environ/env))

(defn bulk-ingest
  "Generate ingest requests for list of posted ARD."
  [{:keys [:body] :as req}]
  (let [tifs    (string/split (:urls body) #",")
        results (doall (pmap #(persist/ingest % iwds-host) tifs))]
    {:status 200 :body results}))

(defn http-deps-check
  "Return error response if external http dependencies are not reachable"
  []
  (let [ard-accessible  (validation/http-accessible? ard-host "ARD_HOST")
        iwds-accessible (validation/http-accessible? iwds-host "IWDS_HOST")
        ard-message  (str "ARD Host: " ard-host " is not reachable. ") 
        iwds-message (str "IWDS Host: " iwds-host " is not reachable")]
    (if (= #{true} (set [ard-accessible iwds-accessible]))
      (do {:error nil})
      (do (cond
           (= true ard-accessible)  {:error iwds-message}
           (= true iwds-accessible) {:error ard-message}
           :else {:error (str ard-message iwds-message)})))))

(defn available-ard
  "Return a vector of available ARD for the given tile id"
  [tileid]
  (let [hvmap    (util/hv-map tileid)
        filepath (str ard-path (:h hvmap) "/" (:v hvmap) "/*")]
    (-> filepath (file/get-filenames "tar")
                 (#(map data/ard-manifest %))
                 (flatten))))

(defn filtered-ard
  "Return vector of available ARD for a given tile id, between the provided years"
  [tileid from to]
  (let [ardtifs (available-ard tileid)
        froms   (filter (fn [i] (>= (-> i (util/tif-only) (data/year-acquired) (read-string)) from)) ardtifs)
        tos     (filter (fn [i] (<= (-> i (util/tif-only) (data/year-acquired) (read-string)) to)) ardtifs)]
    (vec (set/intersection (set froms) (set tos)))))

(defn data-report
  "Return hash-map of missing ARD and an ingested count"
  [tifs type]
  (let [ingest_host (if (= "aux" type) (str aux-host) (str ard-host "/ard"))
        ard_res  (doall (pmap #(persist/status-check % iwds-host ingest_host) tifs))
        missing  (-> ard_res (#(filter (fn [i] (= (vals i) '("[]"))) %)) 
                             (#(apply merge-with concat %)) 
                             (keys))
        ingested_count (- (count ard_res) (count missing))]
  {:missing missing :ingested ingested_count}))

(defn ard-status
  "Return ARD ingest status for given tile id"
  [tileid {:keys [params] :as req}]
  (try
     (let [from (or (util/try-string (:from params)) 0)
           to   (or (util/try-string (:to params)) 3000)
           tifs (filtered-ard tileid from to)
           deps (http-deps-check)]
       (if (nil? (:error deps))
         {:status 200 :body (data-report tifs "ard")}
         {:status 200 :body {:error (:error deps)}}))
      (catch Exception ex
        (log/errorf "Error determining tile: %s ARD status. exception: %s" tileid (.getMessage ex))
        {:status 200 :body {:error (format "Error determining tile: %s ARD status. exception: %s" tileid (.getMessage ex))}})))

(defn aux-status
  "Return Auxiliary data ingest status for given tile id"
  [tileid]
  (try
    (let [aux_resp (http/get aux-host)
          aux_file (util/get-aux-name (:body @aux_resp) tileid)
          aux_tifs (doall (data/aux-manifest aux_file)) 
          deps (http-deps-check)]
      (if (nil? (:error deps))
        {:status 200 :body (data-report aux_tifs "aux")}
        {:status 200 :body {:error (:error deps)}}))
      (catch Exception ex
        (log/errorf "Error determining tile: %s AUX data status. exception: %s" tileid (.getMessage ex))
        {:status 200 :body {:error (format "Error determining tile: %s AUX data status. exception: %s" tileid (.getMessage ex))}})))

(defn get-base 
  "Hello Mastodon"
  [request]
  {:status 200 :body ["Would you like some data with that?"]})

(defn get-status
  "Route inventory status request to proper function"
  [tileid request]
  (if (= server-type "ard")
    (ard-status tileid request)
    (aux-status tileid)))

(compojure/defroutes routes
  (compojure/context "/" request
    (route/resources "/")
    (compojure/GET   "/" [] (get-base request))
    (compojure/GET   "/inventory/:tileid{[0-9]{6}}" [tileid] (get-status tileid request))
    (compojure/POST  "/bulk-ingest" [] (bulk-ingest request))))

(defn response-handler
  [routes]
  (-> routes
      (ring-json/wrap-json-body {:keywords? true})
      (ring-json/wrap-json-response)
      (ring-defaults/wrap-defaults ring-defaults/api-defaults)
      (ring-keyword-params/wrap-keyword-params)))

(def app (response-handler routes))

(defn run-server
  [server-type]
  (log/infof "iwds-host: %s" iwds-host)
  (log/infof "aux-host: %s" aux-host)
  (log/infof "ard-host: %s" ard-host)
  (log/infof "ard-path: %s" ard-path)
  (http-server/run-server app {:port 9876}))

