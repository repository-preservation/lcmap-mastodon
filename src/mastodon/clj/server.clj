(ns mastodon.clj.server
   (:require [clojure.string           :as string]
             [clojure.tools.logging    :as log]
             [clojure.set              :as set]
             [compojure.core           :as compojure]
             [compojure.route          :as route]
             [environ.core             :as environ]
             [mastodon.cljc.ard        :as ard]
             [mastodon.cljc.util       :as util]
             [mastodon.clj.file        :as file]
             [mastodon.clj.persistance :as persist]
             [mastodon.clj.validation  :as validation]
             [ring.middleware.json     :as ring-json]))

(def iwds-host (:iwds-host environ/env))
(def ard-host  (:ard-host  environ/env))
(def ard-path  (:ard-path  environ/env))

(defn bulk-ingest
  "Generate ingest requests for list of posted ARD."
  [{:keys [:body] :as req}]
  (let [tifs    (string/split (:urls body) #",")
        results (doall (pmap #(persist/ingest % iwds-host) tifs))]
    {:status 200 :body results}))

(defn http-deps-check
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

(defn ard-status-orig
  "Determine the ARD ingest status for the given tile id."
  [tileid]
  (let [ard-accessible  (validation/http-accessible? ard-host "ARD_HOST")
        iwds-accessible (validation/http-accessible? iwds-host "IWDS_HOST")
        ard-message  (str "ARD Host: " ard-host " is not reachable. ") 
        iwds-message (str "IWDS Host: " iwds-host " is not reachable")]

    (if (= #{true} (set [ard-accessible iwds-accessible]))
      (do (try
            (let [hvmap    (util/hv-map tileid)
                  filepath (str ard-path (:h hvmap) "/" (:v hvmap) "/*")
                  ardtifs  (-> filepath (file/get-filenames "tar")
                               (#(map ard/ard-manifest %))
                               (flatten))
                  ard_res  (doall (pmap #(persist/status-check % iwds-host (str ard-host "/ard")) ardtifs))
                  missing  (-> ard_res (#(filter (fn [i] (= (vals i) '("[]"))) %)) 
                               (#(apply merge-with concat %)) 
                               (keys))
                  ingested_count (- (count ard_res) (count missing))]
              {:status 200 :body {:ingested ingested_count :missing missing}})
            (catch Exception ex
              (log/errorf "Error determining tile: %s ARD status. exception: %s" (.getMessage ex))
              {:status 200 :body {:error (format "Error determining tile: %s ARD status. exception: %s" (.getMessage ex))}})))
      (do (cond
           (= true ard-accessible)  {:status 200 :body {:error iwds-message}}
           (= true iwds-accessible) {:status 200 :body {:error ard-message}}
           :else {:status 200 :body {:error (str ard-message iwds-message)}})))))

(defn available-ard
  "Return a vector of available ARD for the given tile id"
  [tileid]
  (let [hvmap    (util/hv-map tileid)
        filepath (str ard-path (:h hvmap) "/" (:v hvmap) "/*")]
    (-> filepath (file/get-filenames "tar")
                 (#(map ard/ard-manifest %))
                 (flatten))))

(defn available-ard-filtered
  [tileid from to]
  (let [ardtifs (available-ard tileid)
        froms   (filter (fn [i] (>= (-> i (util/tif-only) (ard/year-acquired) (read-string)) from-i)) ardtifs)
        tos     (filter (fn [i] (<= (-> i (util/tif-only) (ard/year-acquired) (read-string)) to-i)) ardtifs)]
    (vec (set/intersection (set froms) (set tos)))))

(defn ard-report
  "Return hash-map of missing ARD and an ingested count"
  [tifs]
  (let [ard_res  (doall (pmap #(persist/status-check % iwds-host (str ard-host "/ard")) ardtifs))
        missing  (-> ard_res (#(filter (fn [i] (= (vals i) '("[]"))) %)) 
                             (#(apply merge-with concat %)) 
                             (keys))
        ingested_count (- (count ard_res) (count missing))])
  {:missing missing :ingested ingested_count})

(defn ard-status
  ([tileid]
   (try
     (let [ardtifs (available-ard tileid)
           report  (ard-report ardtifs)
           deps-check (http-deps-check)]
       (if (nil? (:error deps-check))
         {:status 200 :body report}
         {:status 200 :body {:error (:error deps-check)}}))
      (catch Exception ex
        (log/errorf "Error determining tile: %s ARD status. exception: %s" (.getMessage ex))
        {:status 200 :body {:error (format "Error determining tile: %s ARD status. exception: %s" (.getMessage ex))}})))
  ([tileid from to]
   (try
     (let [ardtifs (available-ard-filtered tileid from to)
           report  (ard-report ardtifs)
           deps-check (http-deps-check)]
       (if (nil? (:error deps-check))
         {:status 200 :body report}
         {:status 200 :body {:error (:error deps-check)}}))
      (catch Exception ex
        (log/errorf "Error determining tile: %s ARD status. exception: %s" (.getMessage ex))
        {:status 200 :body {:error (format "Error determining tile: %s ARD status. exception: %s" (.getMessage ex))}}))))

(defn get-base 
  "Hello Mastodon"
  [request]
  {:status 200 :body ["Would you like some ARD with that?"]})

(compojure/defroutes routes
  (compojure/context "/" request
    (route/resources "/")
    (compojure/GET   "/" [] (get-base request))
    (compojure/GET   "/inventory/:tileid{[0-9]{6}}" [tileid] (ard-status tileid))
    (compojure/GET   "/inventory/:tileid{[0-9]{6}}/:from{[0-9]{6}}/:to{[0-9]{6}}" [tileid from to] (ard-status tileid from to))
    (compojure/POST  "/bulk-ingest" [] (bulk-ingest request))))

(def app (-> routes
             (ring-json/wrap-json-body {:keywords? true})
             (ring-json/wrap-json-response)))
