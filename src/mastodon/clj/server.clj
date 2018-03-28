(ns mastodon.clj.server
   (:require [clojure.string           :as string]
             [clojure.tools.logging    :as log]
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

(defn ard-status
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

(defn get-base 
  "Hello Mastodon"
  [request]
  {:status 200 :body ["Would you like some ARD with that?"]})

(compojure/defroutes routes
  (compojure/context "/" request
    (route/resources "/")
    (compojure/GET   "/" [] (get-base request))
    (compojure/GET   "/inventory/:tileid{[0-9]{6}}" [tileid] (ard-status tileid))
    (compojure/POST  "/bulk-ingest" [] (bulk-ingest request))))

(def app (-> routes
             (ring-json/wrap-json-body {:keywords? true})
             (ring-json/wrap-json-response)))
