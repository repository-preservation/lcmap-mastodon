(ns mastodon.clj.validation
  (:require [org.httpkit.client :as http]))

(defn http-accessible?
  "Return whether an http resource is accessible."
  [resource]
  (let [response (http/get resource)
        status   (:status @response)]
    (try
      (if (< status 300) 
        true 
        (do (println resource " returned non-200 status: " status)
            false))
    (catch NullPointerException ex
      (println resource " is unaccessible")
      false))))

(defn not-nil? 
  "Return whether val is not nil."
  [val name]
  (let [resp (not (nil? val))]
    (when (not resp)
      (println name " is not defined"))
    resp))

(defn does-match? 
  "Return whether val matches pattern."
  [pattern val name]
  (let [resp (not (nil? (re-matches pattern val)))]
    (when (= false resp)
      (println name " does not appear valid"))
    resp))

(defn is-int?
  "Return whether val is an int."
  [val name]
  (let [resp (int? val)]
    (when (not resp)
      (println name " does not appear to be an int"))
    resp))

(defn validate-cli
  "Wrapper func for CLI parameters."
  [tileid iwds_host ard_host par_level]
  (= #{true} 
     (set [(does-match? #"[0-9]{6}" tileid "Tile ID")
           (not-nil? iwds_host "IWDS_HOST")
           (not-nil? ard_host "ARD_HOST")
           (is-int? par_level "PARTITION_LEVEL")
           (http-accessible? iwds_host)
           (http-accessible? ard_host)])))

(defn validate-server
  "Wrapper func for server parameters."
  [iwds_host ard_host par_level ard_path]
  (= #{true} 
     (set [(not-nil? iwds_host "IWDS_HOST")
           (not-nil? ard_host "ARD_HOST")
           (is-int? par_level "PARTITION_LEVEL")
           (not-nil? ard_path "ARD_PATH")
           (http-accessible? iwds_host)
           (http-accessible? ard_host)])))

