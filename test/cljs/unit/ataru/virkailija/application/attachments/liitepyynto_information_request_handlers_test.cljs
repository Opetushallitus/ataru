(ns ataru.virkailija.application.attachments.liitepyynto-information-request-handlers-test
  (:require [ataru.virkailija.application.attachments.liitepyynto-information-request-handlers :as handlers]
            [cljs.test :refer-macros [deftest is testing]]))

(deftest parses-backend-field-deadline-timestamps
  (testing "timestamp without milliseconds"
    (let [[date time] (handlers/parse-deadline "2026-05-07T09:00:00Z")]
      (is (string? date))
      (is (string? time))))
  (testing "timestamp with milliseconds"
    (let [[date time] (handlers/parse-deadline "2026-05-07T09:00:00.000Z")]
      (is (string? date))
      (is (string? time)))))
