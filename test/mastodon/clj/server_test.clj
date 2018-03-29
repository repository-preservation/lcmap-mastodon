(ns mastodon.clj.server-test
  (:require [clojure.test :refer :all]
            [mastodon.clj.server :as server]
            [mastodon.clj.file :as file]
            [ring.mock.request :as mock]
            [mastodon.clj.persistance :as persist]
            [mastodon.clj.validation :as validation]))

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
