(ns mastodon.clj.persistance
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.string :as string]
            [mastodon.cljc.data :as data]
            [mastodon.cljc.util :as util]
            [clojure.tools.logging :as log]))

(defn ingest 
  "Post ingest requests to IWDS resources"
  [data iwds_resource]
  (log/infof "persist/ingest: data: %s  iwds_resource: %s" data iwds_resource)
  (let [file_name (last (string/split data #"/"))]
    (try 
      (let [iwds_path (str iwds_resource "/inventory")
            post_opts {:body (json/encode {"url" data}) :timeout 120000
                       :headers {"Content-Type" "application/json" "Accept" "application/json"}}
            post_resp (http/post iwds_path post_opts)
            response {file_name (:status @post_resp)}]
        (log/infof "ingest attempt: %s" response)
        response)
      (catch Exception ex 
        (log/errorf "caught exception during ingest. ard: %s  iwds: %s  exception: %" 
                    data iwds_resource (.getMessage ex))
        {file_name 500 :error (.getMessage ex)}))))

(defn status-check
  "Return hash-map of ingest resource and IWDS ingest query response"
  ([data iwds_host]
   (let [iwdsresp (http/get (str iwds_host "/inventory?only=source&source=" data))]
     (hash-map data (:body @iwdsresp))))
  ([data iwds_host ing_resource]
   (let [iwdsresp (http/get (str iwds_host "/inventory?only=source&source=" data))
         tar      (data/tar-name data)
         tarpath  (data/tar-path tar)]
     (hash-map (str ing_resource "/" tarpath "/" tar "/" data) (:body @iwdsresp)))))


