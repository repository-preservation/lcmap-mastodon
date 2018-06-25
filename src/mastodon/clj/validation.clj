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
      (log/errorf "%s is unaccessible" name)
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
  (let [resp (not (nil? (re-matches pattern val)))]
    (when (= false resp)
      (log/errorf "%s does not appear valid" name))
    resp))

(defn int?
  "Return whether val is an int."
  [val name]
  (let [resp (core/int? val)]
    (when (not resp)
      (log/errorf "%s is not an int" name))
    resp))

(defn validate-cli
  "Wrapper func for CLI parameters."
  [tileid config]
  (= #{true} 
     (set [(match?   #"[0-9]{6}" tileid        "Tile ID")
           (present? (:chipmunk_host config)   "CHIPMUNK_HOST")
           (present? (:ard_host config)        "ARD_HOST")
           (int?     (:partition_level config) "PARTITION_LEVEL")
           (http?    (:chipmunk_host config)   "CHIPMUNK_HOST")
           (http?    (:ard_host config)        "ARD_HOST")])))

(defmulti validate-server
  (fn [config] (keyword (:server_type config))))

(defmethod validate-server :default [x] 
  (log/errorf "invalid SERVER_TYPE")
  false)

(defmethod validate-server :ard
  [config]
  (= #{true} 
     (set [(present? (:chipmunk_host config)   "CHIPMUNK_HOST")
           (present? (:ard_host config)        "ARD_HOST")
           (int?     (:partition_level config) "PARTITION_LEVEL")
           (present? (:ard_path config)        "ARD_PATH")
           (http?    (:chipmunk_host config)   "CHIPMUNK_HOST")
           (http?    (:nemo_host config)       "NEMO_HOST")])))

(defmethod validate-server :aux
  [config]
  (= #{true} 
     (set [(present? (:chipmunk_host config) "CHIPMUNK_HOST")
           (present? (:ard_host config)      "ARD_HOST")
           (present? (:aux_host config)      "AUX_HOST")
           (http?    (:aux_host config)      "AUX_HOST")
           (http?    (:chipmunk_host config) "CHIPMUNK_HOST")])))


