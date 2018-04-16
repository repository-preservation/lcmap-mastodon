(ns mastodon.cljc.util
  (:require [clojure.string :as string]
  #? (:cljs [cljs.reader :refer [read-string]])))

(defn get-map-val
  "Return particular value for a map, for the conditional key and value."
  [map-obj desired-key & [conditional-key conditional-val]]
  (let [ck (or conditional-key :nil)]
    (if (= conditional-val (ck map-obj))
      (desired-key map-obj))))

(defn collect-map-values
  "Return a collection of values for specified key, if another specified key/value
   pair exists in the map."
  [map-list desired-key & [conditional-key conditional-value]]
  (map #(get-map-val % desired-key conditional-key conditional-value) map-list))

(defn key-for-value
  "Return the key from provided map whose value object includes the provided value."
  [in-map in-value]
  (let [matching-key-list
    (map (fn [kv] (when (string/includes? (val kv) in-value) (key kv)))in-map)]
    ;; is there a better way? there should only be one match
    (first (remove nil? matching-key-list))))

(defn with-suffix 
  "Return items from collection with suffix."
  [lst sfx]
  (filter #(string/ends-with? % sfx) lst))

(defn trailing-slash
  "Ensure input string has trailing slash."
  [input]
  (if (string/ends-with? input "/")
    input
    (str input "/")))

(defn hv-map
  "Given a tile-id as a string, return hash-map with keys :h & :v"
  [id & [regx]]
  (let [match (re-seq (or regx #"[0-9]{3}") id)]
    (hash-map :h (first match)
              :v (last match))))

(defn inventory-url-format
  "Return formatted url as a string for requesting source list from ARD server"
  ([host tile-id]
   (let [hvm (hv-map tile-id)
         host-fmt (trailing-slash host)]
    (str host-fmt "inventory/" (:h hvm) (:v hvm))))
  ([host tile-id from to]
   (let [hvm (hv-map tile-id)
         host-fmt (trailing-slash host)]
     (str host-fmt "inventory/" (:h hvm) (:v hvm) "?from=" from "&to=" to))))

(defn iwds-url-format
  "Return formatted url as a string for requesting source list from IWDS"
  [host tile-id]
  (let [host-fmt (trailing-slash host)]
    (str host-fmt "inventory?only=source&tile=" tile-id)))

(defn string-to-list 
  "Convert a list represented as a string into a list"
  [instring]
  (if (nil? instring)
    []
    (do (-> instring (string/replace "[" "") 
                     (string/replace "]" "") 
                     (string/replace "\"" "") 
                     (string/split #",")))))

(defn get-aux-name
  [aux-response tileid]
  (let [tar-match (re-find (re-pattern (format "AUX_.*_%s_.*.tar" tileid)) aux-response)]
    (subs tar-match 0 39)))

(defn tif-only
  "Return the layer name from a complete URL path"
  [ardpath]
  (-> ardpath
      (string/split #"/")
      (last)))

(defn try-string
  [input]
  #? (:clj (try
             (read-string input)
             (catch Exception ex
               nil))
      :cljs (try
              (read-string input)
              (catch :default e
                nil))))

