(ns mastodon.clj.persistance
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.string :as string]
            [mastodon.cljc.ard :as ard]
            [mastodon.cljc.util :as util]
            [clojure.tools.logging :as log]))

(defn ingest 
  "Post ingest requests to IWDS resources"
  [ard iwds_resource]
  (let [tif_name (last (string/split ard #"/"))]
    (try 
      (let [iwds_path (str iwds_resource "/inventory")
            post_opts {:body (json/encode {"url" ard}) :timeout 120000
                       :headers {"Content-Type" "application/json" "Accept" "application/json"}}
            ard_resp (http/post iwds_path post_opts)
            response {tif_name (:status @ard_resp)}]
        (log/infof "ingest attempt: %s" response)
        response)
      (catch Exception ex 
        (log/errorf "caught exception during ingest. ard: %s  iwds: %s  exception: %" 
                    ard iwds_resource (.getMessage ex))
        {tif_name 500 :error (.getMessage ex)}))))

(defn status-check
  "Based on ingest status, put ARD into correct Atom"
  [tif iwds_host ing_resource]
  (let [iwdsresp (http/get (str iwds_host "/inventory?only=source&source=" tif))
        tar      (ard/tar-name tif)
        tarpath  (ard/tar-path tar)]
    (hash-map (str ing_resource "/" tarpath "/" tar "/" tif) (:body @iwdsresp))))


