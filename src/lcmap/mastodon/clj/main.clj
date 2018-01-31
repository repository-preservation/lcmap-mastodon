(ns lcmap.mastodon.clj.main
  (:gen-class)
  (:require [lcmap.mastodon.cljc.core :as mcore]
            [lcmap.mastodon.cljc.util :as util]
            [lcmap.mastodon.clj.ard :as ard]
            [cheshire.core :as json]
            [environ.core :as environ]
            [org.httpkit.client :as http]
            [clojure.string :as string]))

(def ard-to-ingest-atom (atom []))
(def ingested-ard-atom (atom []))
(def ingested-and-missing-ard-atom (atom []))
(def ard-errored-on-ingest-atom (atom []))

(defn http-body-to-list [inbody]
  (if (nil? inbody)
    []
    (do (-> inbody (string/replace "[" "") 
                   (string/replace "]" "") 
                   (string/replace "\"" "") 
                   (string/split #",")))))

(defn ard_status_check [tif iwds_resource ing_resource]
  (let [iwdsresp (http/get (str iwds_resource tif))
        tar      (ard/tar-name tif)
        tarpath  (ard/tar-path tar)]
    (if (= (:body @iwdsresp) "[]")
      (swap! ard-to-ingest-atom conj (str ing_resource "/" tarpath "/" tar "/" tif))
      (swap! ingested-ard-atom conj tif)))
  true)

(defn ingest-ard [ard iwds_resource]
 (let [iwds_path (str iwds_resource "/inventory")
       post_opts {:body (json/encode {"url" ard})
                  :headers {"Content-Type" "application/json"
                            "Accept" "application/json"}}
       {:keys [status headers body error] :as ard_resp} @(http/post iwds_path post_opts)]
   (if (= status 200)
     (println "success\n")
     (println "ingest fail\n")))
  true)

(defn -main [& args]
  (let [tileid      (first args)
        action      (last args)
        iwds_host   (:iwds-host   environ/env)
        ard_host    (:ard-host    environ/env)
        ingest_host (:ingest-host environ/env)]

    (when (nil? (re-matches #"[0-9]{6}" tileid))
      (println "Invalid Tile Id: " tileid)
      (System/exit 0))

    (when (not (contains? #{"report" "ingest"} action))
      (println "Invalid action, must use 'report' or 'ingest': " action)
      (System/exit 0))

    (when (nil? iwds_host)
      (println "IWDS_HOST must be defined in your environment, exiting")
      (System/exit 0))

    (when (nil? ard_host)
      (println "ARD_HOST must be defined in your environment, exiting")
      (System/exit 0))

    (let [iwds_resource (str iwds_host "/inventory?only=source&source=")
          ard_resource  (mcore/ard-url-format ard_host tileid)
          ard_download  (str ard_host "/ardtars/")
          ing_resource  (or ingest_host ard_download)
          {:keys [status headers body error] :as ard_resp} @(http/get ard_resource)
          ard_vec (-> body (http-body-to-list) (util/with-suffix "tar") (ard/expand-tars))
          results (pmap #(ard_status_check % iwds_resource ing_resource) ard_vec)]

        (if (= #{true} (set results))
          (println "we got results...")))
          
      (println "")
      (println "Tile Status Report for: " tileid)
      (println "To be ingested: "   (count @ard-to-ingest-atom))
      (println "Already ingested: " (count @ingested-ard-atom))
      (when (= action "ingest")
        (println "ingesting!")
        (let [ing_results (pmap #(ingest-ard % iwds_host) @ard-to-ingest-atom)]
          (when (= #{true} (set ing_results)) (println "Ingest complete!")) )))
  (System/exit 0))

