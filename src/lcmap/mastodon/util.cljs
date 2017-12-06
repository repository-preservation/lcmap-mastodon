(ns lcmap.mastodon.util
  (:require [clojure.string :as string]))

(defn log [msg]
  (.log js/console msg)
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

