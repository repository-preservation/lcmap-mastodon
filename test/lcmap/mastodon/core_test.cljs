(ns lcmap.mastodon.core-test
  (:require [cljs.test :refer-macros [deftest is]]
            [clojure.string :as string]
            [lcmap.mastodon.core :as mc]))

(def xmap (hash-map :a "a" :b "b" :c "c" :d "foo"))
(def xmaplist [(hash-map :a "a" :b "b" :c "c" :d "foo")
               (hash-map :a "a" :b "b" :c "c" :d "bar")
               (hash-map :a "a" :b "b" :c "c" :d "baz")])

(defn httpget
  [url]
  (if (string/includes? url "ardhost")
    ;; faux ard request
    (list {:name "baz.tar" :type "file"} 
          {:name "boo.tar" :type "file"} 
          {:name "foo.tar" :type "file"} 
          {:name "now.tar" :type "file"})
    ;; faux idw request
    (hash-map :result (list {:source "foo.tar"} 
                            {:source "baz.tar"} 
                            {:source "bar.tar"}))
  )
)

(deftest hv-map-test
  (is 
    (= {:h "002" :v "999"} 
       (mc/hv-map "002999"))
  )
)

(deftest tile-id-rest-test
  (is
    (= "111/222/"
       (mc/tile-id-rest "111222"))
  )
)

(deftest get-map-val-test
  (is
    (= "foo" 
       (mc/get-map-val xmap :d :b "b"))
  )
)

(deftest get-map-val-nil-conditional-test
  (is
    (= "foo"
       ;;(mc/get-map-val xmap :d :nonexistentkey nil))
       (mc/get-map-val xmap :d))
  )
)

(deftest collect-map-values-test
  (is
    (= #{"foo" "bar" "baz"}
       (mc/collect-map-values xmaplist :d :c "c"))
  )
)

(deftest collect-map-values-nil-conditional-test
  (is
    (= #{"foo" "bar" "baz"}
       (mc/collect-map-values xmaplist :d))
  )
)

(deftest ard-url-format-test
  (is
    (= "http://magichost.org/043/999/"
       (mc/ard-url-format "http://magichost.org" "043999"))
  )
)

(deftest idw-url-format-test
  (is
    (= "http://magichost.org/inventory?tile=043999"
       (mc/idw-url-format "http://magichost.org" "043999"))
  )
)

(deftest inventory-diff-test
  (is
    (= [#{"boo.tar" "now.tar"} #{"bar.tar"}]
       (mc/inventory-diff "http://ardhost.com" "http://idwhost.com" "043029" "CONUS" httpget)
    )
  )
)
