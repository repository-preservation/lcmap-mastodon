(ns mastodon.clj.main
  (:gen-class)
  (:require [cheshire.core            :refer [parse-string]]
            [mastodon.clj.config      :refer [config]]
            [mastodon.cljc.util       :as util]
            [mastodon.clj.persistance :as persist]
            [mastodon.clj.validation  :as validation]
            [org.httpkit.client       :as http]
            [mastodon.clj.server      :as server]
            [clojure.tools.logging    :as log]))

(defn pmap-partitions
  "Realize func with pmap over a collection of collections." 
  [infunc collection]
  (doseq [i collection]
    (doall (pmap infunc i))))

(defn data-ingest
  [tileid args]
  (let [data_url       (util/inventory-url-format (:data_host config) tileid (:from_date config) (:to_date config))
        data_response  (http/get data_url {:timeout (:inventory_timeout config)})
        response_map   (-> (:body @data_response) (parse-string true))
        missing_vector (:missing response_map)
        data_partition (partition (:partition_level config) (:partition_level config) "" missing_vector)
        ingest_map     #(persist/ingest % (:chipmunk_host config))
        autoingest     (first args)]

    (if (:error @data_response)
      (do (log/errorf "Error response from DATA_HOST: %s" (:error @data_response))
          (System/exit 1))
      (do (log/infof "Tile Status report for: %s \nTo be ingested: %s \nAlready ingested: %s\n" 
               tileid (count missing_vector) (:ingested response_map))))

    (if (= autoingest "-y")
      (do (pmap-partitions ingest_map data_partition)
          (log/infof "Ingest Complete"))
      (do (println "Ingest? (y/n)")
          (if (= (read-line) "y")
            (do (pmap-partitions ingest_map data_partition)
                (println "Ingest Complete"))
            (do (println "Exiting!")))))))

(defn -main 
  ([]
   (try
     (when (not (validation/validate-server config))
       (log/errorf "validation failed, exiting")
       (System/exit 1))
     (server/run-server config)
     (catch Exception ex
       (log/errorf "error starting Mastodon server. exception: %s" (util/exception-cause-trace ex "mastodon"))
       (System/exit 1))))
  ([tileid & args]
   (try
     (when (not (validation/validate-cli tileid config))
       (log/errorf "validation failed, exiting")
       (System/exit 1))
     (data-ingest tileid args)
     (System/exit 0)
     (catch Exception ex
       (log/errorf "Error determining tile ingest status. exception: %s" (util/exception-cause-trace ex "mastodon"))
       (System/exit 1)))))


