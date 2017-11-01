(ns lcmap.mastodon.http
   "HTTP Request functions"
   (:require-macros [cljs.core.async.macros :refer [go]])
   (:require [cljs-http.client :as http-client]
             [cljs.core.async :refer [<!]]))

(enable-console-print!)

(defn get-request
 "Wrapper func for async HTTP GET requests

  ^String   :url:  URL to request
  ^Hash Map :resp: Optional desired response, used for testing
 "
 [url & [resp]]
 (if (nil? resp)
     (do   
       (go (def response (<! (http-client/get url {:with-credentials? false}))))
       (:body response))
     (do resp)) 
)


