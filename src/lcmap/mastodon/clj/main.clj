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
            [lcmap.mastodon.clj.file  :as file]
            [org.httpkit.client       :as http]
            [org.httpkit.server       :as server]
            [ring.middleware.json     :as ring-json]
            [lcmap.mastodon.cljc.util :as util]))

(def ard-to-ingest-atom (atom []))
(def ingested-ard-atom  (atom []))

; export ARDPATH=/tmp/fauxard/\{tm,etm,oli_tirs\}/ARD_Tile/*/CU/
(def ardpath (:ard-path environ/env))

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
  [ard iwds_resource]
  (try 
    (let [iwds_path (str iwds_resource "/inventory")
          post_opts {:body (json/encode {"url" ard})
                     :timeout 120000
                     :headers {"Content-Type" "application/json" "Accept" "application/json"}}
          ard_resp (http/post iwds_path post_opts)
          tif_name (last (string/split ard #"/"))]
          {tif_name (:status @ard_resp)})
    (catch Exception ex 
      (.printStackTrace ex)
      (str "caught exception in ingest-ard: " (.getMessage ex)))))

(defn post-bulk-ingest
  "Generate ingest requests for list of posted ARD"
  [{:keys [:body] :as req}]
  (let [tifs (:urls body)
        tif_list (string/split tifs #",")
        iwds (:iwds-host environ/env)
        ingest_results (pmap #(ingest-ard % iwds) tif_list)]
    
    ;; realize results
    (count ingest_results)
    {:status 200 :body ingest_results}))

(defn ard-lookup 
  "Return list of ARD for a give tileid"
  [tileid]
  (let [hvmap (util/hv-map tileid)
        fpath (str ardpath (:h hvmap) "/" (:v hvmap) "/*")]
    {:status 200 :body (file/get-filenames fpath)}))

(defn get-base [request]
{:status 200 :body ["Would you like some ARD with that?"]})

;; ## Routes
(compojure/defroutes routes
  (compojure/context "/" request
    (route/resources "/")
    (compojure/GET "/" [] (get-base request))
    (compojure/GET "/inventory/:tileid{[0-9]{6}}" [tileid] (ard-lookup tileid))
    (compojure/POST "/bulk-ingest" [] (post-bulk-ingest request))))

(def app (-> routes
             (ring-json/wrap-json-body {:keywords? true})
             (ring-json/wrap-json-response)))

(declare http-server)

(defn -main [& args]
  (let [tileid          (first args)
        autoingest      (last  args)
        iwds_host       (:iwds-host   environ/env)
        ard_host        (:ard-host    environ/env)
        ingest_host     (:ingest-host environ/env)
        partition_level (read-string (:partition-level environ/env))]

    (if (nil? tileid)
      (server/run-server #'app {:port 9876}) ;; no args passed, run ring app
      (do
        (when (nil? (re-matches #"[0-9]{6}" tileid))
          (println "Invalid Tile Id: " tileid)
          (System/exit 0))

        (when (nil? iwds_host)
          (println "IWDS_HOST must be defined in your environment, exiting")
          (System/exit 0))

        (when (nil? ard_host)
          (println "ARD_HOST must be defined in your environment, exiting")
          (System/exit 0))

        (when (not (int? partition_level))
          (println "PARTITION_LEVEL must be an integer defined in your environment, exiting ")
          (System/exit 0))

        (let [iwds_resource (str iwds_host "/inventory?only=source&source=")
              ard_resource  (util/ard-url-format ard_host tileid)
              ing_resource  (str ard_host "/ard")
              {:keys [status headers body error] :as resp} @(http/get ard_resource)
              ard_vector    (-> body (util/string-to-list) (util/with-suffix "tar") (ard/expand-tars))
              ard_results   (pmap #(ard_status_check % iwds_resource ing_resource) ard_vector)]

          ; realize the pmap results
          (count ard_results)

          (println "Tile Status Report for: " tileid)
          (println "To be ingested: "   (count @ard-to-ingest-atom))
          (println "Already ingested: " (count @ingested-ard-atom))
          (println "")

          (let [ard_partition (partition partition_level partition_level "" @ard-to-ingest-atom)
                ingest_map #(ingest-ard % iwds_host)]
            
            (if (= autoingest "-y")
              (do 
                (doseq [a ard_partition]
                  (count (pmap ingest_map a)))
                (println "Ingest Complete"))
              (do 
                (println "Ingest? (y/n)")
                (if (= (read-line) "y")
                  (do
                    (doseq [a ard_partition]
                      (count (pmap ingest_map a)))
                    (println "Ingest Complete"))
                  (do 
                    (println "Exiting!")
                    (System/exit 0)))))))
        (System/exit 0)))))

