(ns ataru.application-common.option-visibility-test
  (:require [cljs.test :refer-macros [are deftest is testing]]
            [ataru.application.option-visibility :as option-visibility]))

(defn- build-option [props]
  (merge {:value "0"}
         props))

(defn- build-field-descriptor [props]
  (merge {:fieldType "dropdown"
          :options   []}
         props))

(deftest visibility-checker-test
  (testing "visibility-checker: for field types: dropdown, multipleChoice, singleChoice:"
    (testing "when option is selected in answer:"
      (testing "option is visible:"
        (are [field-type]
          (let [field-descriptor (build-field-descriptor {:fieldType field-type})
                answer-value     "0"
                option-visible?  (option-visibility/visibility-checker field-descriptor answer-value)]
            (true? (option-visible? (build-option {:value "0"}))))
          "dropdown"
          "multipleChoice"
          "singleChoice")))

    (testing "when option is not selected in answer:"
      (testing "option is not visible:"
        (are [field-type]
          (let [field-descriptor (build-field-descriptor {:fieldType field-type})
                answer-value     "1"
                option-visible?  (option-visibility/visibility-checker field-descriptor answer-value)]
            (false? (option-visible? (build-option {:value "0"}))))
          "dropdown"
          "multipleChoice"
          "singleChoice")))

    (testing "when option is selected in answer to question group:"
      (testing "option is visible:"
        (are [answer-value]
          (let [field-descriptor (build-field-descriptor {:fieldType "dropdown"})
                option-visible?  (option-visibility/visibility-checker field-descriptor answer-value)]
            (true? (option-visible? (build-option {:value "0"}))))
          [["0"]]
          [["1"] ["0"]]
          [nil ["0"]])))

    (testing "when option is not selected in answer to question group:"
      (testing "option is not visible:"
        (are [answer-value]
          (let [field-descriptor (build-field-descriptor {:fieldType "dropdown"})
                option-visible?  (option-visibility/visibility-checker field-descriptor answer-value)]
            (false? (option-visible? (build-option {:value "0"}))))
          [[]]
          [["1"]]
          [["1"] ["1"]]
          [nil ["1"]]))))

  (testing "visibility-checker: for textField"
    (testing "option without condition:"
      (testing "when non-empty string in answer:"
        (testing "option is visible:"
          (let [field-descriptor (build-field-descriptor {:fieldType "textField"})
                answer-value     "non-empty answer"
                option-visible?  (option-visibility/visibility-checker field-descriptor answer-value)]
            (is (true? (option-visible? (build-option {})))))))

      (testing "when empty string in answer:"
        (testing "option is not visible:"
          (let [field-descriptor (build-field-descriptor {:fieldType "textField"})
                answer-value     ""
                option-visible?  (option-visibility/visibility-checker field-descriptor answer-value)]
            (is (false? (option-visible? (build-option {}))))))))

    (testing "option with condition:"
      (testing "text field not in question group:"
        (are [answer-value comparison-operator answer-compared-to expected-visibility]
          (let [field-descriptor (build-field-descriptor {:fieldType "textField"})
                option-visible?  (option-visibility/visibility-checker field-descriptor answer-value)]
            (= expected-visibility (option-visible? (build-option
                                                      {:condition {:answer-compared-to  answer-compared-to
                                                                   :comparison-operator comparison-operator}}))))
          "11" "=" 12 false
          "12" "=" 12 true
          "13" "<" 12 false
          "11" "<" 12 true
          "11" ">" 12 false
          "13" ">" 12 true))

      (testing "text field in question group:"
        (are [answer-value comparison-operator answer-compared-to expected-visibility]
          (let [field-descriptor (build-field-descriptor {:fieldType "textField"})
                option-visible?  (option-visibility/visibility-checker field-descriptor answer-value)]
            (= expected-visibility (option-visible? (build-option
                                                      {:condition {:answer-compared-to  answer-compared-to
                                                                   :comparison-operator comparison-operator}}))))
          [["11"]] "=" 12 false
          [["12"]] "=" 12 true
          [["11"] ["11"]] "=" 12 false
          [["11"] ["12"]] "=" 12 true
          [[]] "=" 12 false
          [nil ["12"]] "=" 12 true))))

  (testing "visibility-checker: for other field types"
    (testing "option is visible:"
      (let [field-descriptor (build-field-descriptor {:fieldType "other field type"})
            answer-value     "does not matter"
            option-visible?  (option-visibility/visibility-checker field-descriptor answer-value)]
        (is (true? (option-visible? (build-option {:value "any option value"}))))))))
