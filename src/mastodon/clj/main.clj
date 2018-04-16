(ns mastodon.clj.main
  (:gen-class)
  (:require [cheshire.core            :refer [parse-string]]
            [environ.core             :as environ]
            [mastodon.cljc.util       :as util]
            [mastodon.clj.persistance :as persist]
            [mastodon.clj.validation  :as validation]
            [org.httpkit.client       :as http]
            [mastodon.clj.server      :as server]
            [clojure.tools.logging    :as log]))

(def iwds_host       (:iwds-host   environ/env))
(def aux_host        (:aux-host    environ/env))
(def ard_host        (:ard-host    environ/env))
(def ard_path        (:ard-path    environ/env))
(def from_date       (:from-date   environ/env))
(def to_date         (:to-date     environ/env))
(def server_type     (:server-type environ/env))   
(def partition_level (if (nil? (:partition-level environ/env)) nil 
                       (read-string (:partition-level environ/env))))    

(defn pmap-partitions
  "Realize func with pmap over a collection of collections." 
  [infunc collection]
  (doseq [i collection]
    (doall (pmap infunc i))))

(defn data-ingest
  [tileid args]
    (try
      (when (not (validation/validate-cli tileid iwds_host ard_host partition_level))
        (log/errorf "validation failed, exiting")
        (System/exit 1))
      (let [data_url       (if (= "ard" server_type)
                             (util/inventory-url-format ard_host tileid from_date to_date)
                             (util/inventory-url-format ard_host tileid))
            ard_response   (http/get data_url)
            response_map   (-> (:body @ard_response) (parse-string true))
            missing_vector (:missing response_map)
            ard_partition  (partition partition_level partition_level "" missing_vector)
            ingest_map     #(persist/ingest % iwds_host)
            autoingest     (first args)]

        (when (:error @ard_response)
          (log/errorf "Error response from ARD_HOST: %s" (:error @ard_response))
          (System/exit 1))

        (log/infof "Tile Status report for: %s \nTo be ingested: %s \nAlready ingested: %s\n" 
                   tileid (count missing_vector) (:ingested response_map))

        (if (= autoingest "-y")
          (do (pmap-partitions ingest_map ard_partition)
              (log/infof "Ingest Complete"))
          (do (println "Ingest? (y/n)")
              (if (= (read-line) "y")
                (do (pmap-partitions ingest_map ard_partition)
                    (println "Ingest Complete"))
                (do (println "Exiting!"))))))
      (catch com.fasterxml.jackson.core.JsonParseException jx
        (log/errorf "Non-json response from ARD_HOST request. exception: %s " (.getMessage jx))
        (System/exit 1))
      (catch Exception ex
        (log/errorf "Error determining tile ingest status. exception: %s" (.getMessage ex))
        (System/exit 1))))

(defn -main
  ([]
   (try
     (when (not (contains? #{"ard" "aux"} server_type))
       (log/errorf "invalid option for mastodon server: %s" server_type)
       (System/exit 1))

     (log/infof "Running Mastodon for data type: %s" server_type)
     (server/run-server server_type)
     (catch Exception ex
       (log/errorf "error starting Mastodon server. exception: %s" (.getMessage ex))
       (System/exit 1))))
  ([tileid & args]
   (data-ingest tileid args)
   (System/exit 0)))


