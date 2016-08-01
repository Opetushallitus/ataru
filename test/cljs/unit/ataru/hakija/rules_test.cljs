(ns ataru.hakija.rules-test
  (:require [cljs.test :refer-macros [deftest are is]]
            [ataru.hakija.rules :as rules]
            [taoensso.timbre :refer-macros [spy debug]]))

(deftest rule-runner
  (let [rule-fn                (fn [db argument]
                                 (do
                                   (is (= argument [:argument :a :b :c]))
                                   (is (= (:this-is-a-test-db db) :test-db))
                                   (assoc db :rule-fn-ran? true)))
        rule-to-fn             (fn [rule]
                                 (is (= rule :test-rule))
                                 rule-fn)
        {:keys [rule-fn-ran?]} (rules/run-rules rule-to-fn
                                 {:test-rule [:argument :a :b :c]}
                                 {:this-is-a-test-db :test-db})]
    (is (= true rule-fn-ran?))))

