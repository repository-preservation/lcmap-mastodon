(ns mastodon.cljs.http
   "HTTP Request functions"
   (:require-macros [cljs.core.async.macros :refer [go]])
   (:require [cljs-http.client :as http-client]
             [cljs.core.async :refer [<!]]))

(defn get-request
  "Wrapper func for async HTTP GET requests."
  [url & [resp]]
  (if (nil? resp)
     (do (go (let [response (<! (http-client/get url {:with-credentials? false}))]
                  response))) 
     (do resp)))

(defn post-request
  "Wrapper func for async HTTP POST requests."
  [url data & [resp]]
  (if (nil? resp)
    (do (go (let [headers  {"Content-Type" "application/json" "Accept" "application/json"}
                  params   {:with-credentials? false :json-params data :headers headers}
                  response (<! (http-client/post url params))]
             response)))
    (do resp)))

