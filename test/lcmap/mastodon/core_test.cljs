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




(deftest hv-map-test
  (is 
    (= {:h "002" :v "999"} 
       (mc/hv-map "002999"))))

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

(deftest ard-url-format-test
  (is
    (= "http://magichost.org/043999/"
       (mc/ard-url-format "http://magichost.org" "043999"))))

(deftest idw-url-format-test
  (is
    (= "http://magichost.org/inventory?tile=043999"
       (mc/idw-url-format "http://magichost.org" "043999"))))

(deftest key-for-value-test
  (is (= :SR (util/key-for-value ard/tar-map "SRB1")))
  (is (= :TA (util/key-for-value ard/tar-map "TAB4")))
  (is (= :QA (util/key-for-value ard/tar-map "PIXELQA")))
  (is (= :BT (util/key-for-value ard/tar-map "BTB10"))))

(defn test-async
  "Asynchronous test awaiting ch to produce a value or close."
  [ch]
    (let [done (fn [] (prn "done with async test"))]
      (async done
        (async/take! ch (fn [_] (done)))))
)

(deftest ard-status-check-test
  (let [achan (async/chan 1)
        rchan (async/chan 1)]
    (go 
      (async/>! achan (util/collect-map-values (async/<! (testdata/ard-resp)) :name :type "file"))
      (mc/ard-status-check achan "idw.com" (mhttp/mock-idw) "bdiv" "ibtn" "ictr" "mctr" (fn [i] (str i)) rchan)
      )

    (test-async
      (go (is (= 12 (:mis-cnt (async/<! rchan))))))
  ) 
)
