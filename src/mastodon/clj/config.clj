(ns mastodon.clj.config
  [:require [environ.core :as environ]])

(defn chipmunk-inventory
  []
  )

(defn try-read
  [val]
  (try (read-string val)
       (catch Exception ex
         nil)))

(def config
  {:chipmunk_host  (:chipmunk-host environ/env)
   :chipmunk_inventory (str (:chipmunk-host environ/env) "/sources?tile=")
   :aux_host       (:aux-host      environ/env)
   :ard_host       (:ard-host      environ/env)
   :ard_path       (:ard-path      environ/env)
   :from_date      (:from-date     environ/env)
   :to_date        (:to-date       environ/env)
   :data_type      (:data-type     environ/env)
   :ingest_timeout    (or (try-read (:ingest-timeout    environ/env)) 120000)
   :inventory_timeout (or (try-read (:inventory-timeout environ/env)) 120000)
   :partition_level   (or (try-read (:partition-level   environ/env)) 10)})

