(ns mastodon.clj.persistance-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [mastodon.clj.persistance :as persist]
            [org.httpkit.client :as http]))

(deftest ingest-test
  (with-fake-http [{:url "http://www.iwdshost.com/inventory" :method :post} {:status 200 :body "ok"}]
    (is (= {"foo.tif" 200}
           (persist/ingest "sweetard.tar/foo.tif" "http://www.iwdshost.com")))))

(deftest status-check-test
  (let [tar "LE07_CU_005015_20021221_20170919_C01_V01_BT.tar"
        tif "LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif"]

  (with-fake-http ["http://iwdshost.org/inventory?only=source&source=LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif" {:status 200 :body "ok"}]
    (is (= (persist/status-check tif "http://iwdshost.org" "http://ingesthost.org")
           {"http://ingesthost.org/etm/ARD_Tile/2002/CU/005/015/LE07_CU_005015_20021221_20170919_C01_V01_BT.tar/LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif" "ok"})))))

(deftest ard-resource-path-test
  (is (= "http://ingestresource/etm/ARD_Tile/2002/CU/005/015/LE07_CU_005015_20021221_20170919_C01_V01_BT.tar/LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif"
       (persist/ard-resource-path "LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif" "http://ingestresource"))))

(deftest aux-resource-path-test
  (is (= (persist/aux-resource-path "AUX_CU_005015_20000731_20171031_V01_ASPECT.tif" "http://ingestresource")
         "http://ingestresource/AUX_CU_005015_20000731_20171031_V01.tar/AUX_CU_005015_20000731_20171031_V01_ASPECT.tif")))
