(ns lcmap.mastodon.clj.persistance
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.string :as string]
            [lcmap.mastodon.cljc.ard :as ard]))

(defn log-error
  "Record ingest error in appropriate log files"
  [ard body error tileid]
  (let [ard_log (str "ingest_error_list_" tileid ".log")
        msg_log (str "ingest_error_body_" tileid ".log")]
    (spit ard_log (str ard "\n") :append true)
    (spit msg_log (str ard " - " body " - " error "\n") :append true)))

(defn ingest 
  "Post ingest requests to IWDS resources"
  [ard iwds_resource]
  (try 
    (let [iwds_path (str iwds_resource "/inventory")
          post_opts {:body (json/encode {"url" ard})
                     :timeout 120000
                     :headers {"Content-Type" "application/json" "Accept" "application/json"}}
          ard_resp (http/post iwds_path post_opts)
          tif_name (last (string/split ard #"/"))]
      {tif_name (:status @ard_resp)})
    (catch Exception ex 
      (.printStackTrace ex)
      (str "caught exception in ingest-ard: " (.getMessage ex)))))

(defn status-check
  "Based on ingest status, put ARD into correct Atom"
  [tif iwds_resource ing_resource]
  (let [iwdsresp (http/get (str iwds_resource tif))
        tar      (ard/tar-name tif)
        tarpath  (ard/tar-path tar)]
    (hash-map (str ing_resource "/" tarpath "/" tar "/" tif) (:body @iwdsresp))))


