(ns ataru.fixtures.excel-fixtures
  (:require [ataru.component-data.component :as component]
            [clj-time.core :as c]))

(def form {:id           123
           :key          "form_123_key"
           :name         {:fi "Form name"}
           :created-by   "SEPPO PAPUNEN"
           :created-time (c/date-time 2016 6 14 12 34 56)
           :content      [{:id       "form_field_0"
                           :label    {:fi "Lomakeosio"}
                           :fieldType "fieldset"
                           :fieldClass "wrapperElement"
                           :children [{:id         "form_field_0_0"
                                       :label      {:fi "Kysymys 1"}
                                       :fieldType  "textField"
                                       :fieldClass "formField"}
                                      {:id         "form_field_0_1"
                                       :label      {:fi "Kysymys 2"}
                                       :fieldType  "textField"
                                       :fieldClass "formField"}]}]})

(def form-for-hakukohde {:id           321
                         :key          "form_321_key"
                         :name         {:fi "Form name"}
                         :created-by   "IRMELI KUIKELOINEN"
                         :created-time (c/date-time 2016 6 14 12 34 56)
                         :content      [{:id       "form_field_1"
                                         :label    {:fi "Lomakeosio"}
                                         :fieldType "fieldset"
                                         :fieldClass "wrapperElement"
                                         :children [{:id         "form_field_1_0"
                                                     :label      {:fi "Kysymys 4"}
                                                     :fieldType  "textField"
                                                     :fieldClass "formField"}
                                                    {:id                   "should_not_be_shown"
                                                     :label                {:fi "You should not see this"}
                                                     :fieldType            "textField"
                                                     :exclude-from-answers true
                                                     :fieldClass           "formField"}]}]})

(def form-for-multiple-hakukohde {:id           321
                                  :key          "form_321_key"
                                  :name         {:fi "Form name"}
                                  :created-by   "IRMELI KUIKELOINEN"
                                  :created-time (c/date-time 2016 6 14 12 34 56)
                                  :content      [{:id       "form_field_1"
                                                  :label    {:fi "Lomakeosio"}
                                                  :fieldType "fieldset"
                                                  :fieldClass "wrapperElement"
                                                  :children [{:id         "form_field_1_0"
                                                              :label      {:fi "Kysymys 4"}
                                                              :belongs-to-hakukohteet ["other.hakukohde.oid"]
                                                              :fieldType  "textField"
                                                              :fieldClass "formField"
                                                              :children [{:id         "form_field_1_0_0"
                                                                          :label      {:fi "Question is not visible because its parent is from other hakukohde"}
                                                                          :fieldType  "textField"
                                                                          :fieldClass "formField"
                                                                          :children [{:id         "form_field_1_0_0_0"
                                                                                      :label      {:fi "Question is not visible because its grand-parent is from other hakukohde"}
                                                                                      :belongs-to-hakukohteet ["hakukohde.oid"]
                                                                                      :fieldType  "textField"
                                                                                      :fieldClass "formField"}]}]}

                                                             {:id                   "should_not_be_shown"
                                                              :label                {:fi "You should not see this because of exclude-from-answers"}
                                                              :fieldType            "textField"
                                                              :exclude-from-answers true
                                                              :belongs-to-hakukohteet ["hakukohde.oid"]
                                                              :fieldClass           "formField"}]}
                                                 {:id         "form_field_2"
                                                  :label      {:fi "Visible from form"}
                                                  :belongs-to-hakukohteet ["hakukohde.oid"]
                                                  :fieldType  "textField"
                                                  :fieldClass "formField"}
                                                 {:id         "form_field_2a"
                                                  :label      {:fi "Visible from form 2"}
                                                  :belongs-to-hakukohteet ["hakukohde-in-ryhma.oid"]
                                                  :fieldType  "textField"
                                                  :fieldClass "formField"}
                                                 {:id         "my-id"
                                                  :label      {:fi "Visible only if belongs to hakukohderyhmä1"}
                                                  :belongs-to-hakukohderyhma ["1.2.246.562.28.00000000001"]
                                                  :fieldType  "textField"
                                                  :fieldClass "formField"
                                                  :options [{:belongs-to-hakukohderyhma []
                                                             :fieldClass "formField"
                                                             :label {:fi "Minulla on seuraava dokumentti:"}
                                                             :id "3e73258f-ef94-46b4-a3da-00bf92efb1e8"
                                                             :followups [{:id        "form_field_X_0"
                                                                          :label     {:fi "Visible because of parent's hakukohderyhmä"}
                                                                          :fieldType  "textField"
                                                                          :fieldClass "formField"}]}]}
                                                 {:id         "form_field_3"
                                                  :label      {:fi "Should be visible because belongs-to-hakukohde is not specified"}
                                                  :fieldType  "textField"
                                                  :fieldClass "formField"}]})

(def form-with-special-questions {:id           321
                                  :key          "form_321_key"
                                  :name         {:fi "Form name"}
                                  :created-by   "IRMELI KUIKELOINEN"
                                  :created-time (c/date-time 2016 6 14 12 34 56)
                                  :content      [{:id         "form_field_1"
                                                  :label      {:fi "Lomakeosio"}
                                                  :fieldType  "fieldset"
                                                  :fieldClass "wrapperElement"
                                                  :children   [{:id         "form_field_1_0"
                                                                :label      {:fi "Kysymys 4"}
                                                                :fieldType  "textField"
                                                                :fieldClass "formField"}
                                                               {:id                   "should_not_be_shown"
                                                                :label                {:fi "You should not see this"}
                                                                :fieldType            "textField"
                                                                :exclude-from-answers true
                                                                :fieldClass           "formField"}]}
                                                 (component/valintatuloksen-julkaisulupa {})]})

(def application-for-form {:id                            9432
                           :key                           "application_9432_key"
                           :created-time                  (c/date-time 2016 6 15 12 34 56)
                           :submitted                     (c/date-time 2016 6 15 12 30 00)
                           :state                         "active"
                           :form                          123
                           :name                          {:fi "Standalone form"}
                           :lang                          "fi"
                           :person                        {:turvakielto false}
                           :application-hakukohde-reviews [{:requirement "language-requirement" :state "unreviewed" :hakukohde "form"}
                                                           {:requirement "degree-requirement" :state "fulfilled" :hakukohde "form"}
                                                           {:requirement "eligibility-state" :state "uneligible" :hakukohde "form"}]
                           :answers                       [{:key   "first-name"
                                                            :label {:fi "Etunimi"}
                                                            :value "Lomake-etunimi"}
                                                           {:key       "form_field_0_0"
                                                            :label     {:fi "Kysymys 1"}
                                                            :value     "Vastaus 1"
                                                            :fieldType "textfield"}
                                                           {:key       "form_field_0_1"
                                                            :label     {:fi "Kysymys 2"}
                                                            :value     "Vastaus 2"
                                                            :fieldType "textfield"}
                                                           {:key       "kysymys_3"
                                                            :label     {:fi "Kysymys 3"}
                                                            :value     "Vastaus 3"
                                                            :fieldType "textfield"}]})

(def application-for-hakukohde {:id                            3424
                                :key                           "application_3424_key"
                                :created-time                  (c/date-time 2016 6 15 12 34 56)
                                :submitted                     (c/date-time 2016 6 15 12 30 00)
                                :state                         "active"
                                :form                          321
                                :name                          {:fi "Form with hakukohde and haku"}
                                :lang                          "fi"
                                :hakukohde                     ["hakukohde.oid"]
                                :haku                          "haku.oid"
                                :person-oid                    "1.123.345456567123"
                                :person                        {:turvakielto true
                                                                :first-name  "Person-etunimi"}
                                :application-hakukohde-reviews [{:requirement "selection-state" :state "selected" :hakukohde "hakukohde.oid"}
                                                                {:requirement "processing-state" :state "processing" :hakukohde "hakukohde.oid"}]
                                :kk-payment-state              "paid"
                                :answers                       [{:key       "first-name"
                                                                 :label     {:fi "Etunimi"}
                                                                 :value     "Lomake-etunimi"}
                                                                {:key       "form_field_1_0"
                                                                 :label     {:fi "Kysymys 4"}
                                                                 :value     "Vastaus 4"
                                                                 :fieldType "textfield"}
                                                                {:key       "kysymys_5"
                                                                 :label     {:fi "Kysymys 5"}
                                                                 :value     "Vastaus 5"
                                                                 :fieldType "textfield"}
                                                                {:key       "should_not_be_shown"
                                                                 :label     {:fi "You should not see this"}
                                                                 :value     "Really, no"
                                                                 :fieldType "textField"}]})

(def application-with-special-answers {:id                            3424
                                       :key                           "application_3424_key"
                                       :created-time                  (c/date-time 2016 6 15 12 34 56)
                                       :submitted                     (c/date-time 2016 6 15 12 30 00)
                                       :state                         "active"
                                       :form                          321
                                       :name                          {:fi "Form with hakukohde and haku"}
                                       :lang                          "fi"
                                       :hakukohde                     ["hakukohde.oid"]
                                       :haku                          "haku.oid"
                                       :person-oid                    "1.123.345456567123"
                                       :person                        {:turvakielto true
                                                                       :first-name  "Person-etunimi"}
                                       :application-hakukohde-reviews [{:requirement "selection-state" :state "selected" :hakukohde "hakukohde.oid"}
                                                                       {:requirement "processing-state" :state "processing" :hakukohde "hakukohde.oid"}]
                                       :kk-payment-state              "ok-by-proxy"
                                       :answers                       [{:key   "first-name"
                                                                        :label {:fi "Etunimi"}
                                                                        :value "Lomake-etunimi"}
                                                                       {:key       "form_field_1_0"
                                                                        :label     {:fi "Kysymys 4"}
                                                                        :value     "Vastaus 4"
                                                                        :fieldType "textfield"}
                                                                       {:key       "random_0"
                                                                        :label     {:fi "Kysymys 5"}
                                                                        :value     "Vastaus 5 will be included only when skip-answers? == false"
                                                                        :fieldType "textfield"}
                                                                       {:key       "valintatuloksen-julkaisulupa"
                                                                        :label     (:label (component/valintatuloksen-julkaisulupa {}))
                                                                        :value     "Ei"
                                                                        :fieldType "singleChoice"}]})

(def application-with-special-answers-2 (merge application-with-special-answers
                                               {:hakukohde ["hakukohde-in-ryhma.oid"]
                                                :haku "haku-2.oid"
                                                :application-hakukohde-reviews
                                                [{:requirement "selection-state" :state "selected" :hakukohde "hakukohde-in-ryhma.oid"}
                                                 {:requirement "processing-state" :state "processing" :hakukohde "hakukohde-in-ryhma.oid"}]}))

(def application-review {:id              1
                         :application_key "c58df586-fdb9-4ee1-b4c4-030d4cfe9f81"
                         :state           "active"
                         :score           12})

(def application-review-notes
  {"application_3424_key" [{:id              1
                            :application-key "application_3424_key"
                            :created-time    (c/date-time 2018 7 29 14 11 12)
                            :notes           "Asia kunnossa"
                            :hakukohde       nil
                            :state-name      nil
                            :first-name      "Virk"
                            :last-name       "Ailija"}
                           {:id              2
                            :application-key "application_3424_key"
                            :created-time    (c/date-time 2018 7 30 15 12 13)
                            :notes           "Muikkari"
                            :hakukohde       nil
                            :state-name      nil
                            :first-name      "Ajilia"
                            :last-name       "Kriv"}]})
