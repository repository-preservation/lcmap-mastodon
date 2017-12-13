(ns lcmap.mastodon.ard
  (:require [clojure.string :as string]
            [clojure.set :as set]
            [lcmap.mastodon.util :as util]))

(def L457-ard-map
  (hash-map :QA '("LINEAGEQA" "PIXELQA" "RADSATQA" "SRATMOSOPACITYQA" "SRCLOUDQA")
            :SR '("SRB1" "SRB2" "SRB3" "SRB4" "SRB5" "SRB7")
            :BT '("BTB6")
            :TA '("TAB1" "TAB2" "TAB3" "TAB4" "TAB5" "TAB7"))
)

(def L8-ard-map
  (hash-map :QA '("LINEAGEQA" "PIXELQA" "RADSATQA" "SRAEROSOLQA")
            :SR '("SRB1" "SRB2" "SRB3" "SRB4" "SRB5" "SRB6" "SRB7" )
            :BT '("BTB10" "BTB11")
            :TA '("TAB1" "TAB2" "TAB3" "TAB4" "TAB5" "TAB6" "TAB7" "TAB9")))

(def tar-map
  (hash-map :LC08 L8-ard-map
            :LE07 L457-ard-map
            :LT05 L457-ard-map
            :LT04 L457-ard-map))

(defn tar-name
  "Derive an ARD tif files original containing Tar file name"
  [tif-name]
  (let [base_name   (string/replace tif-name ".tif" "")
        base_list   (string/split base_name "_")
        base_suffix (last base_list)
        base_prefix (keyword (first base_list))
        tar_suffix  (name (util/key-for-value (base_prefix tar-map) base_suffix))]
      (str (string/replace base_name base_suffix tar_suffix) ".tar"))
)

(defn ard-manifest
  "From a ARD tar file name, return a list of that tar
   files expected contents.
   ^String :ard-tar:"
  [ard-tar]
  (let [tar-all   (-> ard-tar (string/replace ".tar" "") (string/split "_"))
        tar-pre   (string/join "_" (take 7 tar-all))
        key-last  (keyword (last tar-all))
        key-first (keyword (first tar-all))
        ard-files (key-last (key-first tar-map))]
    (map (fn [i] (str tar-pre "_" i ".tif")) ard-files))
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

(defn ard-iwds-report
  "Return hash map of set differences for ARD and IWDS holdings"
  [ard-tifs iwds-tifs]
  (hash-map :ard-only (set/difference ard-tifs iwds-tifs)
            :iwd-only (set/difference iwds-tifs ard-tifs)
            :ingested (set/intersection ard-tifs iwds-tifs))
)

(defn tif-path [tif rpath]
  (let [tar (tar-name tif)]
    (str rpath tar "/" tif))
)
