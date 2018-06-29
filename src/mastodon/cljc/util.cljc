(ns mastodon.cljc.util
  (:require [clojure.string :as string]
  #? (:cljs [cljs.reader :refer [read-string]])))

(defn get-map-val
  "Return particular value for a map, for the conditional key and value."
  [map-obj desired-key & [conditional-key conditional-val]]
  (let [ck (or conditional-key :nil)]
    (if (= conditional-val (ck map-obj))
      (desired-key map-obj))))

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
   (str (inventory-url-format host tile-id) "?from=" from "&to=" to)))

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

(defn exception-cause-trace
  "Returns the exceptions cause and stack trace.
   Supply a keyword to filter the trace optionally"
  ([exception]
   (let [ex-map (Throwable->map exception)]
     (select-keys ex-map [:cause :trace])))
  ([exception trace-filter]
   (let [exc (exception-cause-trace exception)
         filter_fn (fn [i] (-> i (#(string/join #" " %)) (string/includes? (str trace-filter))))
         filtered_trace (filter filter_fn (:trace exc))]
     (assoc exc :trace filtered_trace))))
