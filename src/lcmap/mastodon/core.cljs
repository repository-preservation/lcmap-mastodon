(ns lcmap.mastodon.core
  (:require [clojure.data :as data]
            [clojure.string :as string]
            [lcmap.mastodon.http :as http]))

(enable-console-print!)
(println "Hello from LCMAP Mastodon!")

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(defn hv-map
  "Helper function.
   Return map for :h and :v given
   a tileid of hhhvvv e.g 052013

   ^String :id: 6 character string representing tile id
   ^Expression :regx: Optional expression to parse tile id

   Returns map with keys :h & :v"
  [id & [regx]]
  (hash-map :h (first (re-seq (or regx #"[0-9]{3}") id)) 
            :v (last  (re-seq (or regx #"[0-9]{3}") id)))
)

(defn tile-id-rest
  "Helper function.
   Takes a 6 digit tileid, and returns a
   string splitting the H and V values so
   they may be used in generating a URL
   for an inventory request.
   ^String :tile-id: 6 character string representing a tileid

   Returns a string with a forward slash separating H and V values
   e.g. /hhh/vvv/
  "
  [tile-id]
  (string/join "/" [(:h (hv-map tile-id)) (:v (hv-map tile-id)) ""])
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
  [desired-key conditional-key conditional-val map-obj]
  (if (= conditional-val (conditional-key map-obj))
    (desired-key map-obj)
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
  [map-list desired-key conditional-key conditional-value]
  
  (def rcount (count map-list))
  (map get-map-val
       (repeat rcount desired-key) 
       (repeat rcount conditional-key) 
       (repeat rcount conditional-value) 
       map-list)
)

(defn ard-inventory
  "Reporting function, provides list of source files available
   from the ARD host for a give Tile

   ^String   :host:    ARD Host
   ^String   :tile-id: Tile ID
   ^String   :region:  Region (conus, etc)
   ^Function :req-fn:  Function used for making request to ARD Host

   Returns list of ARD source files for an individual tile
  "
  [host tile-id region req-fn]
  (def url (string/join "/" [host (tile-id-rest tile-id)]))
  (collect-map-values (req-fn url) :name :type "file")
)

(defn idw-inventory
  "Reporting function, provides list of source files available
   from the ID, host, and region for a given Tile

   ^String :host: IDW Host
   ^String :hv:   Tile ID

   Returns list of ARD source files for an individual tile
  "
  [host tile-id region req-fn]
  (list "foo.tar.gz" "bar.tar.gz" "baz.tar.gz")
)


(defn inventory-diff
  "Diff function, comparing what source files the ARD source has available, 
   and what sources have been ingested into the data warehouse for a specific tile

   ^String :ardh: ARD Host
   ^String :idwh: IDW Host
   ^String :hv:   Tile ID
   ^String :reg:  Region

   Returns tuple (things only in IDW, things only in ARD)
  "
  [ard-host idw-host tile-id region] 
  (def ard-list (ard-inventory ard-host tile-id region http/get-request))
  (def idw-list (idw-inventory idw-host tile-id region http/get-request))
  (rest (reverse (data/diff ard-list idw-list)))
)

