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
  (when (not (validation/validate-cli tileid config))
    (log/errorf "validation failed, exiting")
    (System/exit 1))
  (let [data_url       (util/inventory-url-format (:ard_host config) tileid (:from_date config) (:to_date config))
        ard_response   (http/get data_url)
        response_map   (-> (:body @ard_response) (parse-string true))
        missing_vector (:missing response_map)
        ard_partition  (partition (:partition_level config) (:partition_level config) "" missing_vector)
        ingest_map     #(persist/ingest % (:chipmunk_host config))
        autoingest     (first args)]

    (if (:error @ard_response)
      (do (log/errorf "Error response from ARD_HOST: %s" (:error @ard_response))
          (System/exit 1))
      (do (log/infof "Tile Status report for: %s \nTo be ingested: %s \nAlready ingested: %s\n" 
               tileid (count missing_vector) (:ingested response_map))))

    (if (= autoingest "-y")
      (do (pmap-partitions ingest_map ard_partition)
          (log/infof "Ingest Complete"))
      (do (println "Ingest? (y/n)")
          (if (= (read-line) "y")
            (do (pmap-partitions ingest_map ard_partition)
                (println "Ingest Complete"))
            (do (println "Exiting!")))))))

(defn -main
  ([]
   (try
     (when (not (contains? #{"ard" "aux"} (:server_type config)))
       (log/errorf "invalid option for mastodon server: %s" (:server_type config))
       (System/exit 1))

     (log/infof "Running Mastodon for data type: %s" (:server_type config))
     (server/run-server config)
     (catch Exception ex
       (log/errorf "error starting Mastodon server. exception: %s" (util/exception-cause-trace ex "mastodon"))
       (System/exit 1))))
  ([tileid & args]
   (try
     (data-ingest tileid args)
     (System/exit 0)
     (catch com.fasterxml.jackson.core.JsonParseException jx
       (log/errorf "Non-json response from ARD_HOST request. exception: %s " (util/exception-cause-trace jx "mastodon"))
       (System/exit 1))
     (catch Exception ex
       (log/errorf "Error determining tile ingest status. exception: %s" (util/exception-cause-trace ex "mastodon"))
       (System/exit 1)))))


