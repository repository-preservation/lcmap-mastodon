(ns mastodon.cljc.util
  (:require [clojure.string :as string]))

(defn log [msg]
  #? (:clj (println msg)
      :cljs (.log js/console msg))
)

(defn get-map-val
  "Helper function.
   Return particular value for a map,
   for the conditional key and value

   ^Keyword :desired-key:     Desired key
   ^Keyword :conditional-key: Conditional key
   ^String  :conditional-val: Conditional key val
   ^Map     :map-obj: Map object
  "
  [map-obj desired-key & [conditional-key conditional-val]]
  (let [ck (or conditional-key :nil)]
    (if (= conditional-val (ck map-obj))
      (desired-key map-obj)
    )
  )
)

(defn collect-map-values
  "Data organization function.
   Takes a list of maps and returns
   a collection of values for specified
   key, if another specified key/value
   pair exists in the map
   
   ^List :inmaps: List of maps representing dir contents as json
   ^Keyword :dk: Desired key to collect from ard-response
   ^Keyword :ck: Conditional key to check value of
   ^String  :cv: Conditional value to check value of

   Returns list of dk values from ard-response"
  [map-list desired-key & [conditional-key conditional-value]]
  (map #(get-map-val % desired-key conditional-key conditional-value) map-list)
)

(defn key-for-value
  "Convenience Function
   Return the key from provided map whose value object
   includes the provided value"
  [in-map in-value]
  (let [matching-key-list
        (map (fn [kv] (when (string/includes? (val kv) in-value) (key kv)))
             in-map)]
    ;; is there a better way? there should only be one match
    (first (remove nil? matching-key-list)))
)

(defn fresh-includes [coll i]
  (if (contains? (set coll) i)
    (do coll)
    (do (conj coll i)
    )
  )
)

(defn with-suffix [lst sfx]
  (filter #(string/ends-with? % sfx) lst))

(defn trailing-slash
  [input]

  (if (string/ends-with? input "/")
    input
    (str input "/")))

(defn hv-map
  "Helper function.
   Return map for :h and :v given
   a tileid of hhhvvv e.g 052013

   ^String :id: 6 character string representing tile id
   ^Expression :regx: Optional expression to parse tile id

   Returns map with keys :h & :v"
  [id & [regx]]
  (let [match (re-seq (or regx #"[0-9]{3}") id)]
    (hash-map :h (first match)
              :v (last match))))

(defn ard-url-format
  "URL generation function for requests to an ARD file access server

   ^String :host:    Host name
   ^String :tile-id: Tile ID

   Returns formatted url as a string for requesting source list from ARD server"
  [host tile-id]
  (let [hvm (hv-map tile-id)
        host-fmt (trailing-slash host)]
    (str host-fmt "inventory/" (:h hvm) (:v hvm))))

(defn iwds-url-format
  "URL generation function for requests to an LCMAP-Chipmunk instance

   ^String :host:    lcmap-chipmunk instance host
   ^String :tile-id: Tile ID

   Returns formatted url as a string for requesting source list from IWDS"
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
