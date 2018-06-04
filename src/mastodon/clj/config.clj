(ns mastodon.clj.config
  [:require [environ.core :as environ]])

(defn nemo-host
  []
  (:nemo-host environ/env))

(defn nemo-inventory
  []
  (or (:nemo-inventory environ/env) "/inventory_by_tile?tile="))

(def config
  {:nemo_host      (nemo-host)
   :nemo_inventory (str (nemo-host) (nemo-inventory))
   :chipmunk_host  (:chipmunk-host environ/env)
   :aux_host       (:aux-host      environ/env)
   :ard_host       (:ard-host      environ/env)
   :ard_path       (:ard-path      environ/env)
   :from_date      (:from-date     environ/env)
   :to_date        (:to-date       environ/env)
   :server_type    (:server-type   environ/env)
   :partition_level (try (read-string (:partition-level environ/env))
                         (catch Exception ex
                             nil))})
