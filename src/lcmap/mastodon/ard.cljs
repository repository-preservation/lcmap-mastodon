(ns lcmap.mastodon.ard
  (:require [clojure.string :as string]
            [lcmap.mastodon.util :as util]))

(def tar-map
  (hash-map :QA '("LINEAGEQA" "PIXELQA" "RADSATQA" "SRAEROSOLQA")
            :SR '("SRB1" "SRB2" "SRB3" "SRB4" "SRB5" "SRB6" "SRB7" )
            :BT '("BTB10" "BTB11")
            :TA '("TAB1" "TAB2" "TAB3" "TAB4" "TAB5" "TAB6" "TAB7" "TAB8" "TAB9"))
)

(defn tar-name
  "Derive an ARD tif files original containing Tar file name"
  [tif-name]
  ;; LC08_CU_027009_20130701_20170729_C01_V01_PIXELQA.tif
  ;; LC08_CU_027009_20130701_20170729_C01_V01_QA.tar
  ;; (name :var_name) (keyword "str_name")
  (let [tname (string/replace tif-name ".tif" "")
        tlst  (string/split tname "_")
        tval  (last tlst)
        tkey  (name (util/key-for-value tar-map tval))]
       (str (string/replace tname tval tkey) ".tar") 
  )
)

(defn ard-manifest
  "From a ARD tar file name, generate a map 
   keyed by tar file name of its expected contents.

   ^String :ard-tar:"
  [ard-tar]
  (let [tname (string/replace ard-tar ".tar" "")
        tlst  (string/split tname "_")
        tval  (last tlst)
        tkey  (keyword tval)
        tifs  (tkey tar-map)]
      (map (fn [x] (str (string/replace tname tval x) ".tif")) tifs)
  )
)

(defn expand-tars
  "Return set of tifs from list of tars"
  [tars]
  (set (flatten (map ard-manifest tars)))
)

(defn iwds-tifs
  "Return set of tifs from list of maps"
  [iwds-map & [map-key]]
  (let [mkey (or map-key :source)]
    (-> iwds-map 
        (util/collect-map-values mkey)
        (set)))
)
