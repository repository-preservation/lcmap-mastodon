(ns lcmap.mastodon.dom
  (:require [cljs.reader :refer [read-string]])
  (:import goog.dom))

(defn inc-counter-div
  "Increment by 1 the value within a div.

   ^String :divid:"
  [divid & [amt]]
  (let [div (dom.getElement divid)
        val (read-string (dom.getTextContent div))
        imt (or amt 1)
        ival (+ imt val)]
    (dom.setTextContent div ival)
   )
)

(defn dec-counter-div
  "Decrement by 1 the value within a div.

  ^String :divid:"
  [divid & [amt]]
  (let [div (dom.getElement divid)
        val (read-string (dom.getTextContent div))
        imt (or amt 1)
        ival (- val imt)]
    (dom.setTextContent div ival)
  )
)

(defn ^:export reset-counter-divs [divs]
  (doseq [d divs]
    (let [i (dom.getElement d)]
      (dom.setTextContent i "0")
    )
  )
)

(defn ^:export show-div [divid]
  (let [div (dom.getElement divid)]
    (dom.setProperties div (js-obj "style" "display: block"))
  )
)

(defn hide-div [divid]
  (let [div (dom.getElement divid)]
    (dom.setProperties div (js-obj "style" "display: none"))
  )
)

(defn enable-btn [btnid]
  (let [btn (dom.getElement btnid)]
    (dom.setProperties btn (js-obj "disabled" false))
  )
)

(defn ^:export disable-btn [btnid]
  (let [btn (dom.getElement btnid)]
    (dom.setProperties btn (js-obj "disabled" true))
  )
)

(defn update-for-ard-check [params missing-count]
  (inc-counter-div (:ing-ctr (:dom-map params)) (:ingested-count params))
  (inc-counter-div (:mis-ctr (:dom-map params)) (:ard-missing-count params))
  (inc-counter-div (:iwds-miss-ctr (:dom-map params)) (:iwds-missing-count params))
  (reset-counter-divs [(:error-ctr (:dom-map params))])
  (hide-div   (:bsy-div (:dom-map params)))
  (when (> missing-count 0)
    (enable-btn (:ing-btn (:dom-map params))))
)

(defn update-for-ingest-success [params]
  (dec-counter-div (:progress params))
  (dec-counter-div (:missing params))
  (inc-counter-div (:ingested params))
)

(defn update-for-ingest-start [div count]
    (reset-counter-divs [div])
    (inc-counter-div div count)
)
