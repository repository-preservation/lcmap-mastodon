(ns lcmap.mastodon.clj.file
  (:require [clojure.string :as string] 
            [org.satta.glob :as glob]))

(defn strip-path 
  "Return the filename, minus the path"
  [filepath]
  (last (string/split filepath #"/")))

(defn jfile-name 
  "Convert java.io.File object into string of file name"
  [jfile]
  (strip-path (str jfile)))

(defn get-filenames
  "Return list of files for a given filesystem path patter"
  [filepath]
  (map jfile-name (glob/glob filepath)))

