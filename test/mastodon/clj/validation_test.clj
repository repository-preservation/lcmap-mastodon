(ns mastodon.clj.validation-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all] 
            [mastodon.clj.validation :as validation]))

(deftest http?-test
  (with-fake-http [{:url "http://foo.com" :method :get} {:status 200 :body "okay"}]
    (is (= true (validation/http? "http://foo.com" "iwds")))))

(deftest http?-false-test
  (with-fake-http [{:url "http://foo.com" :method :get} {:status 403 :body "forbidden"}]
    (is (= false (validation/http? "http://foo.com" "iwds")))))

(deftest http?-exception-test
  (is (= false (validation/http? nil "iwds"))))

(deftest present?-true-test
  (is (= (validation/present? 9 "foo") true)))

(deftest present?-false-test
  (is (= (validation/present? nil "foo") false)))

(deftest match?-true-test
  (is (= (validation/match? #"[0-9]{6}" "005015" "TileID") true)))

(deftest match?-false-test
  (is (= (validation/match? #"[0-9]{6}" "005015xx" "TileID") false)))

(deftest match?-exception-test
  (is (= (validation/match? nil "005015" "TileID") false)))

(deftest int?-true-test
  (is (= (validation/int? 9 "Foo") true)))

(deftest int?-false-test
  (is (= (validation/int? nil "Foo") false)))

(deftest validate-cli-ard-test
  (with-redefs [validation/http? (fn [x y] true)]
    (is (= (validation/validate-cli "005015" {:chipmunk_host "iwdshost" :ard_host "ardhost" :partition_level 10 :data_type "ard"}) true))))

(deftest validate-cli-aux-test
  (with-redefs [validation/http? (fn [x y] true)]
    (is (= (validation/validate-cli "005015" {:chipmunk_host "iwdshost" :ard_host "ardhost" :aux_host "auxhost" :partition_level 10 :data_type "aux"}) true))))

(deftest validate-ard-server-test
  (with-redefs [validation/http? (fn [x y] true)]
    (is (= (validation/validate-server {:data_type "ard" :chipmunk_host "iwdshost" :ard_host "ardhost" :partition_level 10 :ard_path "/tmp/foo/"} ) true))))

(deftest validate-aux-server-test
  (with-redefs [validation/http? (fn [x y] true)]
    (is (= (validation/validate-server {:data_type "aux" :chipmunk_host "iwdshost" :ard_host "ardhost" :aux_host "auxhost" :partition_level 10} ) true))))


