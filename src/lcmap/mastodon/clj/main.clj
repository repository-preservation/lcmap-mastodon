(ns lcmap.mastodon.clj.main
  (:gen-class)
  (:require [clojure.string                 :as string]
            [compojure.core                 :as compojure]
            [compojure.route                :as route]
            [environ.core                   :as environ]
            [lcmap.mastodon.cljc.ard        :as ard]
            [lcmap.mastodon.cljc.util       :as util]
            [lcmap.mastodon.clj.file        :as file]
            [lcmap.mastodon.clj.persistance :as persist]
            [lcmap.mastodon.clj.validation  :as validation]
            [org.httpkit.client             :as http]
            [org.httpkit.server             :as server]
            [ring.middleware.json           :as ring-json]))

(def ard-to-ingest-atom (atom []))
(def ingested-ard-atom  (atom []))

(defn bulk-ingest
  "Generate ingest requests for list of posted ARD"
  [{:keys [:body] :as req}]
  (let [tifs    (string/split (:urls body) #",")
        iwds    (:iwds-host environ/env)
        results (pmap #(persist/ingest % iwds) tifs)]
    
    ;; realize results
    (count results)
    {:status 200 :body results}))

(defn ard-lookup 
  "Return list of ARD for a give tileid"
  [tileid]
  (let [hvmap     (util/hv-map tileid)
        ardpath   (:ard-path environ/env) 
        filepath  (str ardpath (:h hvmap) "/" (:v hvmap) "/*")
        ardfiles  (file/get-filenames filepath)]
    {:status 200 :body ardfiles}))

(defn get-base [request]
  {:status 200 :body ["Would you like some ARD with that?"]})

;; ## Routes
(compojure/defroutes routes
  (compojure/context "/" request
    (route/resources "/")
    (compojure/GET   "/" [] (get-base request))
    (compojure/GET   "/inventory/:tileid{[0-9]{6}}" [tileid] (ard-lookup tileid))
    (compojure/POST  "/bulk-ingest" [] (bulk-ingest request))))

(def app (-> routes
             (ring-json/wrap-json-body {:keywords? true})
             (ring-json/wrap-json-response)))

(defn -main [& args]
  (let [tileid          (first args)
        autoingest      (last  args)
        iwds_host       (:iwds-host   environ/env)
        ard_host        (:ard-host    environ/env)
        ingest_host     (:ingest-host environ/env)
        partition_level (read-string (:partition-level environ/env))]

    (if (nil? tileid)
      (do ;; no args, run server
        (when (not (validation/not-nil? (:ard-path environ/env) "ARD_PATH"))
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
              {:keys [status headers body error] :as resp} @(http/get ard_resource)
              ard_vector    (-> body (util/string-to-list) (util/with-suffix "tar") (ard/expand-tars))
              ard_results   (pmap #(persist/status-check % iwds_resource ing_resource ard-to-ingest-atom ingested-ard-atom) ard_vector)]

          ; realize the pmap results
          (count ard_results)

          (println "Tile Status Report for: " tileid)
          (println "To be ingested: "   (count @ard-to-ingest-atom))
          (println "Already ingested: " (count @ingested-ard-atom))
          (println "")

          (let [ard_partition (partition partition_level partition_level "" @ard-to-ingest-atom)
                ingest_map #(persist/ingest % iwds_host)]
            
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

