(ns mastodon.clj.validation
  (:require [org.httpkit.client :as http]
            [clojure.core :as core]
            [clojure.tools.logging :as log]))

(defn http?
  "Return whether an http resource is accessible."
  [resource name]
  (try
    (let [response (http/get resource)
          status   (:status @response)]
      (if (< status 300) 
        true 
        (do (log/errorf "%s returned non-200 status: %s" resource status)
            false)))
    (catch Exception ex
      (log/errorf "%s is unaccessible %s" name resource)
      false)))

(defn present?
  "Return whether val is not nil."
  [val name]
  (let [resp (not (nil? val))]
    (when (not resp)
      (log/errorf "%s is not defined" name))
    resp))

(defn match? 
  "Return whether val matches pattern."
  [pattern val name]
  (try
    (let [resp (not (nil? (re-matches pattern val)))]
      (when (= false resp)
        (log/errorf "%s does not appear valid" name))
      resp)
    (catch Exception ex
      (log/errorf "exception in validation/match? params %s %s %s" pattern val name)
      false)))

(defn int?
  "Return whether val is an int."
  [val name]
  (let [resp (core/int? val)]
    (when (not resp)
      (log/errorf "%s is not an int" name))
    resp))

(defn validate-cli
  [tileid config]
  (= #{true} 
     (set [(match? #"[0-9]{6}" tileid          "Tile ID")
           (int?   (:partition_level config)   "PARTITION_LEVEL")
           (http? (str (:data_host config)     "/status/000000") "DATA_HOST /status")
           (http? (str (:chipmunk_host config) "/sources?tile=000000") "CHIPMUNK /sources")
           (http? (str (:chipmunk_host config) "/inventory?url=http://fauxhost.gov/foo.tar/bar.tif") "CHIPMUNK /inventory")])))

(defn validate-server
  [config]
  (= #{true} 
     (set [(int?     (:partition_level config) "PARTITION_LEVEL")
           (present? (:data_path config)       "DATA_PATH")
           (present? (:data_host config)       "DATA_HOST")
           (http? (str (:chipmunk_host config) "/sources?tile=005999") "CHIPMUNK /sources")
           (http? (str (:chipmunk_host config) "/inventory?url=http://fauxhost.gov/foo.tar/bar.tif") "CHIPMUNK /inventory")])))


