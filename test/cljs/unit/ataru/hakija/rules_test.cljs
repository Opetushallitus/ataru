(ns ataru.hakija.rules-test
  (:require [cljs.test :refer-macros [deftest are is]]
            [ataru.hakija.rules :as rules]
            [taoensso.timbre :refer-macros [spy debug]]))

(deftest rule-runner
  (let [rule-fn                 (fn [db argument]
                                  (do
                                    (is (= argument [:argument :a :b :c]))
                                    (is (= (:this-is-a-test-db db) :test-db))
                                    (assoc db :rule-fn-ran? true)))
        rule-fn2                (fn [db argument]
                                  (do
                                    (is (= argument [:foo]))
                                    (assoc db :rule-fn2-ran? true)))
        rule-to-fn              (fn [rule]
                                  (case rule
                                    :test-rule   rule-fn
                                    :test-rule-2 rule-fn2))
        {:keys [rule-fn-ran?
                rule-fn2-ran?]} (rules/run-rules rule-to-fn
                                                 {:test-rule   [:argument :a :b :c]
                                                  :test-rule-2 [:foo]}
                                                 {:this-is-a-test-db :test-db})]
    (is (= true rule-fn-ran?))
    (is (= true rule-fn2-ran?))))

