(ns mastodon.clj.validation
  (:require [org.httpkit.client :as http]
            [clojure.tools.logging :as log]))

(defn http-accessible?
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

(defn not-nil? 
  "Return whether val is not nil."
  [val name]
  (let [resp (not (nil? val))]
    (when (not resp)
      (log/errorf "%s is not defined" name))
    resp))

(defn does-match? 
  "Return whether val matches pattern."
  [pattern val name]
  (let [resp (not (nil? (re-matches pattern val)))]
    (when (= false resp)
      (log/errorf "%s does not appear valid" name))
    resp))

(defn is-int?
  "Return whether val is an int."
  [val name]
  (let [resp (int? val)]
    (when (not resp)
      (log/errorf "%s is not an int" name))
    resp))

(defn validate-cli
  "Wrapper func for CLI parameters."
  [tileid iwds_host ard_host par_level]
  (= #{true} 
     (set [(does-match? #"[0-9]{6}" tileid "Tile ID")
           (not-nil? iwds_host "IWDS_HOST")
           (not-nil? ard_host "ARD_HOST")
           (is-int? par_level "PARTITION_LEVEL")
           (http-accessible? iwds_host "IWDS_HOST")
           (http-accessible? ard_host "ARD_HOST")])))

(defn validate-ard-server
  [iwds_host ard_host par_level ard_path]
  (= #{true} 
     (set [(not-nil? iwds_host "IWDS_HOST")
           (not-nil? ard_host "ARD_HOST")
           (is-int? par_level "PARTITION_LEVEL")
           (not-nil? ard_path "ARD_PATH")
           (http-accessible? iwds_host "IWDS_HOST")])))

(defn validate-aux
  [iwds_host ard_host aux_host]
  (= #{true} 
     (set [(not-nil? iwds_host "IWDS_HOST")
           (not-nil? ard_host "ARD_HOST")
           (not-nil? aux_host "AUX_HOST")
           (http-accessible? aux_host "AUX_HOST")
           (http-accessible? iwds_host "IWDS_HOST")])))

(defn validate-server
  "Wrapper func for server parameters."
  [type iwds_host ard_host aux_host par_level ard_path]
  (cond
   (= type "ard") (do (validate-ard-server iwds_host ard_host par_level ard_path) (log/errorf "type is ard")) 
   (= type "aux") (do (validate-aux iwds_host ard_host aux_host) (log/errorf "type is aux")) 
   :else false))

