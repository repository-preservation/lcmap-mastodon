(ns mastodon.clj.config-test
  (:require [clojure.test :refer :all]
            [environ.core :as environ]
            [mastodon.clj.config :as config]))

(deftest test-nemo-host
  (with-redefs [environ/env {:nemo-host "http://nemo-host.gov"}]
    (is (= (config/nemo-host) "http://nemo-host.gov"))))

(deftest test-nemo-inventory-env
  (with-redefs [environ/env {:nemo-inventory "/nemo-inventory"}]
    (is (= (config/nemo-inventory) "/nemo-inventory"))))

(deftest test-nemo-inventory-default
  (is (= (config/nemo-inventory) "/inventory_by_tile?tile=")))

(deftest test-try-read-valid
  (is (= (config/try-read "100") 100)))

(deftest test-try-read-nil
  (is (nil? (config/try-read nil))))


(deftest test-config
  (is (= (set (keys config/config))
         #{:ingest_timeout :partition_level 
           :ard_host :nemo_host :aux_host :from_date 
           :chipmunk_host :inventory_timeout :to_date 
           :nemo_inventory :data_type :ard_path})))
