(ns mastodon.cljs.dom-test
  (:require [cljs.test :refer-macros [deftest is]]
            [clojure.string :as string]
            [mastodon.cljs.dom :as dom]))

(def testparams {:dom-map {:ing-ctr "ingest" :mis-ctr "missing" :iwds-miss-list "iwdsmissing"
                           :error-ctr "errors" :ing-btn "ingbutton" :bsy-div "busydiv"}
                 :ingested-count 32
                 :ard-missing-count 5
                 :iwds-missing 4
                 :progress 3
                 :missing 2
                 :ingested 1})

(deftest set-div-content-test
  (with-redefs [dom/set-dom-content (fn [x y] {x y})]
    (is (= {"foo" "bar, is, me"} (dom/set-div-content "foo" ["bar" "is" "me"])))))

(deftest inc-counter-div-test
  (with-redefs [dom/get-dom-content (fn [i] 1) 
                dom/set-dom-content (fn [x y] {x y})]
    (is (= {"foo" 2} (dom/inc-counter-div "foo")))))

(deftest dec-counter-div-test
  (with-redefs [dom/get-dom-content (fn [i] 5) 
                dom/set-dom-content (fn [x y] {x y})]
    (is (= {"foo" 4} (dom/dec-counter-div "foo")))))

(deftest show-div-test
  (with-redefs [dom/set-dom-properties (fn [a b c] [a b c])]
    (is (= '("foo" "style" "display: block") (dom/show-div "foo")))))

(deftest hide-div-test
  (with-redefs [dom/set-dom-properties (fn [a b c] [a b c])]
    (is (= '("foo" "style" "display: none") (dom/hide-div "foo")))))

(deftest enable-btn-test
  (with-redefs [dom/set-dom-properties (fn [a b c] [a b c])]
    (is (= ["foo" "disabled" false] (dom/enable-btn "foo")))))

(deftest disable-btn-test
  (with-redefs [dom/set-dom-properties (fn [a b c] [a b c])]
    (is (= ["foo" "disabled" true] (dom/disable-btn "foo")))))

(deftest update-for-ard-check-test
  (with-redefs [dom/inc-counter-div (fn [a b] (constantly true))
                dom/set-div-content (fn [a b] (constantly true))
                dom/reset-counter-divs (fn [a] (constantly true))
                dom/hide-div (fn [x] x)]
    (is (= "busydiv" (dom/update-for-ard-check testparams 0)))))

(deftest update-for-ingest-success-test
  (with-redefs [dom/dec-counter-div (fn [x] x)
                dom/inc-counter-div (fn [x] x)]
    (is (= 1 (dom/update-for-ingest-success testparams)))))

(deftest update-for-ingest-fail-test
  (with-redefs [dom/dec-counter-div (fn [x] x)
                dom/inc-counter-div (fn [x] x)]
    (is (= 3 (dom/update-for-ingest-fail testparams)))))

(deftest update-for-ingest-start-test
  (with-redefs [dom/reset-counter-divs (fn [x] x)
                dom/inc-counter-div (fn [a b] {a b})]
    (is (= {"a" "b"} (dom/update-for-ingest-start "a" "b")))))

(deftest update-for-ingest-completion-test
  (with-redefs [dom/hide-div (fn [x] x)
                dom/set-div-content (fn [a b] {a b})]
    (is (= {"foodiv" ""} (dom/update-for-ingest-completion "busydiv" "ingesting" "foodiv")))))
