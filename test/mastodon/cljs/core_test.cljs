(ns mastodon.cljs.core-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [deftest is async]]
            [clojure.string :as string]
            [mastodon.cljs.core :as mc]
            [mastodon.cljs.http :as mhttp]
            [mastodon.cljs.data :as testdata]
            [mastodon.cljc.util :as util]
            [mastodon.cljc.ard :as ard]
            [cljs.core.async :as async]))

(defn test-async
  "Asynchronous test awaiting ch to produce a value or close."
  [ch]
    (let [done (fn [] (prn "done with async test"))]
      (async done
        (async/take! ch (fn [_] (done)))))
)

;; (deftest ard-status-check-test
;;   (let [achan (async/chan 1)
;;         rchan (async/chan 1)]
;;     (go 
;;       (async/>! achan (util/collect-map-values (async/<! (testdata/ard-resp)) :name :type "file"))
;;       (mc/ard-status-check achan "idw.com" (mhttp/mock-idw) "bdiv" "ibtn" "ictr" "mctr" (fn [i] (str i)) rchan)
;;       )

;;     (test-async
;;       (go (is (= 12 (:mis-cnt (async/<! rchan))))))
;;   ) 
;; )
