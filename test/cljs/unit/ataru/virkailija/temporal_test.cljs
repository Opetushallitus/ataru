(ns ataru.virkailija.temporal-test
  (:require [ataru.virkailija.temporal :as temporal]
            [cljs.test :refer-macros [deftest is testing]]))

(deftest parses-java-time-iso-offset-timestamp
  (testing "timestamp with microsecond precision fails"
    (let [parsed (temporal/str->googdate "2022-01-04T16:40:36.688473+02:00")]
      (is (nil? (temporal/time->short-str parsed)))))
  (testing "timestamp with millisecond precision is also accepted"
    (let [parsed (temporal/str->googdate "2022-01-04T16:40:36.688+02:00")]
      (is (some? parsed))
      (is (= "04.01.2022 16:40"
             (temporal/time->short-str parsed)))))
  (testing "parsing an integer fails"
    (let [parsed (temporal/str->googdate "100")]
      (is (nil? (temporal/time->short-str parsed))))))

(deftest formatting-nil-time-returns-nil
  (testing "failed timestamp parsing does not render current time"
    (is (nil? (temporal/time->short-str nil)))
    (is (nil? (temporal/time->date nil)))
    (is (nil? (temporal/time->str nil)))))
