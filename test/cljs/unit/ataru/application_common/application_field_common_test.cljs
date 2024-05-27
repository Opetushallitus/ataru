(ns ataru.application-common.application-field-common-test
  (:require [cljs.test :refer-macros [deftest is testing are]]
            [ataru.application-common.application-field-common :as common]))

(defn build-question-with-options
  [field-type]
  {:id         "c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0",
   :params     {},
   :options
   [{:label {:fi "Ensimmäinen vaihtoehto", :sv ""}, :value "eka"}
    {:label {:fi "Toinen vaihtoehto", :sv ""}, :value "toka"}
    {:label {:fi "Kolmas vaihtoehto", :sv ""}, :value "kolmas"}
    {:label {:fi "", :sv ""}, :value ""}],
   :fieldType  field-type,
   :fieldClass "formField"})

(def text-field-question
  {:id         "047da62c-9afe-4e28-bfe8-5b50b21b4277",
   :params     {},
   :fieldType  "textField",
   :fieldClass "formField"})

(deftest multiple-choice-values
  (testing "multiple choice values"
    (testing "are kept as is when matching an allowed value"
      (are [field-type]
           (let [field-descriptor (build-question-with-options field-type)
                 answer-value     "toka"
                 sanitized-value  (common/sanitize-value field-descriptor answer-value nil)]
             (is (= sanitized-value answer-value)))
        "dropdown"
        "multipleChoice"
        "singleChoice"))
    (testing "are filtered out when a single value does not match allowed"
      (are [field-type]
           (let [field-descriptor (build-question-with-options field-type)
                 answer-value     "neljäs"
                 sanitized-value  (common/sanitize-value field-descriptor answer-value nil)]
             (is (= sanitized-value nil)))
        "dropdown"
        "multipleChoice"
        "singleChoice"))
    (testing "are left untouched when no padding or filtering is needed"
      (are [field-type]
           (let [field-descriptor (build-question-with-options field-type)
                 answer-value     ["toka" "kolmas" "eka"]
                 sanitized-value  (common/sanitize-value field-descriptor answer-value nil)]
             (is (= sanitized-value answer-value)))
        "dropdown"
        "multipleChoice"
        "singleChoice"))
    (testing "are filtered by options"
      (are [field-type]
           (let [field-descriptor (build-question-with-options field-type)
                 answer-value     ["toka" "kolmas" "neljäs"]
                 sanitized-value  (common/sanitize-value field-descriptor answer-value nil)]
             (is (= sanitized-value ["toka" "kolmas"])))
        "dropdown"
        "multipleChoice"
        "singleChoice"))
    (testing "are filtered by inside question group answers"
      (are [field-type]
           (let [field-descriptor (build-question-with-options field-type)
                 answer-value     [["toka"] ["kolmas"] ["neljäs"]]
                 sanitized-value  (common/sanitize-value field-descriptor answer-value 3)]
             (is (= sanitized-value [["toka"] ["kolmas"] []])))
        "dropdown"
        "multipleChoice"
        "singleChoice"))
    (testing "are nil padded as question group"
      (are [field-type]
           (let [field-descriptor (build-question-with-options field-type)
                 answer-value     [["toka"] ["kolmas"]]
                 sanitized-value  (common/sanitize-value field-descriptor answer-value 4)]
             (is (= sanitized-value [["toka"] ["kolmas"] nil nil])))
        "dropdown"
        "multipleChoice"
        "singleChoice"))
    (testing "are filtered and nil padded"
      (are [field-type]
           (let [field-descriptor (build-question-with-options field-type)
                 answer-value     [["toka"] ["kolmas"] ["foo"]]
                 sanitized-value  (common/sanitize-value field-descriptor answer-value 4)]
             (is (= sanitized-value [["toka"] ["kolmas"] [] nil])))
        "dropdown"
        "multipleChoice"
        "singleChoice"))))

(deftest text-field-values
  (testing "text field values"
    (testing "are not filtered"
      (let [field-descriptor text-field-question
            answer-value     [["foo"] ["bar"] ["baz"]]
            sanitized-value  (common/sanitize-value field-descriptor answer-value 3)]
        (is (= sanitized-value answer-value))))
    (testing "are nil padded to question group maximum dimension"
      (let [field-descriptor text-field-question
            answer-value     [["foo"] ["bar"] ["baz"]]
            sanitized-value  (common/sanitize-value field-descriptor answer-value 5)]
        (is (= sanitized-value [["foo"] ["bar"] ["baz"] nil nil]))))))
  