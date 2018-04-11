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

(defn ard-report
  "Return hash-map of missing ARD and an ingested count"
  [tifs]
  (let [ard_res  (doall (pmap #(persist/status-check % iwds-host (str ard-host "/ard")) tifs))
        missing  (-> ard_res (#(filter (fn [i] (= (vals i) '("[]"))) %)) 
                             (#(apply merge-with concat %)) 
                             (keys))
        ingested_count (- (count ard_res) (count missing))]
  {:missing missing :ingested ingested_count}))

(defn ard-status
  [tileid {:keys [params] :as req}]
  (try
     (let [from (or (util/try-string (:from params)) 0)
           to   (or (util/try-string (:to params)) 3000)
           tifs (filtered-ard tileid from to)
           deps (http-deps-check)]
       (if (nil? (:error deps))
         {:status 200 :body (ard-report tifs)}
         {:status 200 :body {:error (:error deps)}}))
      (catch Exception ex
        (log/errorf "Error determining tile: %s ARD status. exception: %s" tileid (.getMessage ex))
        {:status 200 :body {:error (format "Error determining tile: %s ARD status. exception: %s" tileid (.getMessage ex))}})))

(defn aux-status
  [tileid]
  (let [aux_resp (http/get aux-host)
        aux_file (util/get-aux-name (:body @aux_resp) tileid)]
    {:status 200 :body (persist/status-check aux_file iwds-host)}))

(defn aux-ingest
  [{:keys [:body] :as req}])

(defn get-base 
  "Hello Mastodon"
  [request]
  {:status 200 :body ["Would you like some data with that?"]})

(compojure/defroutes ard-routes
  (compojure/context "/" request
    (route/resources "/")
    (compojure/GET   "/" [] (get-base request))
    (compojure/GET   "/inventory/:tileid{[0-9]{6}}" [tileid] (ard-status tileid request))
    (compojure/POST  "/bulk-ingest" [] (bulk-ingest request))))

(compojure/defroutes aux-routes
  (compojure/context "/" request
    (route/resources "/")
    (compojure/GET   "/" [] (get-base request))
    (compojure/GET   "/inventory/:tileid{[0-9]{6}}" [tileid] (aux-status tileid))
    (compojure/POST  "/ingest" [] (aux-ingest request))))

(defn response-handler
  [routes]
  (-> routes
      (ring-json/wrap-json-body {:keywords? true})
      (ring-json/wrap-json-response)
      (ring-defaults/wrap-defaults ring-defaults/api-defaults)
      (ring-keyword-params/wrap-keyword-params)))

(def ard-app (response-handler ard-routes))
(def aux-app (response-handler aux-routes))

(defn run-server
  [server-type]
  (cond
   (= server-type "ard") (http-server/run-server ard-app {:port 9876})
   (= server-type "aux") (http-server/run-server aux-app {:port 9876})
   :else (throw (Exception. (format "Wrong type for run-server: %s" server-type)))))

