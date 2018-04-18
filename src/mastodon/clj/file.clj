(ns mastodon.clj.file
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [org.satta.glob :as glob]
            [mastodon.cljc.util :as util]))

(defn strip-path 
  "Return the filename, minus the path"
  [filepath]
  (try
    (last (string/split filepath #"/"))
    (catch Exception ex
      (log/debugf "exception in file/strip-path. arg: %s  message: %s" filepath (.getMessage ex)))
    (finally nil)))

(defn jfile-name 
  "Convert java.io.File object into string of file name"
  [jfile]
  (when (nil? jfile) (log/debugf "nil passed to file/jfile-name"))
  (strip-path (str jfile)))

(defn get-filenames
  "Return list of files for a given filesystem path patter"
  ([filepath]
   (try
     (map jfile-name (glob/glob filepath))
     (catch Exception ex
       (log/debugf "exception in file/get-filenames. arg: %s  message: %s" filepath (.getMessage ex)))
     (finally nil)))
  ([filepath suffix]
   (-> filepath (get-filenames) (util/with-suffix suffix))))

