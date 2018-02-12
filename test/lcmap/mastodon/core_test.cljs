(ns lcmap.mastodon.core-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [deftest is async]]
            [clojure.string :as string]
            [lcmap.mastodon.cljs.core :as mc]
            [lcmap.mastodon.cljs.http :as mhttp]
            [lcmap.mastodon.data :as testdata]
            [lcmap.mastodon.cljc.util :as util]
            [lcmap.mastodon.cljc.ard :as ard]
            [cljs.core.async :as async]))

;; util tests
(deftest get-map-val-test
  (is
    (= "foo" 
       (util/get-map-val testdata/xmap :d :b "b"))))

(deftest get-map-val-nil-conditional-test
  (is
    (= "foo"
       ;;(mc/get-map-val xmap :d :nonexistentkey nil))
       (util/get-map-val testdata/xmap :d))))

(deftest collect-map-values-test
  (is
    (= '("foo" "bar" "baz")
       (util/collect-map-values testdata/xmaplist :d :c "c"))))

(deftest collect-map-values-nil-conditional-test
  (is
    (= '("foo" "bar" "baz")
       (util/collect-map-values testdata/xmaplist :d))))

;; core tests
(deftest hv-map-test
  (is 
    (= {:h "002" :v "999"} 
       (util/hv-map "002999"))))

(deftest ard-url-format-test
  (is
    (= "http://magichost.org/ard/043999"
       (util/ard-url-format "http://magichost.org" "043999"))))


(deftest iwds-url-format-test
  (is
    (= "http://magichost.org/inventory?only=source&tile=043999"
       (util/iwds-url-format "http://magichost.org" "043999"))))

;; ard tests
(deftest key-for-value-test
  (is (= :SR (util/key-for-value ard/L8-ard-map "SRB2")))
  (is (= :SR (util/key-for-value ard/L8-ard-map "PIXELQA")))
  (is (= :BT (util/key-for-value ard/L8-ard-map "BTB10")))
  (is (= :SR (util/key-for-value ard/L457-ard-map "SRB1")))
  (is (= :SR (util/key-for-value ard/L457-ard-map "PIXELQA")))
  (is (= :BT (util/key-for-value ard/L457-ard-map "BTB6")))
)

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
    (is (= l8resp (ard/ard-manifest "LC08_CU_022010_19841204_20170912_C01_V01_SR.tar"))))
)

(deftest ard-tar-name-test
  (let [tif-name "LC08_CU_022010_20131211_20171016_C01_V01_PIXELQA.tif"
        tar-name "LC08_CU_022010_20131211_20171016_C01_V01_SR.tar"]
    (is (= tar-name (ard/tar-name tif-name))))
)

(deftest iwds-tifs-test
  (let [inputs '({:foo "bar" :source "baz.tif"} {:bo "jenkins" :source "maz.tif"})]
    (is (= #{"baz.tif" "maz.tif"}  (:tifs (ard/iwds-tifs inputs)))))
)

(deftest ard-iwds-report-test
  (let [ard-tifs #{:a :b :c}
        iwds-tifs #{:a :d :e}
        resp (ard/ard-iwds-report ard-tifs iwds-tifs)]
      (is (= (:ard-only resp) '(:b :c)))
      (is (= (:iwd-only resp) '(:d :e)))
      (is (= (:ingested resp) '(:a))))
)

(deftest tif-path-test
  (let [tif "LC08_CU_022010_20131211_20171016_C01_V01_PIXELQA.tif"
        pth "http://foobar.cr.usgs.gov/ard"
        ipth ""
        rsp (ard/tif-path tif pth ipth)]
    (is (= rsp "http://foobar.cr.usgs.gov/ard/oli_tirs/ARD_Tile/2013/CU/022/010/LC08_CU_022010_20131211_20171016_C01_V01_SR.tar/LC08_CU_022010_20131211_20171016_C01_V01_PIXELQA.tif"))
  )
)

(deftest tif-path-w-ingest-test
  (let [tif "LC08_CU_022010_20131211_20171016_C01_V01_PIXELQA.tif"
        pth "http://foobar.cr.usgs.gov/ard"
        ipth "http://barfoo.cr.usgs.gov/ardlinks"
        rsp (ard/tif-path tif pth ipth)]
    (is (= rsp "http://barfoo.cr.usgs.gov/ardlinks/oli_tirs/ARD_Tile/2013/CU/022/010/LC08_CU_022010_20131211_20171016_C01_V01_SR.tar/LC08_CU_022010_20131211_20171016_C01_V01_PIXELQA.tif"))
  )
)

(defn test-async
  "Asynchronous test awaiting ch to produce a value or close."
  [ch]
    (let [done (fn [] (prn "done with async test"))]
      (async done
        (async/take! ch (fn [_] (done)))))
)

;; (deftest ard-status-check-test
;;   (let [achan (async/chan 1)
;;         rchan (async/chan 1)]
;;     (go 
;;       (async/>! achan (util/collect-map-values (async/<! (testdata/ard-resp)) :name :type "file"))
;;       (mc/ard-status-check achan "idw.com" (mhttp/mock-idw) "bdiv" "ibtn" "ictr" "mctr" (fn [i] (str i)) rchan)
;;       )

;;     (test-async
;;       (go (is (= 12 (:mis-cnt (async/<! rchan))))))
;;   ) 
;; )
