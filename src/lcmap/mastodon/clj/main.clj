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

;; (swap! sweet-atom conj <value>)
;; @sweet-atom

(defn http-body-to-list [inbody]
  (-> inbody (string/replace "[" "") (string/replace "]" "") (string/replace "\"" "") (string/split #","))
)

(defn -main [& args]
  (let [tileid (first args)
        iwds_host (:iwds-host environ/env)
        ard_host  (:ard-host environ/env)
        ingest_host (:ingest-host environ/env)
        ;;iwds_resource (mcore/iwds-url-format iwds_host tileid)
        iwds_resource (str iwds_host "/inventory?url=")
        ard_resource (mcore/ard-url-format ard_host tileid)
        {:keys [status headers body error] :as ard_resp} @(http/get ard_resource)
        ard_vec (-> body (http-body-to-list)
                         (util/with-suffix "tar")
                         (ard/expand-tars))
      ]

    (doseq [i ard_vec]
     ;; (println "ingesting " (str "http://fauxhost.gov/" (ard/full-name i)))
      ;; make iwds inventory request
      ;; if body is empty, ard has not been ingested
      ;; add it to ard-to-ingest-atom
      ;; else add it to ingested-ard-atom
        (let [iwdsresp (http/get (str iwds_resource "http://fauxhost.gov/" (ard/full-name i)))]
          (if (= (:body @iwdsresp) "[]")
            (swap! ard-to-ingest-atom conj (ard/full-name i))
            (swap! ingested-ard-atom conj (ard/full-name i))
            )          


          )
      )

      (println "To be ingested: " (count @ard-to-ingest-atom))
      (println "Already ingested: " (count @ingested-ard-atom))


    )
  
)
