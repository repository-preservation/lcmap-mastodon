(ns lcmap.mastodon.http
   "HTTP Request functions"
   (:require-macros [cljs.core.async.macros :refer [go]])
   (:require [cljs-http.client :as http-client]
             [cljs.core.async :refer [<!]]))

(defn log 
  "Function for logging messages to the JS console.
   ^String :msg: The message to log to the console"
  [msg]
  (.log js/console msg))

(defn get-request
  "Wrapper func for async HTTP GET requests
   ^String   :url:  URL to request

   Returns a Core.Async Channel"
  [url & [resp]]
  (if (nil? resp)
     (do (go (let [response (<! (http-client/get url {:with-credentials? false}))]
                  (:body response)))) 
     (do resp)))

(defn post-request
  "Wrapper func for async HTTP POST requests
   ^String   :url: URL to post to
   ^Hash Map :data: Data to be posted
   
   Returns a Core.Async Channel"
  [url data & [resp]]
  (if (nil? resp)
    (do (go (let [headers  {"Content-Type" "application/json" "Accept" "application/json"}
                  params   {:with-credentials? false :json-params data :headers headers}
                  response (<! (http-client/post url params))]
             response)))
    (do resp)))

