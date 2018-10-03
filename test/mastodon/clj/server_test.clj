(ns mastodon.clj.server-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [mastodon.clj.config :as config]
            [mastodon.clj.server :as server]
            [mastodon.clj.file :as file]
            [ring.mock.request :as mock]
            [mastodon.clj.persistance :as persist]
            [mastodon.clj.validation :as validation]))

(def tiflist ["http://192.168.43.5/ard/oli_tirs/ARD_Tile/2013/CU/005/015/LC08_CU_005015_20130415_20171016_C01_V01_SR.tar/LC08_CU_005015_20130415_20171016_C01_V01_SRB4.tif"
              "http://192.168.43.5/ard/tm/ARD_Tile/1984/CU/005/015/LT05_CU_005015_19840508_20170912_C01_V01_BT.tar/LT05_CU_005015_19840508_20170912_C01_V01_BTB6.tif"
              "http://192.168.43.5/ard/tm/ARD_Tile/1982/CU/005/015/LT04_CU_005015_19821119_20170912_C01_V01_SR.tar/LT04_CU_005015_19821119_20170912_C01_V01_SRB5.tif"])

(def filelist ["LC08_CU_005015_20130415_20171016_C01_V01_SR.tar"
               "LT05_CU_005015_19840508_20170912_C01_V01_BT.tar"
               "LT04_CU_005015_19821119_20170912_C01_V01_SR.tar"])

(deftest bulk-ingest-test
  (with-redefs [persist/ingest (fn [a b] "tif")]
    (is (= (server/bulk-ingest {:body {:urls "list,of,ard"}})
           {:status 200 :body '("tif" "tif" "tif")}))))

(deftest bulk-ingest-test-error
  (with-redefs [config/config {:chipmunk_host "foohost.gov"}]
    (let [resp (server/bulk-ingest {:body {:data ""}})]
      (is (string/includes? (get-in resp [:body :error]) "exception with bulk-ingest")))))

(deftest available-ard-test-tile-only
  (with-redefs [file/get-filenames (fn [a b] ["LC08_CU_005015_20130415_20171016_C01_V01_SR.tar" "LT05_CU_005015_19840508_20170912_C01_V01_BT.tar"])] 
    (is (= (server/available-ard "005015")
        '("LC08_CU_005015_20130415_20171016_C01_V01_SRB2.tif"
          "LC08_CU_005015_20130415_20171016_C01_V01_SRB3.tif" 
          "LC08_CU_005015_20130415_20171016_C01_V01_SRB4.tif" 
          "LC08_CU_005015_20130415_20171016_C01_V01_SRB5.tif" 
          "LC08_CU_005015_20130415_20171016_C01_V01_SRB6.tif" 
          "LC08_CU_005015_20130415_20171016_C01_V01_SRB7.tif" 
          "LC08_CU_005015_20130415_20171016_C01_V01_PIXELQA.tif" 
          "LT05_CU_005015_19840508_20170912_C01_V01_BTB6.tif")))))

(deftest available-ard-test-dates
  (with-redefs [file/get-filenames (fn [path sfx] filelist)]
    (is (= (server/available-ard "005015" 1983 1994)
           ["LT05_CU_005015_19840508_20170912_C01_V01_BTB6.tif"]))))

(deftest data-report-test
  (is (= (server/data-report ["LC08_CU_005015_20130415_20171016_C01_V01_SRB4.tif" "LT05_CU_005015_19840508_20170912_C01_V01_BTB6.tif" "LT04_CU_005015_19821119_20170912_C01_V01_SRB5.tif"]
                             ["LC08_CU_005015_20130415_20171016_C01_V01_SRB4.tif" "LT05_CU_005015_19840508_20170912_C01_V01_BTB6.tif"])
         {:missing ["LT04_CU_005015_19821119_20170912_C01_V01_SRB5.tif"]
          :ingested 2})))

(deftest ard-status-test
  (with-redefs [config/config {:data_type "ard" :data_host "http://datahost.gov" :chipmunk_host "http://iwdshost.gov"}
                server/data-tifs (fn [tile req] ["LC08_CU_005015_20130415_20171016_C01_V01_SRB4.tif" "LT05_CU_005015_19840508_20170912_C01_V01_BTB6.tif" "LT04_CU_005015_19821119_20170912_C01_V01_SRB5.tif"])
                persist/ingested-tifs (fn [tile] ["LC08_CU_005015_20130415_20171016_C01_V01_SRB4.tif" "LT05_CU_005015_19840508_20170912_C01_V01_BTB6.tif"])]
    (is (= (server/get-status "005015" {})
           {:status 200 :body {:missing ["LT04_CU_005015_19821119_20170912_C01_V01_SRB5.tif"] :ingested 2}}))))

(deftest aux-status-test
  (with-redefs [config/config {:data_type "aux" :data_host "http://datahost.gov" :chipmunk_host "http://iwdshost.gov"}
                server/data-tifs (fn [tile req] ["AUX_CU_005015_20000731_20171031_V01_ASPECT.tif" "AUX_CU_005015_20000731_20171031_V01_POSIDEX.tif" "AUX_CU_005015_20000731_20171031_V01_TRENDS.tif"])
                persist/ingested-tifs (fn [tile] ["AUX_CU_005015_20000731_20171031_V01_ASPECT.tif"])]
    (is (= (server/get-status "005015" {})
           {:status 200 :body {:missing ["AUX_CU_005015_20000731_20171031_V01_TRENDS.tif" "AUX_CU_005015_20000731_20171031_V01_POSIDEX.tif"] :ingested 1}}))))


