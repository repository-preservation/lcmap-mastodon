(ns mastodon.clj.validation-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all] 
            [mastodon.clj.validation :as validation]))

(deftest http-accessible?-test
  (with-fake-http [{:url "http://foo.com" :method :get} {:status 200 :body "okay"}]
    (is (= true (validation/http-accessible? "http://foo.com" "iwds")))))

(deftest http-accessible?-false-test
  (with-fake-http [{:url "http://foo.com" :method :get} {:status 403 :body "forbidden"}]
    (is (= false (validation/http-accessible? "http://foo.com" "iwds")))))

(deftest not-nil?-test
  (is (= (validation/not-nil? 9 "foo") true)))

(deftest does-match?-test
  (is (= (validation/does-match? #"[0-9]{6}" "005015" "TileID") true)))

(deftest is-int?-test
  (is (= (validation/is-int? 9 "Foo") true)))

(deftest validate-cli-test
  (with-redefs [validation/http-accessible? (fn [x y] true)]
    (is (= (validation/validate-cli "005015" {:chipmunk_host "iwdshost" :ard_host "ardhost" :partition_level 10}) true))))

(deftest validate-server-test
  (with-redefs [validation/http-accessible? (fn [x y] true)]
    (is (= (validation/validate-server "aux" "iwdshost" "ardhost" "auxhost" 10 "/tmp/foo/") true))))



