(ns lcmap.mastodon.http
   "HTTP Request functions"
   (:require-macros [cljs.core.async.macros :refer [go]])
   (:require [cljs-http.client :as http-client]
             [cljs.core.async :refer [<!]]
             [lcmap.mastodon.data :as testdata]))

(enable-console-print!)

(defn log [msg]
  (.log js/console msg))

(defn get-request
 "Wrapper func for async HTTP GET requests

  ^String   :url:  URL to request
  ^Hash Map :resp: Optional desired response, used for testing
 "
 [url & [resp]]
 (if (nil? resp)
    (do (go (let [response (<! (http-client/get url {:with-credentials? false}))]
               (:body response)))) 
    (do resp)) 
)

(defn post-request
  "Wrapper func for async HTTP POST requests

   ^String   :url: URL to post to
   ^Hash Map :data: Data to be posted
   ^Hash Map :resp: Optional desired response, used for testing"
  [url data & [resp]]
  (if (nil? resp)
    (do 
      (go
        (let [response (<! (http-client/post url {:with-credentials? false
                                                  :json-params data
                                                  :headers {"Content-Type" "application/json"
                                                            "Accept" "application/json"}}))]
        response)))

    (do resp))
)

(defn mock-ard []
  #(get-request % (testdata/ard-resp)))

(defn mock-idw []
  #(get-request % (testdata/idw-resp)))


