(ns lcmap.mastodon.clj.main
  (:gen-class)
  (:require [clojure.string                 :as string]
            [compojure.core                 :as compojure]
            [compojure.route                :as route]
            [cheshire.core                  :refer :all]
            [environ.core                   :as environ]
            [lcmap.mastodon.cljc.ard        :as ard]
            [lcmap.mastodon.cljc.util       :as util]
            [lcmap.mastodon.clj.file        :as file]
            [lcmap.mastodon.clj.persistance :as persist]
            [lcmap.mastodon.clj.validation  :as validation]
            [org.httpkit.client             :as http]
            [org.httpkit.server             :as server]
            [ring.middleware.json           :as ring-json]))

(defn bulk-ingest
  "Generate ingest requests for list of posted ARD"
  [{:keys [:body] :as req}]
  (let [tifs    (string/split (:urls body) #",")
        iwds    (:iwds-host environ/env)
        results (pmap #(persist/ingest % iwds) tifs)]
    
    ;; realize results
    (count results)
    {:status 200 :body results}))

(defn ard-status
  [tileid]
  (let [hvmap    (util/hv-map tileid)
        filepath (-> (:ard-path environ/env) (str (:h hvmap) "/" (:v hvmap) "/*"))
        ardtifs  (-> filepath (file/get-filenames)
                              (util/with-suffix "tar")
                              (#(map ard/ard-manifest %))
                              (flatten))
        iwds_src (-> (:iwds-host environ/env) (str "/inventory?only=source&source="))
        ing_src  (-> (:ard-host environ/env) (str "/ard"))
        ard_res  (pmap #(persist/status-check % iwds_src ing_src) ardtifs)]
    ; realize ard_res
    (count ard_res)
    (let [missing  (filter (fn [i] (= (vals i) '("[]"))) ard_res)
          ingested (filter (fn [i] (not (= (vals i) '("[]")))) ard_res)
          miss_count   (count missing)
          ingest_count (count ingested)
          miss_flat (keys (apply merge-with concat missing))]
      {:status 200 :body {:ingested ingest_count :missing miss_flat}})))

(defn get-base [request]
  {:status 200 :body ["Would you like some ARD with that?"]})

;; ## Routes
(compojure/defroutes routes
  (compojure/context "/" request
    (route/resources "/")
    (compojure/GET   "/" [] (get-base request))
    (compojure/GET   "/inventory/:tileid{[0-9]{6}}" [tileid] (ard-status tileid))
    (compojure/POST  "/bulk-ingest" [] (bulk-ingest request))))

(def app (-> routes
             (ring-json/wrap-json-body {:keywords? true})
             (ring-json/wrap-json-response)))

(defn -main [& args]
  (let [tileid          (first args)
        autoingest      (last  args)
        iwds_host       (:iwds-host environ/env)
        ard_host        (:ard-host  environ/env)
        ard_path        (:ard-path  environ/env)
        partition_level (read-string (:partition-level environ/env))]

    (if (nil? tileid)
      (do ;; no args, run server
        (when (not (validation/validate-server iwds_host ard_host partition_level ard_path)) 
          (println "validation failed, exiting")
          (System/exit 0))
        (server/run-server #'app {:port 9876}))
      (do
        (when (not (validation/validate-cli tileid iwds_host ard_host partition_level))
          (println "validation failed, exiting")
          (System/exit 0))

        (let [iwds_resource (str iwds_host "/inventory?only=source&source=")
              ard_resource  (util/ard-url-format ard_host tileid)
              ing_resource  (str ard_host "/ard")
              ard_response  (http/get ard_resource)]

          (let [response_map   (-> (:body @ard_response) (parse-string true))
                missing_vector (:missing response_map)
                ingested_count (:ingested response_map)
                ard_partition (partition partition_level partition_level "" missing_vector)
                ingest_map #(persist/ingest % iwds_host)]

            (println "Tile Status Report for: " tileid)
            (println "To be ingested: " (count missing_vector))
            (println "Already ingested: " ingested_count)
            (println "")
            
            (if (= autoingest "-y")
              (do 
                (doseq [a ard_partition]
                  (count (pmap ingest_map a)))
                (println "Ingest Complete"))
              (do 
                (println "Ingest? (y/n)")
                (if (= (read-line) "y")
                  (do
                    (doseq [a ard_partition]
                      (count (pmap ingest_map a)))
                    (println "Ingest Complete"))
                  (do 
                    (println "Exiting!")
                    (System/exit 0)))))))
        (System/exit 0)))))

