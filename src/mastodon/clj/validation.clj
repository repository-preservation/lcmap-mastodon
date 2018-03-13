(ns mastodon.clj.validation)

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
           (is-int? par_level "PARTITION_LEVEL")])))

(defn validate-server
  "Wrapper func for server parameters."
  [iwds_host ard_host par_level ard_path]
  (= #{true} 
     (set [(not-nil? iwds_host "IWDS_HOST")
           (not-nil? ard_host "ARD_HOST")
           (is-int? par_level "PARTITION_LEVEL")
           (not-nil? ard_path "ARD_PATH")])))

