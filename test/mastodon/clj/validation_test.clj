(ns mastodon.clj.validation-test
  (:require [clojure.test :refer :all] 
            [mastodon.clj.validation :as validation]))

(deftest not-nil?-test
  (is (= (validation/not-nil? 9 "foo") true)))

(deftest does-match?-test
  (is (= (validation/does-match? #"[0-9]{6}" "005015" "TileID") true)))

(deftest is-int?-test
  (is (= (validation/is-int? 9 "Foo") true)))

(deftest validate-cli-test
  (with-redefs [validation/http-accessible? (fn [x] true)]
    (is (= (validation/validate-cli "005015" "iwdshost" "ardhost" 10) true))))

(deftest validate-server-test
  (with-redefs [validation/http-accessible? (fn [x] true)]
    (is (= (validation/validate-server "iwdshost" "ardhost" 10 "/tmp/foo/") true))))



