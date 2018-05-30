(ns mastodon.clj.server-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [mastodon.clj.config :as config]
            [mastodon.clj.server :as server]
            [mastodon.clj.file :as file]
            [ring.mock.request :as mock]
            [mastodon.clj.persistance :as persist]
            [mastodon.clj.validation :as validation]
            [mastodon.clj.warehouse :as warehouse]))

(def tiflist ["http://192.168.43.5/ard/oli_tirs/ARD_Tile/2013/CU/005/015/LC08_CU_005015_20130415_20171016_C01_V01_SR.tar/LC08_CU_005015_20130415_20171016_C01_V01_SRB4.tif"
              "http://192.168.43.5/ard/tm/ARD_Tile/1984/CU/005/015/LT05_CU_005015_19840508_20170912_C01_V01_BT.tar/LT05_CU_005015_19840508_20170912_C01_V01_BTB6.tif"
              "http://192.168.43.5/ard/tm/ARD_Tile/1982/CU/005/015/LT04_CU_005015_19821119_20170912_C01_V01_SR.tar/LT04_CU_005015_19821119_20170912_C01_V01_SRB5.tif"])

(deftest bulk-ingest-test
  (with-redefs [persist/ingest (fn [a b] "tif")]
    (is (= (server/bulk-ingest {:body {:urls "list,of,ard"}})
           {:status 200 :body '("tif" "tif" "tif")}))))

(deftest http-deps-check-test
  (with-redefs [config/config {:ard_host "ard-host.com" :chipmunk_host "chip-host.com"} 
                validation/http-accessible? (fn [host name] false)]
    (is (= {:error "ARD Host: ard-host.com is not reachable. CHIPMUNK Host: chip-host.com is not reachable"}
           (server/http-deps-check)))))

(deftest available-ard-test
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

(deftest filtered-ard-test
  (with-redefs [server/available-ard (fn [i] tiflist)]
    (is (= (server/filtered-ard "005015" 1983 1994)
           ["http://192.168.43.5/ard/tm/ARD_Tile/1984/CU/005/015/LT05_CU_005015_19840508_20170912_C01_V01_BT.tar/LT05_CU_005015_19840508_20170912_C01_V01_BTB6.tif"]))))

(deftest data-report-test
  (is (= (server/data-report ["LC08_CU_005015_20130415_20171016_C01_V01_SRB4.tif" "LT05_CU_005015_19840508_20170912_C01_V01_BTB6.tif" "LT04_CU_005015_19821119_20170912_C01_V01_SRB5.tif"]
                             ["LC08_CU_005015_20130415_20171016_C01_V01_SRB4.tif" "LT05_CU_005015_19840508_20170912_C01_V01_BTB6.tif"])
         {:missing ["LT04_CU_005015_19821119_20170912_C01_V01_SRB5.tif"]
          :ingested 2})))

(deftest ard-status-test
  (with-redefs [file/get-filenames (fn [path x] ["LE07_CU_005015_20021221_20170919_C01_V01_BT.tar"])
                persist/status-check (fn [tif x y] {"LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif" "[]"})
                validation/http-accessible? (fn [x y] true)
                config/config {:server_type "ard" :ard_host "http://ardhost.gov" :chipmunk_host "http://iwdshost.gov"}]

    (is (= (server/get-status "005015" {})
           {:status 200, :body {:ingested 0, :missing '("LE07_CU_005015_20021221_20170919_C01_V01_BTB6.tif")}}))))

(deftest aux-status-test
  (with-fake-http [{:url "http://auxhost.com/aux" :method :get} {:status 200 :body "<html><head>auxhead</head><body>AUX_CU_005015_20000731_20171031_V01.tar</body></html>"}]
    (with-redefs [config/config {:server_type "aux" :aux_host "http://auxhost.com/aux"}
                  server/aux-tifs (fn [tile] ["AUX_CU_005015_20000731_20171031_V01_ASPECT.tif" "AUX_CU_005015_20000731_20171031_V01_POSIDEX.tif" "AUX_CU_005015_20000731_20171031_V01_TRENDS.tif"])
                  warehouse/ingested-tifs (fn [tile] ["AUX_CU_005015_20000731_20171031_V01_ASPECT.tif"])
                  server/http-deps-check (fn [] {})]
      (is (= (server/get-status "005015" {})
             {:status 200 
              :body {:missing ["AUX_CU_005015_20000731_20171031_V01_TRENDS.tif" "AUX_CU_005015_20000731_20171031_V01_POSIDEX.tif"] 
                     :ingested 1}})))))

