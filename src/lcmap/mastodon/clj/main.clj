(ns lcmap.mastodon.clj.main
  (:gen-class)
  (:require [cheshire.core                  :refer :all]
            [environ.core                   :as environ]
            [lcmap.mastodon.cljc.util       :as util]
            [lcmap.mastodon.clj.persistance :as persist]
            [lcmap.mastodon.clj.validation  :as validation]
            [org.httpkit.client             :as http]
            [org.httpkit.server             :as server]
            [lcmap.mastodon.clj.server      :as mserver]))

(defn pmap-partitions 
  [infunc collection]
  (doseq [i collection]
    (doall (pmap infunc i))))

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
        (server/run-server #'mserver/app {:port 9876}))
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
                ard_partition  (partition partition_level partition_level "" missing_vector)
                ingest_map #(persist/ingest % iwds_host)]

            (println "Tile Status Report for: " tileid)
            (println "To be ingested: " (count missing_vector))
            (println "Already ingested: " ingested_count)
            (println "")
            
            (if (= autoingest "-y")
              (do 
                (pmap-partitions ingest_map ard_partition)
                (println "Ingest Complete"))
              (do 
                (println "Ingest? (y/n)")
                (if (= (read-line) "y")
                  (do
                    (pmap-partitions ingest_map ard_partition)
                    (println "Ingest Complete"))
                  (do 
                    (println "Exiting!")
                    (System/exit 0)))))))
        (System/exit 0)))))

