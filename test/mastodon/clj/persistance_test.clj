(ns mastodon.clj.persistance-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [mastodon.clj.config :as config]
            [mastodon.clj.persistance :as persist]
            [org.httpkit.client :as http]))


(deftest resource-path-test-ard
  (with-redefs [config/config {:data_type "ard" :ard_host "http://ardhost.gov"}]
    (is (= (persist/resource-path "LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif")
           "http://ardhost.gov/ard/etm/ARD_Tile/2002/CU/005/015/LE07_CU_005015_20021221_20170919_C01_V01_BT.tar/LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif"))))

(deftest resource-path-test-aux
  (with-redefs [config/config {:data_type "aux" :aux_host "http://auxhost.gov"}]
    (is (= (persist/resource-path "AUX_CU_005015_20000731_20171031_V01_ASPECT.tif")
         "http://auxhost.gov/AUX_CU_005015_20000731_20171031_V01.tar/AUX_CU_005015_20000731_20171031_V01_ASPECT.tif"))))

(deftest ingest-test
  (with-fake-http [{:url "http://www.iwdshost.gov/inventory" :method :post} {:status 200 :body "ok"}]
    (with-redefs [config/config {:data_type "ard" :ard_host "foohost.gov"}]
      (is (= {"LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif" 200}
             (persist/ingest "LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif" "http://www.iwdshost.gov"))))))

(deftest ingest-test-exception
  (with-redefs [config/config {:data_type "ard" :ard_host "foohost.gov"}]
    (let [resp (persist/ingest "LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif" "http://www.iwdshost.gov")]
      (is (= (set (keys resp)) #{"LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif" :error}))
      (is (= (get resp "LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif") 500)))))

(deftest ingested-tifs-test
  (with-fake-http [{:url "http://chiphost.gov/inventory?tile=005015" :method :get} {:status 200 :body "[{\"tile\": \"005015\", \"source\": \"a.tif\"}, {\"tile\": \"005015\", \"source\": \"b.tif\"}]" }]
    (with-redefs [config/config {:chipmunk_inventory "http://chiphost.gov/inventory?tile="}]
      (is (= (persist/ingested-tifs "005015") '("a.tif" "b.tif"))))))

