(ns mastodon.clj.validation-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all] 
            [mastodon.clj.validation :as validation]))

(deftest http-accessible?-test
  (with-fake-http [{:url "http://foo.com" :method :get} {:status 200 :body "okay"}]
    (is (= true (validation/http? "http://foo.com" "iwds")))))

(deftest http-accessible?-false-test
  (with-fake-http [{:url "http://foo.com" :method :get} {:status 403 :body "forbidden"}]
    (is (= false (validation/http? "http://foo.com" "iwds")))))

(deftest present?-test
  (is (= (validation/present? 9 "foo") true)))

(deftest match?-test
  (is (= (validation/match? #"[0-9]{6}" "005015" "TileID") true)))

(deftest is-int?-test
  (is (= (validation/int? 9 "Foo") true)))

(deftest validate-cli-test
  (with-redefs [validation/http? (fn [x y] true)]
    (is (= (validation/validate-cli "005015" {:chipmunk_host "iwdshost" :ard_host "ardhost" :partition_level 10}) true))))

(deftest validate-server-test
  (with-redefs [validation/http? (fn [x y] true)]
    (is (= (validation/validate-server {:server_type "aux" :chipmunk_host "iwdshost" :ard_host "ardhost" :aux_host "auxhost" :partition_level 10 :ard_path "/tmp/foo/"} ) true))))



