(ns lcmap.mastodon.core-test
  (:require [cljs.test :refer-macros [deftest is]]
            [clojure.string :as string]
            [lcmap.mastodon.core :as mc]
            [lcmap.mastodon.http :as mhttp]
            [lcmap.mastodon.data :as testdata]
            [lcmap.mastodon.ard-maps :as ard-maps]))

(deftest hv-map-test
  (is 
    (= {:h "002" :v "999"} 
       (mc/hv-map "002999"))))

(deftest tile-id-rest-test
  (is
    (= "111/222/"
       (mc/tile-id-rest "111222"))))

(deftest get-map-val-test
  (is
    (= "foo" 
       (mc/get-map-val testdata/xmap :d :b "b"))))

(deftest get-map-val-nil-conditional-test
  (is
    (= "foo"
       ;;(mc/get-map-val xmap :d :nonexistentkey nil))
       (mc/get-map-val testdata/xmap :d))))

(deftest collect-map-values-test
  (is
    (= '("foo" "bar" "baz")
       (mc/collect-map-values testdata/xmaplist :d :c "c"))))

(deftest collect-map-values-nil-conditional-test
  (is
    (= '("foo" "bar" "baz")
       (mc/collect-map-values testdata/xmaplist :d))))

(deftest ard-url-format-test
  (is
    (= "http://magichost.org/043/999/"
       (mc/ard-url-format "http://magichost.org" "043999"))))

(deftest idw-url-format-test
  (is
    (= "http://magichost.org/inventory?tile=043999"
       (mc/idw-url-format "http://magichost.org" "043999"))))

(deftest key-for-value-test
  (is (= :SR (mc/key-for-value ard-maps/tar-map "SRB1")))
  (is (= :TA (mc/key-for-value ard-maps/tar-map "TAB4")))
  (is (= :QA (mc/key-for-value ard-maps/tar-map "PIXELQA")))
  (is (= :BT (mc/key-for-value ard-maps/tar-map "BTB10"))))

