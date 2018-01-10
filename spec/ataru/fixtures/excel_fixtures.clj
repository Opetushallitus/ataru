(ns ataru.fixtures.excel-fixtures
  (:require [clj-time.core :as c]))

(def form {:id           123
           :key          "form_123_key"
           :name         {:fi "Form name"}
           :created-by   "SEPPO PAPUNEN"
           :created-time (c/date-time 2016 6 14 12 34 56)
           :content      [{:id       "form_field_0"
                           :label    {:fi "Lomakeosio"}
                           :children [{:id         "form_field_0_0"
                                       :label      {:fi "Kysymys 1"}
                                       :fieldType  "textField"
                                       :fieldClass "formField"}
                                      {:children [{:id         "form_field_0_1"
                                                   :label      {:fi "Kysymys 2"}
                                                   :fieldType  "textField"
                                                   :fieldClass "formField"}]}]}]})

(def form-for-hakukohde {:id           321
                         :key          "form_321_key"
                         :name         {:fi "Form name"}
                         :created-by   "IRMELI KUIKELOINEN"
                         :created-time (c/date-time 2016 6 14 12 34 56)
                         :content      [{:id       "form_field_1"
                                         :label    {:fi "Lomakeosio"}
                                         :children [{:id         "form_field_1_0"
                                                     :label      {:fi "Kysymys 4"}
                                                     :fieldType  "textField"
                                                     :fieldClass "formField"}
                                                    {:id                   "should_not_be_shown"
                                                     :label                {:fi "You should not see this"}
                                                     :fieldType            "textField"
                                                     :exclude-from-answers true
                                                     :fieldClass           "formField"}]}]})

(def application-for-form {:id           9432
                           :key          "application_9432_key"
                           :created-time (c/date-time 2016 6 15 12 34 56)
                           :state        "active"
                           :form         123
                           :name         {:fi "Standalone form"}
                           :lang         "fi"
                           :application-hakukohde-reviews [{:requirement "language-requirement" :state "unreviewed" :hakukohde "form"}
                                                           {:requirement "degree-requirement" :state "fulfilled" :hakukohde "form"}
                                                           {:requirement "eligibility-state" :state "uneligible" :hakukohde "form"}]
                           :answers      [{:key       "form_field_0_0"
                                           :label     "Kysymys 1"
                                           :value     "Vastaus 1"
                                           :fieldType "textfield"}
                                          {:key       "form_field_0_1"
                                           :label     "Kysymys 2"
                                           :value     "Vastaus 2"
                                           :fieldType "textfield"}
                                          {:key       "random_0"
                                           :label     "Kysymys 3"
                                           :value     "Vastaus 3"
                                           :fieldType "textfield"}]})

(def application-for-hakukohde {:id             3424
                                :key            "application_3424_key"
                                :created-time   (c/date-time 2016 6 15 12 34 56)
                                :state          "active"
                                :form           321
                                :name           {:fi "Form with hakukohde and haku"}
                                :lang           "fi"
                                :hakukohde      ["hakukohde.oid"]
                                :haku           "haku.oid"
                                :person-oid     "1.123.345456567123"
                                :application-hakukohde-reviews [{:requirement "selection-state" :state "selected" :hakukohde "hakukohde.oid"}
                                                                {:requirement "processing-state" :state "processing" :hakukohde "hakukohde.oid"}]
                                :answers        [{:key       "form_field_1_0"
                                                  :label     "Kysymys 4"
                                                  :value     "Vastaus 4"
                                                  :fieldType "textfield"}
                                                 {:key       "random_0"
                                                  :label     "Kysymys 5"
                                                  :value     "Vastaus 5"
                                                  :fieldType "textfield"}
                                                 {:key       "should_not_be_shown"
                                                  :label     "You should not see this"
                                                  :value     "Really, no"
                                                  :fieldType "textField"}]})

(def application-review {:id              1
                         :application_key "c58df586-fdb9-4ee1-b4c4-030d4cfe9f81"
                         :state           "active"
                         :notes           [{:id    342
                                            :notes "Some notes about the applicant"}]})
