(ns lcmap.mastodon.clj.main
  (:gen-class)
  (:require [lcmap.mastodon.cljc.core :as mcore]
            [lcmap.mastodon.cljc.util :as util]
            [lcmap.mastodon.cljc.ard :as ard]
            [cheshire.core :as json]
            [environ.core :as environ]
            [org.httpkit.client :as http]
            [clojure.string :as string]))

(def ard-to-ingest-atom            (atom []))
(def ingested-ard-atom             (atom []))
(def ingested-and-missing-ard-atom (atom []))
(def ard-errored-on-ingest-atom    (atom []))

(defn string-to-list [instring]
  (if (nil? instring)
    []
    (do (-> instring (string/replace "[" "") 
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
    (= 1 1))

(defn ingest_error [ard body error tileid]
  (let [ard_log (str "ingest_error_list_" tileid ".log")
        msg_log (str "ingest_error_body_" tileid ".log")]
    (spit ard_log (str ard "\n") :append true)
    (spit msg_log (str ard " - " body " - " error "\n") :append true)))

(defn ingest-ard [ard_list iwds_resource tileid]
  (try 
    (doseq [ard ard_list]
      (let [iwds_path (str iwds_resource "/inventory")
            post_opts {:body (json/encode {"url" ard})
                       :timeout 60000
                       :headers {"Content-Type" "application/json" "Accept" "application/json"}}
            ard_resp (http/post iwds_path post_opts)
            tif_name (last (string/split ard #"/"))]
        (println (str "layer: " tif_name " " (:status @ard_resp)))
        (when (> (:status @ard_resp) 299) 
          (ingest_error ard (:body @ard_resp) (:error @ard_resp) tileid))))
    (= 1 1)
    (catch Exception ex 
      (.printStackTrace ex)
      (str "caught exception in ingest-ard: " (.getMessage ex)))))

(defn -main [& args]
  (let [tileid          (first args)
        autoingest      (last  args)
        iwds_host       (:iwds-host   environ/env)
        ard_host        (:ard-host    environ/env)
        ingest_host     (:ingest-host environ/env)
        partition_level (read-string (:partition-level environ/env))]

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
          ard_resource  (mcore/ard-url-format ard_host tileid)
          ard_download  (str ard_host "/ardtars/")
          ing_resource  (or ingest_host ard_download)
          {:keys [status headers body error] :as resp} @(http/get ard_resource)
          ard_vector    (-> body (string-to-list) (util/with-suffix "tar") (ard/expand-tars))
          ard_results   (pmap #(ard_status_check % iwds_resource ing_resource) ard_vector)]

      ; realize the pmap results
      (= #{true} (set ard_results))

      (println "Tile Status Report for: " tileid)
      (println "To be ingested: "   (count @ard-to-ingest-atom))
      (println "Already ingested: " (count @ingested-ard-atom))
      (println "")

      (if (= autoingest "-y")
        (do 
          (let [ard_partition (partition partition_level partition_level "" @ard-to-ingest-atom)
                 ingest_set    (pmap #(ingest-ard % iwds_host tileid) ard_partition)]
             (count ingest_set) ; realize ingest_set, a lazy sequence
             (println "Ingest complete!")))
        (do (println "Ingest? (y/n)")
          (let [ingest        (read-line)
                ard_partition (partition partition_level partition_level "" @ard-to-ingest-atom)
                ingest_set    (pmap #(ingest-ard % iwds_host tileid) ard_partition)]
            (if (= ingest "y")
              (do (count ingest_set) ; realize ingest_set, a lazy sequence 
                  (println "Ingest Complete!"))
              (do (println "Exiting!")
                  (System/exit 0))))))))
  (System/exit 0))


