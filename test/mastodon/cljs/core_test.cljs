(ns mastodon.cljs.core-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [deftest is async]]
            [clojure.string :as string]
            [mastodon.cljs.core :as mc]
            [mastodon.cljs.http :as http]
            [mastodon.cljs.data :as testdata]
            [mastodon.cljs.dom :as dom]
            [mastodon.cljc.util :as util]
            [cljs.core.async :as async]))

(defn test-async
  "Asynchronous test awaiting ch to produce a value or close."
  [ch]
    (let [done (fn [] (prn "done with async test"))]
      (async done
        (async/take! ch (fn [_] (done))))))

(deftest year-select-options-test
  (let [years (js->clj (mc/year-select-options))]
    (is (= years (filter int? years)))
    (is (= 1982 (first years)))))

(deftest report-assessment-test
  (with-redefs [mc/log (fn [x] x)]
      (let [achan  (async/chan 1) 
            inpmap {:body {:ingested 9 :missing ["a" "e" "i"]}}
            dommap {:busydiv "busydiv"}]
        
        (go (async/>! achan inpmap))
        (test-async
          (go (is (= {{:iwds-missing [], :ingested-count 9, :dom-map {:busydiv "busydiv"}, :ard-missing-count 3} 3} 
                     (async/<! (mc/report-assessment achan dommap (fn [a b] {a b}))))))))))

(deftest report-assessment-error-test
      (let [achan  (async/chan 1) 
            inpmap {:body {:error "there was a massive failure"}}
            dommap {:busydiv "busydiv"}]
        
        (go (async/>! achan inpmap))
        (test-async
          (go (is (= {"error-container" ["Error reaching ARD server: there was a massive failure"]} 
                     (async/<! (mc/report-assessment achan dommap (fn [a b] {a b}) (fn [a b] {a b}) (fn [a] true)))))))))

(deftest make-chipmunk-requests-test
  (with-redefs [http/post-request (fn [url parms] {:status 200 :body [{:foo 200} {:bar 200}]})
                mc/ingest-status-handler (fn [a b] {a b})]
    (let [ichan (async/chan 1)]

    (go (async/>! ichan ["a" "b"]))

    (test-async
     (go (is (= ["busydiv" "ingdiv" "progdiv"] 
                (async/<! (mc/make-chipmunk-requests ichan "ardhost" "busydiv" "ingdiv" "progdiv" 2 {:foo "bar"} (fn [a b c] [a b c]))))))))))

(deftest ingest-status-handler-test
  (with-redefs [mc/log (fn [x] {:foo "bar"})
                dom/set-div-content (fn [x y] {x y})
                dom/update-for-ingest-success (fn [x] (println "success!!!") true)
                dom/update-for-ingest-fail (fn [x] (println "failure!!!") false)]

        (is (= {:status 200, :tifs '("boo.tif" "who.tif"), :body [{:foo.tar/boo.tif 200} {:mang.tar/who.tif 200}]}
           (mc/ingest-status-handler 200 [{:foo.tar/boo.tif 200} {:mang.tar/who.tif 200}] {"div1" 4})))))


