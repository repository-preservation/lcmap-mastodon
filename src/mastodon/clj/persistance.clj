(ns mastodon.clj.persistance
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.string :as string]
            [environ.core :as environ]
            [mastodon.clj.config :refer [config]]
            [mastodon.cljc.data :as data]
            [mastodon.cljc.util :as util]
            [clojure.tools.logging :as log]))

(defn ingest 
  "Post ingest requests to IWDS resources"
  [data iwds_resource]
  (try
    (let [file_name (last (string/split data #"/"))
          iwds_path (str iwds_resource "/inventory")
          post_opts {:body (json/encode {"url" data}) :timeout (:ingest-timeout config)
                     :headers {"Content-Type" "application/json" "Accept" "application/json"}}
          post_resp (http/post iwds_path post_opts)
          response {file_name (:status @post_resp)}]
      (log/infof "ingest attempt: %s" response)
      response)
    (catch Exception ex 
      (let [msg (format "caught exception during persist/ingest. data: %s  iwds: %s  exception: %s"
                        data iwds_resource (util/exception-cause-trace ex "mastodon"))]
        (log/errorf msg)
        {data 500 :error msg}))))

(defn ard-resource-path
  "Return formatted path for ARD resource"
  [name ing_resource]
  (let [tar     (data/ard-tar-name name)
        tarpath (data/tar-path tar)]
    (format "%s/%s/%s/%s" ing_resource tarpath tar name)))

(defn aux-resource-path
  "Return formatted path for Auxiliary data resource"
  [name ing_resource]
  (let [tar (data/aux-tar-name name)]
    (format "%s/%s/%s" ing_resource tar name)))

(defn status-check
  "Return hash-map of ingest resource and IWDS ingest query response"
  [data iwds_host data_resource]
  (let [options   {:timeout (:inventory-timeout config)}
        iwdsresp  (http/get  (str iwds_host "/inventory?only=source&source=" data) options)
        body      (:body @iwdsresp)
        path_func (if (re-find #"AUX_" data) aux-resource-path ard-resource-path)]
    (hash-map (path_func data data_resource) body)))

