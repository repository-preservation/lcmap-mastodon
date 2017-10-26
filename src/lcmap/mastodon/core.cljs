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

(defn hvmap
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

(defn hvrest
  "Helper function.
   Takes a 6 digit tileid, and returns a
   string splitting the H and V values so
   they may be used in generating a URL
   for an inventory request.
   ^String :hvid: 6 character string representing a tileid

   Returns a string with a forward slash separating H and V values
   e.g. /hhh/vvv/
  "
  [hvid]
  (string/join "/" [(:h (hvmap hvid)) (:v (hvmap hvid)) ""])
)

(defn get-map-val
  "Helper function.
   Return particular value for a map,
   for the conditional key and value

   ^Keyword :dkey: Desired key
   ^Keyword :ckey: Conditional key
   ^String  :cval: Conditional key val
   ^Map     :mobj: Map object
  "
  [dkey ckey cval mobj]
  (if (= cval (ckey mobj))
    (dkey mobj)
  )
)

(defn ardlist
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
  [inmaps dk ck cv]
  
  (def rcount (count inmaps))
  (map get-map-val
       (repeat rcount dk) 
       (repeat rcount ck) 
       (repeat rcount cv) 
       inmaps)
)

(defn ard-sources
  "Reporting function, provides list of source files available
   from the ARD host for a give Tile

   ^String :host: ARD Host
   ^String :hv:   Tile ID

   Returns list of ARD source files for an individual tile
  "
  [host hv reg get_req]
  (def url (string/join "/" [host (hvrest hv)]))
  (ardlist (get_req url) :name :type "file")
)

(defn idw-sources
  "Reporting function, provides list of source files available
   from the ID, host, and region for a given Tile

   ^String :host: IDW Host
   ^String :hv:   Tile ID

   Returns list of ARD source files for an individual tile
  "
  [host hv reg get_req]
  ;; construct GET request to correct chipmunk instance
  ;; make request
  ;; return hash-map constructed from response
  ;; if success {"sources": '(filea fileb)}
  ;; else {"error": "message"}
  (list "foo.tar.gz" "bar.tar.gz" "baz.tar.gz")
)


(defn hvdiff
  "Diff function, comparing what source files the ARD source has available, 
   and what sources have been ingested into the data warehouse for a specific tile

   ^String :ardh: ARD Host
   ^String :idwh: IDW Host
   ^String :hv:   Tile ID
   ^String :reg:  Region

   Returns tuple (things only in IDW, things only in ARD)
  "
  [ardh idwh hv reg] 
  (def ard-list (ard-sources ardh hv reg http/get-request))
  (def idw-list (idw-sources idwh hv reg http/get-request))
  (rest (reverse (data/diff ard-list idw-list)))
)

