(ns lcmap.mastodon.clj.main
  (:gen-class)
  (:require [lcmap.mastodon.cljc.core :as mcore]
            [environ.core :as environ]
            [org.httpkit.client :as http]
            [lcmap.mastodon.cljc.ard :as ard]
            [lcmap.mastodon.cljc.util :as util]
            [clojure.string :as string]))

(def ard-to-ingest-atom (atom []))
(def ingested-ard-atom (atom []))
(def ingested-and-missing-ard-atom (atom []))
(def ard-errored-on-ingest-atom (atom []))

(defn http-body-to-list [inbody]
  (-> inbody (string/replace "[" "") (string/replace "]" "") (string/replace "\"" "") (string/split #",")))

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

    (let [iwds_resource (str iwds_host "/inventory?url=")
          ard_resource  (mcore/ard-url-format ard_host tileid)
          ard_download  (str ard_host "/ardtars/")
          ing_resource  (or ingest_host ard_host)
          {:keys [status headers body error] :as ard_resp} @(http/get ard_resource)
          ard_vec (-> body (http-body-to-list)
                           (util/with-suffix "tar")
                           (ard/expand-tars))]

      (doseq [i ard_vec]
        (let [iwdsresp (http/get (str iwds_resource "http://fauxhost.gov/" (ard/full-name i)))]
          (if (= (:body @iwdsresp) "[]")
            (swap! ard-to-ingest-atom conj (ard/tif-path i ard_download ingest_host) ;(ard/full-name i)
                   )
            (swap! ingested-ard-atom conj i ;(ard/full-name i)
                   )))))

      (println "Tile Status Report for: " tileid)
      (println "To be ingested: "   (count @ard-to-ingest-atom))
      (println "Already ingested: " (count @ingested-ard-atom))

      (when (= action "ingest")
        (println "Ingesting! ")
        (doseq [i @ard-to-ingest-atom]
          (println "ingest: " i "\r")))
  )
)

