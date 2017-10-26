(ns lcmap.mastodon.http
   "HTTP Request functions"
   (:require-macros [cljs.core.async.macros :refer [go]])
   (:require [cljs-http.client :as http-client]
             [cljs.core.async :refer [<!]]))

(enable-console-print!)

(defn get-request
 "Wrapper func for async HTTP GET requests

  ^String :url: URL to request
  ^Hash Map :params: Query parameters
 "
 [url]
 (go (def response (<! (http-client/get url {:with-credentials? false}))))
 (:body response)
)


