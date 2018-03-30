(ns mastodon.clj.server-test
  (:require [clojure.test :refer :all]
            [mastodon.clj.server :as server]
            [mastodon.clj.file :as file]
            [ring.mock.request :as mock]
            [mastodon.clj.persistance :as persist]
            [mastodon.clj.validation :as validation]))

(def tiflist ["http://192.168.43.5/ard/oli_tirs/ARD_Tile/2013/CU/005/015/LC08_CU_005015_20130415_20171016_C01_V01_SR.tar/LC08_CU_005015_20130415_20171016_C01_V01_SRB4.tif"
              "http://192.168.43.5/ard/tm/ARD_Tile/1984/CU/005/015/LT05_CU_005015_19840508_20170912_C01_V01_BT.tar/LT05_CU_005015_19840508_20170912_C01_V01_BTB6.tif"
              "http://192.168.43.5/ard/tm/ARD_Tile/1982/CU/005/015/LT04_CU_005015_19821119_20170912_C01_V01_SR.tar/LT04_CU_005015_19821119_20170912_C01_V01_SRB5.tif"])

(deftest bulk-ingest-test
  (with-redefs [persist/ingest (fn [a b] "tif")]
    (is (= (server/bulk-ingest {:body {:urls "list,of,ard"}})
           {:status 200 :body '("tif" "tif" "tif")}))))

(deftest ard-status-test
  (with-redefs [file/get-filenames (fn [path x] ["LE07_CU_005015_20021221_20170919_C01_V01_BT.tar"])
                persist/status-check (fn [tif x y] {"LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif" "[]"})
                validation/http-accessible? (fn [x y] true)
                server/ard-host "http://ardhost.gov"
                server/iwds-host "http://iwdshost.gov"]

    (is (= (server/ard-status "005015")
           {:status 200, :body {:ingested 0, :missing '("LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif")}}))))

(deftest filter-ard-status-test
  (with-redefs [server/ard-status (fn [i] {:status 200 :body {:ingested 10 :missing tiflist}})]
    (is (= {:status 200 :body {:ingested 10 
                               :missing ["http://192.168.43.5/ard/oli_tirs/ARD_Tile/2013/CU/005/015/LC08_CU_005015_20130415_20171016_C01_V01_SR.tar/LC08_CU_005015_20130415_20171016_C01_V01_SRB4.tif"
                                         "http://192.168.43.5/ard/tm/ARD_Tile/1984/CU/005/015/LT05_CU_005015_19840508_20170912_C01_V01_BT.tar/LT05_CU_005015_19840508_20170912_C01_V01_BTB6.tif"]}}

           (server/filter-ard-status "005015" 1983 2017)
))

    )
)
