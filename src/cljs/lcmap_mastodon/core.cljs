(ns lcmap-mastodon.core
  (:require [clojure.data :as data])
)

(defn ard-sources
  "Reporting function, provides list of source files available
   from the ARD host for a give Tile

   ^String :host: ARD Host
   ^String :hv:   Tile ID

   Returns list of ARD source files for an individual tile
  "
  [host hv]
  (list "ho.tar.gz" "hum.tar.gz" "foo.tar.gz" "bar.tar.gz")
)

(defn idw-sources
  "Reporting function, provides list of source files available
   from the IDW host for a give Tile

   ^String :host: IDW Host
   ^String :hv:   Tile ID

   Returns list of ARD source files for an individual tile
  "
  [host hv]
  (list "foo.tar.gz" "bar.tar.gz" "baz.tar.gz")
)

(defn hvdiff
  "Diff function, comparing what source files the ARD source has available, 
   and what sources have been ingested into the data warehouse for a specific tile

   ^String :ardh: ARD Host
   ^String :idwh: IDW Host
   ^String :hv:   Tile ID

   Returns tuple (things only in IDW, things only in ARD)
  "
  [ardh idwh hv] 
  (def ard-list (ard-sources ardh hv))
  (def idw-list (idw-sources idwh hv))
  (rest (reverse (data/diff ard-list idw-list)))
)

