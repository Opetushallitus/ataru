(ns ataru.virkailija.temporal-test
  (:require [ataru.virkailija.temporal :as temporal]
            [cljs.test :refer-macros [deftest is testing]]))

(deftest parses-java-time-iso-offset-timestamp
  (testing "java.time ISO offset timestamp is accepted in virkailija UI"
    (let [parsed (temporal/str->googdate "2026-04-09T14:52:30+03:00")]
      (is (some? parsed))
      (is (= "09.04.2026 14:52"
             (temporal/time->short-str parsed))))))

(deftest formatting-nil-time-returns-nil
  (testing "failed timestamp parsing does not render current time"
    (is (nil? (temporal/time->short-str nil)))
    (is (nil? (temporal/time->date nil)))
    (is (nil? (temporal/time->str nil)))))
