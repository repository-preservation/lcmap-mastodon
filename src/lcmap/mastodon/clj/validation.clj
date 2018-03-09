(ns lcmap.mastodon.clj.validation)

(defn not-nil? [val name]
  (let [resp (not (nil? val))]
    (when (not resp)
      (println name " is not defined"))
    resp))

(defn does-match? [pattern val name]
  (let [resp (not (nil? (re-matches pattern val)))]
    (when (= false resp)
      (println name " does not appear valid"))
    resp))

(defn is-int? [val name]
  (let [resp (int? val)]
    (when (not resp)
      (println name " does not appear to be an int"))
    resp))

(defn validate-cli
  [tileid iwds_host ard_host par_level]
  (= #{true} 
     (set [(does-match? #"[0-9]{6}" tileid "Tile ID")
           (not-nil? iwds_host "IWDS_HOST")
           (not-nil? ard_host "ARD_HOST")
           (is-int? par_level "PARTITION_LEVEL")])))

(defn validate-server
  [iwds_host ard_host par_level ard_path]
  (= #{true} 
     (set [(not-nil? iwds_host "IWDS_HOST")
           (not-nil? ard_host "ARD_HOST")
           (is-int? par_level "PARTITION_LEVEL")
           (not-nil? ard_path "ARD_PATH")])))

(defn validate-map
  [env-map]
  (let [nil-check (map (fn [[key val]] (not-nil? val key)) env-map)]
    (= (set nil-check) #{true})))
