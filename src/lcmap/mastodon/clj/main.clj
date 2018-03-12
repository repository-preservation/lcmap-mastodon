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

(def iwds_host       (:iwds-host environ/env))
(def ard_host        (:ard-host  environ/env))
(def ard_path        (:ard-path  environ/env))
(def partition_level (if (nil? (:partition-level environ/env))
                       nil 
                       (read-string (:partition-level environ/env))))    

(defn pmap-partitions 
  [infunc collection]
  (doseq [i collection]
    (doall (pmap infunc i))))

(defn -main
  ([]
    (when (not (validation/validate-server iwds_host ard_host partition_level ard_path)) 
      (println "validation failed, exiting")
      (System/exit 1))
    (server/run-server #'mserver/app {:port 9876}))
  ([tileid & args]
    (when (not (validation/validate-cli tileid iwds_host ard_host partition_level))
      (println "validation failed, exiting")
      (System/exit 1))
    (let [ard_response   (http/get (util/ard-url-format ard_host tileid))
          response_map   (-> (:body @ard_response) (parse-string true))
          missing_vector (:missing response_map)
          ard_partition  (partition partition_level partition_level "" missing_vector)
          ingest_map     #(persist/ingest % iwds_host)
          autoingest     args]

      (println "Tile Status Report for: " tileid)
      (println "To be ingested: " (count missing_vector))
      (println "Already ingested: " (:ingested response_map))
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
              (println "Exiting!"))))))
    (System/exit 0)))

