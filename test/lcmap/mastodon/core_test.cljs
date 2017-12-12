(ns lcmap.mastodon.core-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [deftest is async]]
            [clojure.string :as string]
            [lcmap.mastodon.core :as mc]
            [lcmap.mastodon.http :as mhttp]
            [lcmap.mastodon.data :as testdata]
            [lcmap.mastodon.util :as util]
            [lcmap.mastodon.ard :as ard]
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
       (mc/hv-map "002999"))))

(deftest ard-url-format-test
  (is
    (= "http://magichost.org/043999/"
       (mc/ard-url-format "http://magichost.org" "043999"))))

(deftest idw-url-format-test
  (is
    (= "http://magichost.org/inventory?tile=043999"
       (mc/idw-url-format "http://magichost.org" "043999"))))

;; ard tests
(deftest key-for-value-test
  (is (= :SR (util/key-for-value ard/L8-ard-map "SRB1")))
  (is (= :TA (util/key-for-value ard/L8-ard-map "TAB9")))
  (is (= :TA (util/key-for-value ard/L457-ard-map "TAB7")))
  (is (= :QA (util/key-for-value ard/L8-ard-map "SRAEROSOLQA")))
  (is (= :QA (util/key-for-value ard/L457-ard-map "SRATMOSOPACITYQA")))
  (is (= :BT (util/key-for-value ard/L8-ard-map "BTB10")))
  (is (= :BT (util/key-for-value ard/L457-ard-map "BTB6")))
)

(deftest ard-manifest-test
  (let [l5resp '("LT05_CU_022010_19841204_20170912_C01_V01_LINEAGEQA.tif"
                 "LT05_CU_022010_19841204_20170912_C01_V01_PIXELQA.tif"
                 "LT05_CU_022010_19841204_20170912_C01_V01_RADSATQA.tif"
                 "LT05_CU_022010_19841204_20170912_C01_V01_SRATMOSOPACITYQA.tif"
                 "LT05_CU_022010_19841204_20170912_C01_V01_SRCLOUDQA.tif")
        l8resp '("LC08_CU_022010_20131211_20171016_C01_V01_LINEAGEQA.tif"
                 "LC08_CU_022010_20131211_20171016_C01_V01_PIXELQA.tif"
                 "LC08_CU_022010_20131211_20171016_C01_V01_RADSATQA.tif"
                 "LC08_CU_022010_20131211_20171016_C01_V01_SRAEROSOLQA.tif")]
    (is (= l5resp (ard/ard-manifest "LT05_CU_022010_19841204_20170912_C01_V01_QA.tar")))
    (is (= l8resp (ard/ard-manifest "LC08_CU_022010_20131211_20171016_C01_V01_QA.tar"))))
)

(deftest ard-tar-name-test
  (let [tif-name "LC08_CU_022010_20131211_20171016_C01_V01_SRAEROSOLQA.tif"
        tar-name "LC08_CU_022010_20131211_20171016_C01_V01_QA.tar"]
    (is (= tar-name (ard/tar-name tif-name))))
)

(deftest iwds-tifs-test
  (let [inputs '({:foo "bar" :source "baz.tif"} {:bo "jenkins" :source "maz.tif"})]
    (is (= #{"baz.tif" "maz.tif"} (ard/iwds-tifs inputs))))
)

(deftest ard-iwds-report-test
  (let [ard-tifs #{:a :b :c}
        iwds-tifs #{:a :d :e}
        resp (ard/ard-iwds-report ard-tifs iwds-tifs)]
      (is (= (:ard-only resp) #{:b :c}))
      (is (= (:iwd-only resp) #{:d :e}))
      (is (= (:ingested resp) #{:a})))
)

(deftest tif-path-test
  (let [tif "LC08_CU_022010_20131211_20171016_C01_V01_SRAEROSOLQA.tif"
        pth "http://foobar.cr.usgs.gov/ard/"
        rsp (ard/tif-path tif pth)]
    (is (= rsp "http://foobar.cr.usgs.gov/ard/LC08_CU_022010_20131211_20171016_C01_V01_QA.tar/LC08_CU_022010_20131211_20171016_C01_V01_SRAEROSOLQA.tif"))
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
