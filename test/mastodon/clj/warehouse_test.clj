(ns mastodon.clj.warehouse-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [mastodon.clj.config :as config]
            [mastodon.clj.warehouse :as warehouse]))

(deftest ingested-tifs-test
  (with-fake-http [{:url "http://nemohost.gov/inventory?tile=005015" :method :get} {:status 200 :body "[{\"tile\": \"005015\", \"source\": \"a.tif\"}, {\"tile\": \"005015\", \"source\": \"b.tif\"}]" }]
    (with-redefs [config/config {:nemo_inventory "http://nemohost.gov/inventory?tile="}]
      (is (= (warehouse/ingested-tifs "005015") '("a.tif" "b.tif"))))))
