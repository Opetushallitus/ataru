(ns ataru.hakija.application.field-visibility-test
  (:require [cljs.test :refer-macros [are deftest is testing]]
            [ataru.hakija.application.field-visibility :as field-visibility]))

(defn- ui-of [db]
  (get-in db [:application :ui]))

(deftest set-field-visibility-for-plain-field-test
  (testing "answers stays intact:"
    (is (= {:application {:answers {:kysymys {:value "0"}}}}
           (update-in
             (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "0"}}}}
                                                    {:id "kysymys"})
             [:application]
             select-keys [:answers]))))
  (testing "field is always visible:"
    (is (= {:kysymys {:visible? true}}
           (ui-of
             (field-visibility/set-field-visibility {} {:id "kysymys"}))))))

(deftest set-field-visibility-for-hakeminen-tunnistautuneena-field-test
  (let [field-for-tunnistautunut {:id "kysymys" :params {:show-only-for-identified true}}]
    (testing "field is invisible when unidentified:"
      (is (= {:kysymys {:visible? false}}
             (ui-of
               (field-visibility/set-field-visibility {} field-for-tunnistautunut true false false [])))))
    (testing "field is visible when identified:"
      (is (= {:kysymys {:visible? true}}
             (ui-of
               (field-visibility/set-field-visibility {} field-for-tunnistautunut true false true [])))))))

(deftest set-field-visibility-for-options-test
  (testing "single option:"
    (is (= {:application {:answers {:kysymys {:value "0"}}
                          :ui      {:kysymys {0         {:visible? true}
                                              :visible? true}}}}
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "0"}}}}
                                                  {:id      "kysymys"
                                                   :options [{:value "0"}]}))))
  (testing "multiple options:"
    (is (= {:kysymys {0         {:visible? true}
                      1         {:visible? true}
                      :visible? true}}
           (ui-of
             (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "1"}}}}
                                                    {:id      "kysymys"
                                                     :options [{:value "0"}
                                                               {:value "1"}]}))))))

(deftest set-field-visibility-for-option-followups-test
  (testing "single option, option selected, single followup: relevant field types:"
    (are [field-type]
      (= {:kysymys      {0         {:visible? true}
                         :visible? true}
          :jatkokysymys {:visible? true}}
         (ui-of
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "0"}}}}
                                                  {:id        "kysymys"
                                                   :fieldType field-type
                                                   :options   [{:value     "0"
                                                                :followups [{:id "jatkokysymys"}]}]})))
      "dropdown"
      "multipleChoice"
      "singleChoice"))
  (testing "multiple options, option not selected, single followup:"
    (is (= {:kysymys      {0         {:visible? true}
                           1         {:visible? true}
                           :visible? true}
            :jatkokysymys {:visible? false}}
           (ui-of
             (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "0"}}}}
                                                    {:id        "kysymys"
                                                     :fieldType "singleChoice"
                                                     :options   [{:value "0"}
                                                                 {:value     "1"
                                                                  :followups [{:id "jatkokysymys"}]}]})))))
  (testing "single option, option selected, multiple followups: relevant field types:"
    (are [fieldType]
      (= {:kysymys        {0         {:visible? true}
                           :visible? true}
          :jatkokysymys-1 {:visible? true}
          :jatkokysymys-2 {:visible? true}}
         (ui-of
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "0"}}}}
                                                  {:id        "kysymys"
                                                   :fieldType fieldType
                                                   :options   [{:value     "0"
                                                                :followups [{:id "jatkokysymys-1"}
                                                                            {:id "jatkokysymys-2"}]}]})))
      "dropdown"
      "multipleChoice"
      "singleChoice"))
  (testing "option selected, followup visibility for selected hakukohde:"
    (are [fieldType selected belongs-to visible?]
      (= {:kysymys-id      {0         {:visible? true}
                            :visible? true}
          :jatkokysymys-id {:visible? visible?}}
         (ui-of
           (field-visibility/set-field-visibility
             {:application {:answers {:kysymys-id  {:value "0"}
                                      :hakukohteet {:value [selected]}}}}
             {:id        "kysymys-id"
              :fieldType fieldType
              :options   [{:value     "0"
                           :followups [{:id                     "jatkokysymys-id"
                                        :belongs-to-hakukohteet [belongs-to]}]}]})))
      "dropdown" "valittu-hakukohde-id" "hakukohde-id" false
      "dropdown" "hakukohde-id" "hakukohde-id" true
      "multipleChoice" "valittu-hakukohde-id" "hakukohde-id" false
      "multipleChoice" "hakukohde-id" "hakukohde-id" true
      "singleChoice" "valittu-hakukohde-id" "hakukohde-id" false
      "singleChoice" "hakukohde-id" "hakukohde-id" true)))

(deftest set-field-visibility-for-text-field-test
  (testing "text field: single option, empty followup:"
    (is
      (= {:kysymys {0         {:visible? true}
                    :visible? true}}
         (ui-of
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "Vastaus"}}}}
                                                  {:id        "kysymys"
                                                   :fieldType "textField"
                                                   :options   [{:value "0"}]})))))

  (testing "text field: single hidden option, empty followup:"
    (is
      (= {:kysymys {0         {:visible? false}
                    :visible? true}}
         (ui-of
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "Vastaus"}}}}
                                                  {:id        "kysymys"
                                                   :fieldType "textField"
                                                   :options   [{:value "0" :hidden true}]})))))

  (testing "text field: single option, single followup:"
    (is
      (= {:kysymys      {0         {:visible? true}
                         :visible? true}
          :jatkokysymys {:visible? true}}
         (ui-of
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "Vastaus"}}}}
                                                  {:id        "kysymys"
                                                   :fieldType "textField"
                                                   :options   [{:value     "0"
                                                                :followups [{:id "jatkokysymys"}]}]})))))

  (testing "text field: multiple options, single followup each:"
    (is
      (= {:kysymys        {0         {:visible? true}
                           1         {:visible? true}
                           :visible? true}
          :jatkokysymys-0 {:visible? true}
          :jatkokysymys-1 {:visible? true}}
         (ui-of
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value "Vastaus"}}}}
                                                  {:id        "kysymys"
                                                   :fieldType "textField"
                                                   :options   [{:value     "0"
                                                                :followups [{:id "jatkokysymys-0"}]}
                                                               {:value     "1"
                                                                :followups [{:id "jatkokysymys-1"}]}]})))))

  (testing "text field: empty answer, followup should not be visible:"
    (is
      (= {:kysymys      {0         {:visible? true}
                         :visible? true}
          :jatkokysymys {:visible? false}}
         (ui-of
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value ""}}}}
                                                  {:id        "kysymys"
                                                   :fieldType "textField"
                                                   :options   [{:value     "0"
                                                                :followups [{:id "jatkokysymys"}]}]})))))

  (testing "text field: option with condition:"
    (are [answer comparison-operator answer-compared-to option-visible? followup-visible?]
      (= {:kysymys      {0         {:visible? true}
                         :visible? option-visible?}
          :jatkokysymys {:visible? followup-visible?}}
         (ui-of
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value (str answer)}}}}
                                                  {:id        "kysymys"
                                                   :fieldType "textField"
                                                   :options   [{:value     "0"
                                                                :condition {:answer-compared-to  answer-compared-to
                                                                            :comparison-operator comparison-operator}
                                                                :followups [{:id "jatkokysymys"}]}]})))
      12 "=" 11 true false
      12 "=" 12 true true))

  (testing "text field: multiple options with condition"
    (are [answer-value visibility-0 visibility-1]
      (= {:kysymys        {0         {:visible? true}
                           1         {:visible? true}
                           :visible? true}
          :jatkokysymys-0 {:visible? visibility-0}
          :jatkokysymys-1 {:visible? visibility-1}}
         (ui-of
           (field-visibility/set-field-visibility {:application {:answers {:kysymys {:value answer-value}}}}
                                                  {:id        "kysymys"
                                                   :fieldType "textField"
                                                   :options   [{:value     "0"
                                                                :condition {:answer-compared-to  0
                                                                            :comparison-operator "<"}
                                                                :followups [{:id "jatkokysymys-0"}]}
                                                               {:value     "1"
                                                                :condition {:answer-compared-to  2
                                                                            :comparison-operator ">"}
                                                                :followups [{:id "jatkokysymys-1"}]}]})))
      "-1" true false
      "1" false false
      "3" false true))

  (testing "text field: followup visibility for selected hakukohde:"
    (are [selected belongs-to visible?]
      (= {:kysymys-id      {0         {:visible? true}
                            :visible? true}
          :jatkokysymys-id {:visible? visible?}}
         (ui-of
           (field-visibility/set-field-visibility
             {:application {:answers {:kysymys-id  {:value "Vastaus"}
                                      :hakukohteet {:value [selected]}}}}
             {:id        "kysymys-id"
              :fieldType "textField"
              :options   [{:value     "0"
                           :followups [{:id                     "jatkokysymys-id"
                                        :belongs-to-hakukohteet [belongs-to]}]}]})))
      "valittu-hakukohde-id" "hakukohde-id" false
      "hakukohde-id" "hakukohde-id" true)))

