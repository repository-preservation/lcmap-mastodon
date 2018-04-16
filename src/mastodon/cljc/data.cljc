(ns mastodon.cljc.data
  (:require [clojure.string :as string]
            [clojure.set :as set]
            [mastodon.cljc.util :as util]))

(def L457-ard-map
  "Band mapping for Landsat missions 4 through 7."
  (hash-map :SR '("SRB1" "SRB2" "SRB3" "SRB4" "SRB5" "SRB7" "PIXELQA")
            :BT '("BTB6")))

(def L8-ard-map
  "Band mapping for Landsat mission 8."
  (hash-map :SR '("SRB2" "SRB3" "SRB4" "SRB5" "SRB6" "SRB7" "PIXELQA")
            :BT '("BTB10")))

(def aux-vector
  "Layer list for Auxiliary data"
  '("ASPECT" "DEM" "MPW" "POSIDEX" "SLOPE" "TRENDS"))

(def tar-map
  "Aggregated map for each Landsat mission."
  (hash-map :LC08 L8-ard-map
            :LE07 L457-ard-map
            :LT05 L457-ard-map
            :LT04 L457-ard-map))

(defn ard-tar-name
  "Derive an ARD tif files original containing Tar file name."
  [tif-name]
  (try
    (let [base_name   (string/replace tif-name ".tif" "")
          base_list   (string/split base_name #"_")
          base_suffix (last base_list)
          base_prefix (keyword (first base_list))
          tar_suffix  (name (util/key-for-value (base_prefix tar-map) base_suffix))]
      (str (string/replace base_name base_suffix tar_suffix) ".tar"))
    (catch Exception ex
      (throw (ex-info (format "Exception in data/ard-tar-name: %s" (.getMessage ex)))))))

(defn aux-tar-name
  "Determine AUX tar file name from tif name"
  [tif-name]
  (try
    (let [base_name (string/replace tif-name ".tif" "")
          base_list (string/split base_name #"_")]
      (-> base_list (pop) (#(string/join "_" %)) (str ".tar")))
    (catch Exception ex
      (throw (ex-info (format "Exception in data/aux-tar-name: %s" (.getMessage ex)))))))

(defn ard-manifest
  "Return ARD tar files contents."
  [ard-tar]
  (let [tar-all   (-> ard-tar (string/trim) (string/replace ".tar" "") (string/split #"_"))
        tar-pre   (string/join "_" (take 7 tar-all))
        key-last  (keyword (last tar-all))
        key-first (keyword (first tar-all))
        ard-files (key-last (key-first tar-map))]
    (map (fn [i] (str tar-pre "_" i ".tif")) ard-files)))

(defn aux-manifest
  "Return AUX tar files contents."
  [aux-tar]
  (let [aux_name (-> aux-tar (string/trim) (string/replace ".tar" ""))]
     (doall (map (fn [i] (format "%s_%s.tif" aux_name i)) aux-vector))))

(defn tar-path 
  "Return the path to an ARD tar file, given a filename."
  [tar]
  (let [mission-map {"LT04" "tm" "LT05" "tm" "LE07" "etm" "LC08" "oli_tirs"}
        parts-list (-> tar (string/replace ".tar" "") (string/split #"_"))
        mission  (get mission-map (nth parts-list 0)) 
        year (-> (nth parts-list 3) (subs 0 4))
        location (nth parts-list 1)
        hhh  (-> (nth parts-list 2) (subs 0 3))
        vvv  (-> (nth parts-list 2) (subs 3 6))]
      (str mission "/ARD_Tile/" year "/" location "/" hhh "/" vvv)))

(defn date-acquired
  "Return the date acquired for the given layer name"
  [tif]
  (-> tif
      (string/split #"_")
      (nth 3)))

(defn year-acquired
  "Return the year acquired for the given layer namec"
  [tif]
  (-> tif
      (date-acquired)
      (subs 0 4)))
