(ns mastodon.clj.persistance
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.string :as string]
            [environ.core :as environ]
            [mastodon.clj.config :refer [config]]
            [mastodon.cljc.data :as data]
            [mastodon.clj.config :refer [config]]
            [mastodon.cljc.util :as util]
            [clojure.tools.logging :as log]))

(defmulti resource-path
  (fn [x] (keyword (:data_type config))))

(defmethod resource-path :default [x]
  nil)

(defmethod resource-path :ard [tif]
  (let [ing_resource (str (:ard_host config) "/ard")
        tar     (data/ard-tar-name tif)
        tarpath (data/tar-path tar)]
    (format "%s/%s/%s/%s" ing_resource tarpath tar tif)))

(defmethod resource-path :aux [tif]
  (let [ing_resource (:aux_host config)
        tar (data/aux-tar-name tif)]
    (format "%s/%s/%s" ing_resource tar tif)))

(defn ingest 
  "Post ingest requests to Chipmunk service"
  [tif chipmunk_resource]
  (try
    (let [tif_url (resource-path tif)
          chip_path (str chipmunk_resource "/inventory")
          post_opts {:body (json/encode {"url" tif_url}) :timeout (:ingest_timeout config)
                     :headers {"Content-Type" "application/json" "Accept" "application/json"}}
          post_resp (http/post chip_path post_opts)
          response {tif (:status @post_resp)}]
      (log/infof "ingest attempt: %s" response)
      response)
    (catch Exception ex 
      (let [msg (format "caught exception during persist/ingest. data: %s  chipmunk: %s  exception: %s"
                        tif chipmunk_resource (util/exception-cause-trace ex "mastodon"))]
        (log/errorf msg)
        {tif 500 :error msg}))))

