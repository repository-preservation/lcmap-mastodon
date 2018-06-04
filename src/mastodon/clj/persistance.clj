(ns mastodon.clj.persistance
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.string :as string]
            [mastodon.cljc.data :as data]
            [mastodon.clj.config :refer [config]]
            [mastodon.cljc.util :as util]
            [clojure.tools.logging :as log]))

;; (defn ingest 
;;   "Post ingest requests to Chipmunk service"
;;   [data chipmunk_resource]
;;   (try
;;     (let [file_name (last (string/split data #"/"))
;;           chip_path (str chipmunk_resource "/inventory")
;;           post_opts {:body (json/encode {"url" data}) :timeout 120000
;;                      :headers {"Content-Type" "application/json" "Accept" "application/json"}}
;;           post_resp (http/post chip_path post_opts)
;;           response {file_name (:status @post_resp)}]
;;       (log/infof "ingest attempt: %s" response)
;;       response)
;;     (catch Exception ex 
;;       (let [msg (format "caught exception during persist/ingest. data: %s  chipmunk: %s  exception: %s"
;;                         data chipmunk_resource (util/exception-cause-trace ex "mastodon"))]
;;         (log/errorf msg)
;;         {data 500 :error msg}))))

(defmulti resource-path
  (fn [x] (keyword (:server_type config))))

(defmethod resource-path :default [x]
  nil)

(defmethod resource-path :ard [tif]
  (let [ing_resource (str (:ard_host config) "/ard")
        tar     (data/ard-tar-name tif)
        tarpath (data/tar-path tar)]
    (format "%s/%s/%s/%s" ing_resource tarpath tar tif)))

(defmethod resource-path :aux [tif]
  (let [ing_resource (:aux_host config)
        tar (data/aux-tar-name name)]
    (format "%s/%s/%s" ing_resource tar tif)))

(defn ingest 
  "Post ingest requests to Chipmunk service"
  [tif chipmunk_resource]
  (try
    (let [tif_url (resource-path tif)
          chip_path (str chipmunk_resource "/inventory")
          post_opts {:body (json/encode {"url" tif_url}) :timeout 120000
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


;; (defn ard-resource-path
;;   "Return formatted path for ARD resource"
;;   [name ing_resource]
;;   (let [tar     (data/ard-tar-name name)
;;         tarpath (data/tar-path tar)]
;;     (format "%s/%s/%s/%s" ing_resource tarpath tar name)))

;; (defn aux-resource-path
;;   "Return formatted path for Auxiliary data resource"
;;   [name ing_resource]
;;   (let [tar (data/aux-tar-name name)]
;;     (format "%s/%s/%s" ing_resource tar name)))

;; (defn status-check
;;   "Return hash-map of ingest resource and IWDS ingest query response"
;;   [data chipmunk_host data_resource]
;;   (let [chipresp  (http/get (str chipmunk_host "/inventory?only=source&source=" data))
;;         body      (:body @chipresp)
;;         path_func (if (re-find #"AUX_" data) aux-resource-path ard-resource-path)]
;;     (hash-map (path_func data data_resource) body)))

