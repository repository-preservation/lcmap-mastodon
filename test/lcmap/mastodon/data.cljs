(ns lcmap.mastodon.data)

(def xmap (hash-map :a "a" :b "b" :c "c" :d "foo"))
(def xmaplist [(hash-map :a "a" :b "b" :c "c" :d "foo")
               (hash-map :a "a" :b "b" :c "c" :d "bar")
               (hash-map :a "a" :b "b" :c "c" :d "baz")])

(def ard-resp (list {:name "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar" :type "file"} 
                    {:name "LC08_CU_027009_20130701_20170729_C01_V01_TA.tar" :type "file"}))

(def idw-resp (hash-map :result (list {:source "LC08_CU_027009_20130701_20170729_C01_V01_SRB6.tif"} 
                                      {:source "LC08_CU_027009_20130701_20170729_C01_V01_SRB7.tif"} 
                                      {:source "LC08_CU_027009_20130701_20170729_C01_V01_TAB1.tif"}
                                      {:source "LC08_CU_027009_20130701_20170729_C01_V01_TAB2.tif"})))

(def diff-resp-a
  {"ard-only"
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
   "idw-only" 
   #{}
  }
)
