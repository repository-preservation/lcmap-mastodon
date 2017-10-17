(ns mastodon.core-spec 
  (:require-macros [speclj.core :refer [describe it should=]])
  (:require [speclj.core]
            [mastodon.core]))

(describe "hvdiff should return a tuple of lists"
  (it "should return empty lists"
    (should= 0 0)))



(run-specs)
