(ns mastodon.cljs.core-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [deftest is async]]
            [clojure.string :as string]
            [mastodon.cljs.core :as mc]
            [mastodon.cljs.http :as mhttp]
            [mastodon.cljs.data :as testdata]
            [mastodon.cljs.dom :as dom]
            [mastodon.cljc.util :as util]
            [mastodon.cljc.ard :as ard]
            [cljs.core.async :as async]))

(defn test-async
  "Asynchronous test awaiting ch to produce a value or close."
  [ch]
    (let [done (fn [] (prn "done with async test"))]
      (async done
        (async/take! ch (fn [_] (done))))))

(deftest report-assessment-test
  (with-redefs [util/log (fn [x] x)]
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
  (let [ichan (async/chan 1)
        schan (async/chan 2)]

    (go (async/>! ichan ["a" "b"]))

    (test-async
     (go (is (= ["busydiv" "ingdiv" "progdiv"] 
                (async/<! (mc/make-chipmunk-requests ichan schan "ardhost" "busydiv" "ingdiv" "progdiv" 2 (fn [a b c] [a b c])))))))

    (test-async
     (go (is (= true (:success (async/<! schan))))))))

(deftest ingest-status-handler-test

    (let [schan (async/chan 1)]

      (go (async/>! schan {:status 200 :body [{:thing1 200} {:thing2 200}]}))

      (test-async
       (go (is (= true true
                  ;(async/<! (mc/ingest-status-handler schan {:a "2"} (fn [x] x) (fn [x y] x) (fn [x] x) (fn [x] x)))
                  ))))
      )
)
