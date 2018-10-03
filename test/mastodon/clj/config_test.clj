(ns mastodon.clj.config-test
  (:require [clojure.test :refer :all]
            [environ.core :as environ]
            [mastodon.clj.config :as config]))

(deftest test-try-read-valid
  (is (= (config/try-read "100") 100)))

(deftest test-try-read-nil
  (is (nil? (config/try-read nil))))


(deftest test-config
  (is (= (set (keys config/config))
         #{:ingest_timeout :partition_level 
           :data_host :data_dir :from_date 
           :chipmunk_host :inventory_timeout 
           :to_date :data_type :data_path 
           :chipmunk_inventory})))
