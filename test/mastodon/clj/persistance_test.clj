(ns mastodon.clj.persistance-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [mastodon.clj.config :as config]
            [mastodon.clj.persistance :as persist]
            [org.httpkit.client :as http]))

(deftest ingest-test
  (with-fake-http [{:url "http://www.iwdshost.com/inventory" :method :post} {:status 200 :body "ok"}]
    (with-redefs [config/config {:server_type "ard" :ard_host "foohost.com"}]
      (is (= {"LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif" 200}
             (persist/ingest "LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif" "http://www.iwdshost.com"))))))

(deftest resource-path-test-ard
  (with-redefs [config/config {:server_type "ard" :ard_host "http://ardhost.com"}]
    (is (= (persist/resource-path "LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif")
           "http://ardhost.com/ard/etm/ARD_Tile/2002/CU/005/015/LE07_CU_005015_20021221_20170919_C01_V01_BT.tar/LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif"))))

(deftest resource-path-test-aux
  (with-redefs [config/config {:server_type "aux" :aux_host "http://auxhost.com"}]
    (is (= (persist/resource-path "AUX_CU_005015_20000731_20171031_V01_ASPECT.tif")
         "http://auxhost.com/AUX_CU_005015_20000731_20171031_V01.tar/AUX_CU_005015_20000731_20171031_V01_ASPECT.tif"))))

