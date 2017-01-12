(ns ataru.fixtures.excel-fixtures
  (:require [clj-time.core :as c]))

(def form {:id           123
           :key          "form_123_key"
           :name         "Form name"
           :created-by   "SEPPO PAPUNEN"
           :created-time (c/date-time 2016 6 14 12 34 56)
           :content      [{:id       "form_field_0"
                           :label    {:fi "Lomakeosio"}
                           :children [{:id         "form_field_0_0"
                                       :label      {:fi "Kysymys 1"}
                                       :fieldType  "textField"
                                       :fieldClass "formField"}]}]})

(def form-for-hakukohde {:id           321
           :key          "form_321_key"
           :name         "Form name"
           :created-by   "IRMELI KUIKELOINEN"
           :created-time (c/date-time 2016 6 14 12 34 56)
           :content      [{:id       "form_field_1"
                           :label    {:fi "Lomakeosio"}
                           :children [{:id         "form_field_1_0"
                                       :label      {:fi "Kysymys 3"}
                                       :fieldType  "textField"
                                       :fieldClass "formField"}]}]})

(def application-for-form {:id           9432
                           :key          "application_9432_key"
                           :created-time (c/date-time 2016 6 15 12 34 56)
                           :state        "unprocessed"
                           :form         123
                           :name         "Standalone form"
                           :lang         "fi"
                           :answers      [{:key       "form_field_0_0"
                                           :label     "Kysymys 1"
                                           :value     "Vastaus 1"
                                           :fieldType "textfield"}
                                          {:key       "random_0"
                                           :label     "Kysymys 2"
                                           :value     "Vastaus 2"
                                           :fieldType "textfield"}]})

(def application-for-hakukohde {:id             3424
                                :key            "application_3424_key"
                                :created-time   (c/date-time 2016 6 15 12 34 56)
                                :state          "rejected"
                                :form           321
                                :name           "Form with hakukohde and haku"
                                :lang           "fi"
                                :hakukohde      "hakukohde.oid"
                                :hakukohde_name "Hakukohde name"
                                :haku           "haku.oid"
                                :haku_name      "Haku name"
                                :answers        [{:key       "form_field_1_0"
                                                  :label     "Kysymys 3"
                                                  :value     "Vastaus 3"
                                                  :fieldType "textfield"}
                                                 {:key       "random_0"
                                                  :label     "Kysymys 4"
                                                  :value     "Vastaus 4"
                                                  :fieldType "textfield"}]})

(def application-review {:id              1
                         :application_key "c58df586-fdb9-4ee1-b4c4-030d4cfe9f81"
                         :state           "unprocessed"
                         :notes           "Some notes about the applicant"})
