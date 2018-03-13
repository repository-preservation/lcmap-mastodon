(ns mastodon.cljc.ard-test
  (:require [clojure.test :refer :all] 
            [mastodon.cljc.ard :as ard]))

(deftest tar-name-test
  (let [tif-name "LC08_CU_022010_20131211_20171016_C01_V01_PIXELQA.tif"
        tar-name "LC08_CU_022010_20131211_20171016_C01_V01_SR.tar"]
    (is (= tar-name (ard/tar-name tif-name)))))

(deftest full-name-test
  (is (= "LE07_CU_005015_20021221_20170919_C01_V01_SR.tar/LE07_CU_005015_20021221_20170919_C01_V01_SRB1.tif" 
         (ard/full-name "LE07_CU_005015_20021221_20170919_C01_V01_SRB1.tif"))))

(deftest ard-manifest-test
  (let [l5resp '("LT05_CU_022010_19841204_20170912_C01_V01_SRB1.tif"
                 "LT05_CU_022010_19841204_20170912_C01_V01_SRB2.tif"
                 "LT05_CU_022010_19841204_20170912_C01_V01_SRB3.tif"
                 "LT05_CU_022010_19841204_20170912_C01_V01_SRB4.tif"
                 "LT05_CU_022010_19841204_20170912_C01_V01_SRB5.tif"
                 "LT05_CU_022010_19841204_20170912_C01_V01_SRB7.tif"
                 "LT05_CU_022010_19841204_20170912_C01_V01_PIXELQA.tif")
        l8resp '("LC08_CU_022010_19841204_20170912_C01_V01_SRB2.tif"
                 "LC08_CU_022010_19841204_20170912_C01_V01_SRB3.tif"
                 "LC08_CU_022010_19841204_20170912_C01_V01_SRB4.tif"
                 "LC08_CU_022010_19841204_20170912_C01_V01_SRB5.tif"
                 "LC08_CU_022010_19841204_20170912_C01_V01_SRB6.tif"
                 "LC08_CU_022010_19841204_20170912_C01_V01_SRB7.tif"
                 "LC08_CU_022010_19841204_20170912_C01_V01_PIXELQA.tif")]
    (is (= l5resp (ard/ard-manifest "LT05_CU_022010_19841204_20170912_C01_V01_SR.tar")))
    (is (= l8resp (ard/ard-manifest "LC08_CU_022010_19841204_20170912_C01_V01_SR.tar")))))

(deftest expand-tars-test
  (is (= #{"LE07_CU_005015_20021221_20170919_C01_V01_SRB2.tif" 
           "LE07_CU_005015_20021221_20170919_C01_V01_SRB4.tif"
           "LE07_CU_005015_20021221_20170919_C01_V01_SRB7.tif"
           "LC08_CU_005015_20021221_20170919_C01_V01_BTB10.tif"
           "LE07_CU_005015_20021221_20170919_C01_V01_PIXELQA.tif"
           "LE07_CU_005015_20021221_20170919_C01_V01_SRB1.tif"
           "LE07_CU_005015_20021221_20170919_C01_V01_SRB5.tif"
           "LE07_CU_005015_20021221_20170919_C01_V01_SRB3.tif"}
         (ard/expand-tars ["LE07_CU_005015_20021221_20170919_C01_V01_SR.tar"
                           "LC08_CU_005015_20021221_20170919_C01_V01_BT.tar"]))))

(deftest iwds-tifs-test
  (let [inputs '({:foo "bar" :source "baz.tif"} {:bo "jenkins" :source "maz.tif"})]
    (is (= #{"baz.tif" "maz.tif"}  (:tifs (ard/iwds-tifs inputs))))))

(deftest ard-iwds-report-test
  (let [ard-tifs #{:a :b :c}
        iwds-tifs #{:a :d :e}
        resp (ard/ard-iwds-report ard-tifs iwds-tifs)]
      (is (= (:ard-only resp) '(:b :c)))
      (is (= (:iwd-only resp) '(:d :e)))
      (is (= (:ingested resp) '(:a)))))

(deftest tar-path-test
  (is (= "oli_tirs/ARD_Tile/2002/CU/005/015"
         (ard/tar-path "LC08_CU_005015_20021221_20170919_C01_V01_BT.tar"))))

(deftest tif-path-test
  (let [tif "LC08_CU_022010_20131211_20171016_C01_V01_PIXELQA.tif"
        pth "http://foobar.cr.usgs.gov/ard"
        rsp (ard/tif-path tif pth)]
    (is (= rsp 
           "http://foobar.cr.usgs.gov/ard/oli_tirs/ARD_Tile/2013/CU/022/010/LC08_CU_022010_20131211_20171016_C01_V01_SR.tar/LC08_CU_022010_20131211_20171016_C01_V01_PIXELQA.tif"))))
