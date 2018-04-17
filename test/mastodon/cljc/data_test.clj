(ns mastodon.cljc.data-test
  (:require [clojure.test :refer :all] 
            [mastodon.cljc.data :as data]))

(deftest tar-name-test
  (let [tif-name "LC08_CU_022010_20131211_20171016_C01_V01_PIXELQA.tif"
        tar-name "LC08_CU_022010_20131211_20171016_C01_V01_SR.tar"]
    (is (= tar-name (data/ard-tar-name tif-name)))))

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
    (is (= l5resp (data/ard-manifest "LT05_CU_022010_19841204_20170912_C01_V01_SR.tar")))
    (is (= l8resp (data/ard-manifest "LC08_CU_022010_19841204_20170912_C01_V01_SR.tar")))))

(deftest tar-path-test
  (is (= "oli_tirs/ARD_Tile/2002/CU/005/015"
         (data/tar-path "LC08_CU_005015_20021221_20170919_C01_V01_BT.tar"))))

