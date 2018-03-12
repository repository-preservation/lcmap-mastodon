(ns lcmap.mastodon.clj.server
   (:require [clojure.string                 :as string]
             [compojure.core                 :as compojure]
             [compojure.route                :as route]
             [environ.core                   :as environ]
             [lcmap.mastodon.cljc.ard        :as ard]
             [lcmap.mastodon.cljc.util       :as util]
             [lcmap.mastodon.clj.file        :as file]
             [lcmap.mastodon.clj.persistance :as persist]
             [ring.middleware.json           :as ring-json]))

(defn bulk-ingest
  "Generate ingest requests for list of posted ARD"
  [{:keys [:body] :as req}]
  (let [tifs    (string/split (:urls body) #",")
        iwds    (:iwds-host environ/env)
        results (doall (pmap #(persist/ingest % iwds) tifs))]
    {:status 200 :body results}))

(defn ard-status
  [tileid]
  (let [hvmap    (util/hv-map tileid)
        filepath (-> (:ard-path environ/env) (str (:h hvmap) "/" (:v hvmap) "/*"))
        ardtifs  (-> filepath (file/get-filenames)
                              (util/with-suffix "tar")
                              (#(map ard/ard-manifest %))
                              (flatten))
        iwds_src (-> (:iwds-host environ/env) (str "/inventory?only=source&source="))
        ing_src  (-> (:ard-host environ/env) (str "/ard"))
        ard_res  (doall (pmap #(persist/status-check % iwds_src ing_src) ardtifs))]

    (let [missing  (filter (fn [i] (= (vals i) '("[]"))) ard_res)
          ingested (filter (fn [i] (not (= (vals i) '("[]")))) ard_res)
          miss_count   (count missing)
          ingest_count (count ingested)
          miss_flat (keys (apply merge-with concat missing))]
      {:status 200 :body {:ingested ingest_count :missing miss_flat}})))

(defn get-base [request]
  {:status 200 :body ["Would you like some ARD with that?"]})

;; ## Routes
(compojure/defroutes routes
  (compojure/context "/" request
    (route/resources "/")
    (compojure/GET   "/" [] (get-base request))
    (compojure/GET   "/inventory/:tileid{[0-9]{6}}" [tileid] (ard-status tileid))
    (compojure/POST  "/bulk-ingest" [] (bulk-ingest request))))

(def app (-> routes
             (ring-json/wrap-json-body {:keywords? true})
             (ring-json/wrap-json-response)))
