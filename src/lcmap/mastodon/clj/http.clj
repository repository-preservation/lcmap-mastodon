(ns lcmap.mastodon.clj.http)

(defn log [msg] (println (str "logging " msg)))

(defn get-request [url] (println (str "GETing url " url)))

(defn post-request [url data] (println (str "POSTing url " url)))
 
