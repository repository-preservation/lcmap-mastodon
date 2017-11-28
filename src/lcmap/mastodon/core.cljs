(ns lcmap.mastodon.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.data :as data]
            [clojure.string :as string]
            [clojure.set :as set]
            [lcmap.mastodon.http :as http]
            [lcmap.mastodon.ard-maps :as ard-maps]
            [lcmap.mastodon.data :as mdata]
            [cljs.core.async :refer [<! >! chan pipeline]]
            [cljs.reader :refer [read-string]]
)
  (:import goog.dom))

(enable-console-print!)
(println "Hello from LCMAP Mastodon!")

(def ard-chan (chan 1))
(def idw-chan (chan 1))
(def ard-miss-atom (atom []))

(defn log [msg]
  (.log js/console msg)
)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(defn hv-map
  "Helper function.
   Return map for :h and :v given
   a tileid of hhhvvv e.g 052013

   ^String :id: 6 character string representing tile id
   ^Expression :regx: Optional expression to parse tile id

   Returns map with keys :h & :v"
  [id & [regx]]
  (hash-map :h (first (re-seq (or regx #"[0-9]{3}") id)) 
            :v (last  (re-seq (or regx #"[0-9]{3}") id)))
)

(defn tile-id-rest
  "Helper function.
   Takes a 6 digit tileid, and returns a
   string splitting the H and V values so
   they may be used in generating a URL
   for an inventory request.
   ^String :tile-id: 6 character string representing a tileid

   Returns a string with a forward slash separating H and V values
   e.g. /hhh/vvv/
  "
  [tile-id]
  (string/join "/" [(:h (hv-map tile-id)) (:v (hv-map tile-id)) ""])
)

(defn get-map-val
  "Helper function.
   Return particular value for a map,
   for the conditional key and value

   ^Keyword :desired-key:     Desired key
   ^Keyword :conditional-key: Conditional key
   ^String  :conditional-val: Conditional key val
   ^Map     :map-obj: Map object
  "
  [map-obj desired-key & [conditional-key conditional-val]]
  (let [ck (or conditional-key :nil)]
    (if (= conditional-val (ck map-obj))
      (desired-key map-obj)
    )
  )
)

(defn collect-map-values
  "Data organization function.
   Takes a list of maps and returns
   a collection of values for specified
   key, if another specified key/value
   pair exists in the map
   
   ^List :inmaps: List of maps representing dir contents as json
   ^Keyword :dk: Desired key to collect from ard-response
   ^Keyword :ck: Conditional key to check value of
   ^String  :cv: Conditional value to check value of

   Returns list of dk values from ard-response"
  [map-list desired-key & [conditional-key conditional-value]]
  (let [rcount (count map-list)]
       (map get-map-val
           map-list
           (repeat rcount desired-key) 
           (repeat rcount conditional-key) 
           (repeat rcount conditional-value))
    )
)

(defn ard-url-format
  "URL generation function for requests to an ARD file access server

   ^String :host:    Host name
   ^String :tile-id: Tile ID
  "
  [host tile-id]
  (string/join "/" [host (tile-id-rest tile-id)])
)

(defn idw-url-format
  "URL generation function for requests to an LCMAP-Chipmunk instance

   ^String :host:    lcmap-chipmunk instance host
   ^String :tile-id: Tile ID
  "
  ;; $ curl -XGET http://localhost:5656/inventory?tile=027009
  [host tile-id]
  (str host "/inventory?tile=" tile-id)
)

(defn key-for-value
  "Convenience Function
   Return the key from provided map whose value object
   includes the provided value"
  [in-map in-value]
  (let [matching-key-list
        (map (fn [kv] (when (string/includes? (val kv) in-value) (key kv)))
             in-map)]
    ;; is there a better way? there should only be one match
    (first (remove nil? matching-key-list)))
)

(defn tar-name
  "Derive an ARD tif files original containing Tar file name"
  [tif-name]
  ;; LC08_CU_027009_20130701_20170729_C01_V01_PIXELQA.tif
  ;; LC08_CU_027009_20130701_20170729_C01_V01_QA.tar
  ;; (name :var_name) (keyword "str_name")
  (let [tname (string/replace tif-name ".tif" "")
        tlst  (string/split tname "_")
        tval  (last tlst)
        tkey  (name (key-for-value ard-maps/tar-map tval))]
       (str (string/replace tname tval tkey) ".tar") 
  )
)

(defn ard-manifest
  "From a ARD tar file name, generate a map 
   keyed by tar file name of its expected contents.

   ^String :ard-tar:"
  [ard-tar]
  (let [tname (string/replace ard-tar ".tar" "")
        tlst  (string/split tname "_")
        tval  (last tlst)
        tkey  (keyword tval)
        tifs  (tkey ard-maps/tar-map)]
      (map (fn [x] (str (string/replace tname tval x) ".tif")) tifs)
  )
)

(defn inc-counter-div
  "Increment by 1 the value within a div.

   ^String :divid:"
  [divid]
  (let [div (dom.getElement divid)
        val (read-string (dom.getTextContent div))
        ival (inc val)]
    (dom.setTextContent div ival)
   )
)

(defn reset-counter-divs [divs]
  (doseq [d divs]
    (let [i (dom.getElement d)]
      (dom.setTextContent i "0")
    )
  )
)

(defn show-div [divid]
  (let [div (dom.getElement divid)]
    (dom.setProperties div (js-obj "style" "display: block"))
  )
)

(defn hide-div [divid]
  (let [div (dom.getElement divid)]
    (dom.setProperties div (js-obj "style" "display: none"))
  )
)

(defn fresh-includes [coll i]
  (if (contains? (set coll) i)
    (do (log (str "already accounted for: " i))
        coll)
    (do (log (str "new item: " i))
        (conj coll i)
    )
  )
)

(defn ard-status-check 
  "Check whether available ARD has been ingested. If it hasn't
   place it in the requested Atom.

   ^ core.async.chan :ard-c:
   ^ String :idw-url:
   ^ Func :idw-rqt:"
  [ard-c idw-url idw-rqt]
  (reset-counter-divs ["ardingested-counter" "ardmissing-counter"])
  (go-loop []
    (let [tifs (ard-manifest (<! ard-c))
          idw-resp (:result (<! (idw-rqt idw-url)))
          idw-tifs (collect-map-values idw-resp :source)]
          ;; run through the tifs, checking if they're included in the IDWS response
          (doseq [i tifs]
            (if (contains? (set idw-tifs) i)
              (inc-counter-div "ardingested-counter")
              (do (inc-counter-div "ardmissing-counter") 
                  (swap! ard-miss-atom fresh-includes i))))
    )
    (recur))
)

(defn inventory-diff
  "Diff function, comparing what source files the ARD source has available, 
   and what sources have been ingested into the data warehouse for a specific tile

   ^String :ardh: ARD Host
   ^String :idwh: IDW Host
   ^String :hv:   Tile ID
   ^String :reg:  Region

   Returns vector (things only in ARD, things only in IDW)
  "
  [ard-host idw-host tile-id region & [ard-req-fn idw-req-fn]]

    (let [ard-rqt  (or ard-req-fn http/get-request)
          idw-rqt  (or idw-req-fn http/get-request)
          ard-url  (ard-url-format ard-host tile-id)
          idw-url  (idw-url-format idw-host tile-id)
          busy-div "busydiv"]

         ;; turn on busy signal
         (show-div busy-div)
         ;; park functions on ard-chan
         (ard-status-check ard-chan idw-url idw-rqt)
         ;; transfer items to ard-chan
         (go
           (doseq [i (<! (ard-rqt ard-url))]
             (when (= (:type i) "file")
               (>! ard-chan (:name i))))
           (hide-div busy-div))
         ;; tifs not yet ingested into the IWDS are now listed in the ard-miss-atom atom
    )
)

(defn inventory-mock []
  (inventory-diff "ardhost.com" "idwshost.com" "023023" "CU" 
                  #(http/get-request % (mdata/ard-resp)) 
                  #(http/get-request % (mdata/idw-resp)))
)


(defn ingest-req []

  (doseq [i @ard-miss-atom]
    (log (str "ingest: " i)))

)


