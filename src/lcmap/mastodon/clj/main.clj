(ns lcmap.mastodon.clj.main
  (:gen-class)
  (:require [lcmap.mastodon.cljc.core :as mcore]
            [environ.core :as environ]))

(defn -main [& args]
  (let [tileid (first args)
        iwds_host (:iwds-host environ/env)
        ard_host  (:ard-host environ/env)
        ingest_host (:ingest-host environ/env)]
    (println (str "Requesting ingest of " tileid))
    (println (str "iwds host is " iwds_host))
    (println (str "ard host is " ard_host))
    (println (str "ingest host is " ingest_host))

    (mcore/assess-ard ard_host iwds_host ingest_host tileid "" "" "" "" "" "" "" "")




    )
  
)
