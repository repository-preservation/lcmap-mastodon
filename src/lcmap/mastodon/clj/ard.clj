(ns lcmap.mastodon.clj.ard
  (:require [clojure.string :as string]
            [clojure.set :as set]
            [lcmap.mastodon.cljc.util :as util]))

(def L457-ard-map
  (hash-map :SR '("SRB1" "SRB2" "SRB3" "SRB4" "SRB5" "SRB7" "PIXELQA")
            :BT '("BTB6")))

(def L8-ard-map
  (hash-map :SR '("SRB2" "SRB3" "SRB4" "SRB5" "SRB6" "SRB7" "PIXELQA")
            :BT '("BTB10")))

(def tar-map
  (hash-map :LC08 L8-ard-map
            :LE07 L457-ard-map
            :LT05 L457-ard-map
            :LT04 L457-ard-map))

(defn tar-name
  "Derive an ARD tif files original containing Tar file name"
  [tif-name]
  (let [base_name   (string/replace tif-name ".tif" "")
        base_list   (string/split base_name #"_")
        base_suffix (last base_list)
        base_prefix (keyword (first base_list))
        tar_suffix  (name (util/key-for-value (base_prefix tar-map) base_suffix))]
      (str (string/replace base_name base_suffix tar_suffix) ".tar")))

(defn full-name 
  [tif-name]
  (str (tar-name tif-name) "/" tif-name)
)

(defn ard-manifest
  "From a ARD tar file name, return a list of that tar
   files expected contents.
   ^String :ard-tar:"
  [ard-tar]
  (let [tar-all   (-> ard-tar (string/trim) (string/replace ".tar" "") (string/split #"_"))
        tar-pre   (string/join "_" (take 7 tar-all))
        key-last  (keyword (last tar-all))
        key-first (keyword (first tar-all))
        ard-files (key-last (key-first tar-map))]
    (map (fn [i] (str tar-pre "_" i ".tif")) ard-files)))

(defn expand-tars
  "Return set of tifs from list of tars"
  [tars]
  (set (flatten (map ard-manifest tars))))

(defn iwds-tifs
  [iwds-response & [map-key]]
  (let [errors (string/includes? iwds-response "errors")
        mkey   (or map-key :source)
        tifs   (-> iwds-response
                   (util/collect-map-values mkey)
                   (set))]
    (if errors
      (hash-map :errors [iwds-response] :tifs #{})
      (hash-map :errors nil :tifs tifs))))

(defn ard-iwds-report
  "Return hash map of set differences for ARD and IWDS holdings"
  [ard-tifs iwds-tifs]
  (hash-map :ard-only (sort (vec (set/difference ard-tifs iwds-tifs)))    
            :iwd-only (sort (vec (set/difference iwds-tifs ard-tifs)))    
            :ingested (sort (vec (set/intersection ard-tifs iwds-tifs)))))

(defn tar-path [tar]
  ;; /tm/ARD_Tile/2011/CU/015/005/LT05_CU_015005_20111110_20170926_C01_V01_SR.tar
  (let [mission-map {"LT04" "tm" "LT05" "tm" "LE07" "etm" "LC08" "oli_tirs"}
        parts-list (-> tar (string/replace ".tar" "") (string/split #"_"))
        mission  (get mission-map (nth parts-list 0)) 
        year (-> (nth parts-list 3) (subs 0 4))
        location (nth parts-list 1)
        hhh  (-> (nth parts-list 2) (subs 0 3))
        vvv  (-> (nth parts-list 2) (subs 3 6))]
      (str mission "/ARD_Tile/" year "/" location "/" hhh "/" vvv)))

(defn tif-path [tif rpath ingpath]
  (let [tar (tar-name tif)
        tarpath (tar-path tar)
        resource (if (empty? ingpath) rpath ingpath)]    
    (str resource "/" tarpath "/" tar "/" tif)))
