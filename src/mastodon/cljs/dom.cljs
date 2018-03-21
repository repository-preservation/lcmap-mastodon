(ns mastodon.cljs.dom
  (:require [cljs.reader :refer [read-string]]
            [clojure.string :as string])
  (:import goog.dom))

(defn set-dom-content
  "Wrapper for dom.setTextContent"
  [id content]
  (let [elem (dom.getElement id)]
    (dom.setTextContent elem content)))

(defn get-dom-content
  "Wrapper for dom.getElement"
  [id]
  (let [elem (dom.getElement id)]
    (read-string (dom.getTextContent elem))))

(defn set-dom-properties
  "Wrapper for dom.setProperties"
  [divid key value]
  (let [domel (dom.getElement divid)]
    (dom.setProperties domel (js-obj key value))))

(defn ^:export set-div-content
  "Exposed function for setting the text contents of a div."
  [divid values]
  (let [content (string/join ", " values)]
    (set-dom-content divid content)))

(defn inc-counter-div
  "Increment by 1 the value within a div."
  [divid & [amt]]
  (let [val (get-dom-content divid)
        imt (or amt 1)
        ival (+ imt val)]
    (set-dom-content divid ival)))

(defn dec-counter-div
  "Decrement by 1 the value within a div."
  [divid & [amt]]
  (let [val (get-dom-content divid)
        inc-amt (or amt 1)
        inc-val (- val inc-amt)
        upd-val (if (neg? inc-val) 0 inc-val)]
    (set-dom-content divid upd-val)))

(defn ^:export reset-counter-divs 
  "Exposed function for resetting counter div content."
  [divs]
  (doseq [d divs]
    (set-dom-content d "0")))

(defn ^:export show-div 
  "Exposed function for un-hiding a div."
  [divid]
  (set-dom-properties divid "style" "display: block"))

(defn hide-div 
  "Hide a div by id."
  [divid]
  (set-dom-properties divid "style" "display: none"))

(defn enable-btn 
  "Enable a button by id."
  [btnid]
  (set-dom-properties btnid "disabled" false))

(defn ^:export disable-btn 
  "Exposed function for disabling a button."
  [btnid]
  (set-dom-properties btnid "disabled" true))

(defn update-for-ard-check
  "Wrapper for DOM updates post Tile status check."
  [params missing-count]
  (inc-counter-div (:ing-ctr (:dom-map params)) (:ingested-count params))
  (inc-counter-div (:mis-ctr (:dom-map params)) (:ard-missing-count params))
  (set-div-content (:iwds-miss-list (:dom-map params)) (:iwds-missing params))
  (reset-counter-divs [(:error-ctr (:dom-map params))])
  (when (> missing-count 0)
    (enable-btn (:ing-btn (:dom-map params))))
  (hide-div (:bsy-div (:dom-map params))))

(defn update-for-ingest-success 
  "Wrapper for DOM updates post successful ingest."
  [params]
  (dec-counter-div (:progress params))
  (dec-counter-div (:missing params))
  (inc-counter-div (:ingested params)))

(defn update-for-ingest-fail 
  "Wrapper for DOM updates post failed ingest."
  [params]
  (inc-counter-div (:error params))
  (dec-counter-div (:progress params)))

(defn update-for-ingest-start
  "Wrapper for DOM updates upon commencing ingest."
  [div count]
  (reset-counter-divs [div])
  (inc-counter-div div count))

(defn update-for-ingest-completion 
  "Wrapper for DOM updates upon completing ingest."
  [busy-div ingesting-div inprogress-div]
  (hide-div busy-div)
  (set-div-content ingesting-div "")
  (set-div-content inprogress-div "0"))

