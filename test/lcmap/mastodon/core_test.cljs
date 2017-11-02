(ns lcmap.mastodon.core-test
  (:require [cljs.test :refer-macros [deftest is]]
            [clojure.string :as string]
            [lcmap.mastodon.core :as mc]
            [lcmap.mastodon.http :as mhttp]))

(def xmap (hash-map :a "a" :b "b" :c "c" :d "foo"))
(def xmaplist [(hash-map :a "a" :b "b" :c "c" :d "foo")
               (hash-map :a "a" :b "b" :c "c" :d "bar")
               (hash-map :a "a" :b "b" :c "c" :d "baz")])

(defn httpget
  [url]
  (if (string/includes? url "ardhost")
    ;; faux ard response
    (list {:name "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar" :type "file"} 
          {:name "LC08_CU_027009_20130701_20170729_C01_V01_TA.tar" :type "file"})
    ;; faux idw response
    (hash-map :result (list {:source "LC08_CU_027009_20130701_20170729_C01_V01_SRB6.tif"} 
                            {:source "LC08_CU_027009_20130701_20170729_C01_V01_SRB7.tif"} 
                            {:source "LC08_CU_027009_20130701_20170729_C01_V01_TAB1.tif"}
                            {:source "LC08_CU_027009_20130701_20170729_C01_V01_TAB2.tif"}))
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
    (= '("foo" "bar" "baz")
       (mc/collect-map-values xmaplist :d :c "c"))
  )
)

(deftest collect-map-values-nil-conditional-test
  (is
    (= '("foo" "bar" "baz")
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
    (= {"ard-only"
        #{"LC08_CU_027009_20130701_20170729_C01_V01_TAB8.tif"
          "LC08_CU_027009_20130701_20170729_C01_V01_SRB4.tif"
          "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"
          "LC08_CU_027009_20130701_20170729_C01_V01_TAB7.tif"
          "LC08_CU_027009_20130701_20170729_C01_V01_TAB3.tif"
          "LC08_CU_027009_20130701_20170729_C01_V01_SRB3.tif"
          "LC08_CU_027009_20130701_20170729_C01_V01_TAB5.tif"
          "LC08_CU_027009_20130701_20170729_C01_V01_SRB5.tif"
          "LC08_CU_027009_20130701_20170729_C01_V01_TAB4.tif"
          "LC08_CU_027009_20130701_20170729_C01_V01_TAB9.tif"
          "LC08_CU_027009_20130701_20170729_C01_V01_SRB2.tif"
          "LC08_CU_027009_20130701_20170729_C01_V01_TAB6.tif"},
        "idw-only" #{}}
       (mc/inventory-diff "http://ardhost.com" "http://idwhost.com" "043029" "CONUS" httpget)
    )
  )
)
