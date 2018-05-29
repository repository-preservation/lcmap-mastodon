(ns mastodon.clj.warehouse
  (:require [mastodon.clj.config :refer [config]]
            [cheshire.core :as json]
            [org.httpkit.client :as http]))

(defn ingested-tifs
  "Return names of ingestd tifs"
  [tileid]
  (let [response (http/get (str (:nemo_resource config) tileid))
        rsp_json (json/decode (:body @response))]
    (map #(get % "source") rsp_json)))
