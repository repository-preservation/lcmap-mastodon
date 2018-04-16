(ns mastodon.cljc.util-test
  (:require [clojure.test :refer :all]
            [mastodon.cljc.data :as data]
            [mastodon.cljc.util :as util]))

(def xmap (hash-map :a "a" :b "b" :c "c" :d "foo"))
(def xmaplist [(hash-map :a "a" :b "b" :c "c" :d "foo")
               (hash-map :a "a" :b "b" :c "c" :d "bar")
               (hash-map :a "a" :b "b" :c "c" :d "baz")])

(deftest get-map-val-test
  (is
    (= "foo" 
       (util/get-map-val xmap :d :b "b"))))

(deftest get-map-val-nil-conditional-test
  (is
    (= "foo"
       (util/get-map-val xmap :d))))

(deftest collect-map-values-test
  (is
    (= '("foo" "bar" "baz")
       (util/collect-map-values xmaplist :d :c "c"))))

(deftest collect-map-values-nil-conditional-test
  (is
    (= '("foo" "bar" "baz")
       (util/collect-map-values xmaplist :d))))

(deftest hv-map-test
  (is 
    (= {:h "002" :v "999"} 
       (util/hv-map "002999"))))

(deftest inventory-url-format-test
  (is
    (= "http://magichost.org/inventory/043999"
       (util/inventory-url-format "http://magichost.org" "043999"))))

(deftest iwds-url-format-test
  (is
    (= "http://magichost.org/inventory?only=source&tile=043999"
       (util/iwds-url-format "http://magichost.org" "043999"))))

(deftest key-for-value-test
  (is (= :SR (util/key-for-value data/L8-ard-map "SRB2")))
  (is (= :SR (util/key-for-value data/L8-ard-map "PIXELQA")))
  (is (= :BT (util/key-for-value data/L8-ard-map "BTB10")))
  (is (= :SR (util/key-for-value data/L457-ard-map "SRB1")))
  (is (= :SR (util/key-for-value data/L457-ard-map "PIXELQA")))
  (is (= :BT (util/key-for-value data/L457-ard-map "BTB6"))))

(deftest with-suffix-test
  (is (= '("foo.tar") 
         (util/with-suffix ["foo.tar" "foo.xml"] "tar"))))

(deftest trailing-slash-withslash-test
  (is (= (util/trailing-slash "foo/") "foo/")))

(deftest trailing-slash-withoutslash-test
  (is (= (util/trailing-slash "foo") "foo/")))

(deftest string-to-list-test
  (is (= (set (util/string-to-list "[foo,bar,huh]")) 
         (set ["foo" "bar" "huh"]))))



