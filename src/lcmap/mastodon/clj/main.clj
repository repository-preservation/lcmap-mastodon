(ns lcmap.mastodon.clj.main
  (:gen-class)
  (:require 
            [cheshire.core            :as json]
            [clojure.string           :as string]
            [compojure.core           :as compojure]
            [compojure.route          :as route]
            [environ.core             :as environ]
            [lcmap.mastodon.cljc.ard  :as ard]
            [lcmap.mastodon.cljc.util :as util]
            [org.httpkit.client       :as http]
            [org.httpkit.server       :as server]
            [org.satta.glob           :as glob]
            [ring.middleware.json     :as ring-json]))

(def ard-to-ingest-atom (atom []))
(def ingested-ard-atom  (atom []))

; export ARDPATH=/tmp/fauxard/\{tm,etm,oli_tirs\}/ARD_Tile/*/CU/
(def ardpath (:ard-path environ/env))

(defn strip-path 
  "Return the filename, minus the path"
  [filepath]
  (last (string/split filepath #"/")))

(defn jfile-name 
  "Convert java.io.File object into string of file name"
  [jfile]
  (strip-path (str jfile)))

(defn get-filenames
  "Return list of files for a given filesystem path patter"
  [filepath]
  (map jfile-name (glob/glob filepath)))

(defn hv-map
  "Return hash-map for :h and :v given a tileid of hhhvvv e.g 052013"
  [id & [regx]]
  (let [match (re-seq (or regx #"[0-9]{3}") id)]
    (hash-map :h (first match)
              :v (last match))))

(defn ard-lookup 
  "Return list of ARD for a give tileid"
  [tileid]
  (let [hvmap (hv-map tileid)
        fpath (str ardpath (:h hvmap) "/" (:v hvmap) "/*")]
    {:status 200 :body (get-filenames fpath)}))

(defn string-to-list 
  "Convert a list represented as a string into a list"
  [instring]
  (if (nil? instring)
    []
    (do (-> instring (string/replace "[" "") 
                     (string/replace "]" "") 
                     (string/replace "\"" "") 
                     (string/split #",")))))

(defn ard_status_check
  "Based on ingest status, put ARD into correct Atom"
  [tif iwds_resource ing_resource]
  (let [iwdsresp (http/get (str iwds_resource tif))
        tar      (ard/tar-name tif)
        tarpath  (ard/tar-path tar)]
    (if (= (:body @iwdsresp) "[]")
      (swap! ard-to-ingest-atom conj (str ing_resource "/" tarpath "/" tar "/" tif))
      (swap! ingested-ard-atom conj tif)))
    (= 1 1))

(defn ingest_error
  "Record ingest error in appropriate log files"
  [ard body error tileid]
  (let [ard_log (str "ingest_error_list_" tileid ".log")
        msg_log (str "ingest_error_body_" tileid ".log")]
    (spit ard_log (str ard "\n") :append true)
    (spit msg_log (str ard " - " body " - " error "\n") :append true)))

(defn ingest-ard 
  "Post ingest requests to IWDS resources"
  [ard_list iwds_resource tileid]
  (try 
    (doseq [ard ard_list]
      (let [iwds_path (str iwds_resource "/inventory")
            post_opts {:body (json/encode {"url" ard})
                       :timeout 120000
                       :headers {"Content-Type" "application/json" "Accept" "application/json"}}
            ard_resp (http/post iwds_path post_opts)
            tif_name (last (string/split ard #"/"))]
            (println (str "layer: " tif_name " " (:status @ard_resp)))
            (if (nil? (:status @ard_resp))
              (do (println (str "Status nil for " tif_name)))
              (do (when (> (:status @ard_resp) 299) 
                   (ingest_error ard (:body @ard_resp) (:error @ard_resp) tileid))))))
      (= 1 1)
    (catch Exception ex 
      (.printStackTrace ex)
      (str "caught exception in ingest-ard: " (.getMessage ex)))))

(defn get-base [request]
{:status 200 :body ["Would you like some ARD with that?"]})

;; ## Routes
(compojure/defroutes routes
  (compojure/context "/" request
    (route/resources "/")
    (compojure/GET "/" [] (get-base request))
    (compojure/GET "/inventory/:tileid{[0-9]{6}}" [tileid] (ard-lookup tileid))
))

(def app (-> routes 
             (ring-json/wrap-json-body {:keywords? true})
             (ring-json/wrap-json-response)))

(declare http-server)

(defn -main [& args]
  (server/run-server #'app {:port 9876}))

