(ns mastodon.clj.file
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [org.satta.glob :as glob]
            [mastodon.cljc.util :as util]))

(defn strip-path
  "Return the filename, minus the path"
  [filepath]
  (-> filepath
      (str)
      (string/split #"/")
      (last)))

(defn jfile-name 
  "Convert java.io.File object into string of file name"
  [jfile]
  (strip-path (str jfile)))

(defn get-filenames
  "Return list of files for a given filesystem path pattern"
  ([filepath]
   (try
     (-> filepath
         (str)
         (glob/glob)
         (#(map jfile-name %)))
     (catch Exception ex
       (log/debugf "invalid arg passed to file/get-filenames: %s message: %s" filepath (util/exception-cause-trace ex "mastodon"))
       [nil])))
  ([filepath suffix]
   (-> filepath
       (get-filenames)
       (util/with-suffix suffix))))

