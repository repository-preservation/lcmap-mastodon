(ns lcmap.mastodon.core
  (:require [clojure.data :as data]
            [lcmap.mastodon.http :as http]))

(enable-console-print!)
(println "Hello from LCMAP Mastodon!")

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)


(defn ard-sources
  "Reporting function, provides list of source files available
   from the ARD host for a give Tile

   ^String :host: ARD Host
   ^String :hv:   Tile ID

   Returns list of ARD source files for an individual tile
  "
  [host hv reg get_req]
  ;; construct GET request for ARD server
  ;; make request
  ;; return hash map of response
  ;; if success {"sources": '(filea fileb)}
  ;; else {"error": "message"}
  (list "ho.tar.gz" "hum.tar.gz" "foo.tar.gz" "bar.tar.gz")
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

