(ns ataru.tutkinto.tutkinto-util-spec
  (:require [speclj.core :refer :all]
            [cheshire.core :as json]
            [ataru.tutkinto.tutkinto-util :as tutkinto-util]))

(defn- read-application-json [file-name]
  (:application (json/parse-string (slurp (str "dev-resources/koski/" file-name)) true)))

(def answers {:amm-perus-tutkinto-id {:valid true,
                                      :label {:fi "Ammatilliset perustutkinnot",
                                              :sv "SV: Ammatilliset perustutkinnot",
                                              :en ""},
                                      :value [[""]],
                                      :values [[{:value "", :valid true}]],
                                      :original-value [[""]]},
              :lukiokoulutus-tutkinto-id {:valid true,
                                          :label {:fi "Lukiokoulutus", :sv "SV: Lukiokoulutus", :en ""},
                                          :value [[""]],
                                          :values [[{:value "", :valid true}]],
                                          :original-value [[""]]}
              :kk-alemmat-tutkinto-id {:valid true, :label {:fi "Alemmat korkeakoulututkinnot",
                                                            :sv "SV: Alemmat korkeakoulututkinnot",
                                                            :en ""},
                                       :value [["1.2.246.562.10.78305677532_623404_2010-09-20"]],
                                       :values [[{:value "1.2.246.562.10.78305677532_623404_2010-09-20", :valid true}]],
                                       :original-value [[""]], :errors []}})

(def flat-form-content [{:label {:fi "Tutkinnot", :sv "SV: Tutkinnot", :en ""},
                         :tutkinnot {:description {:fi "Hakijalle tuodaan....", :sv "SV: Hakijalle tuodaan...", :en ""},
                                     :field-list {:fi "Tutkinto, Koulutusohjelma...", :sv "SV:", :en ""}},
                         :fieldClass "wrapperElement",
                         :id "koski-tutkinnot-wrapper",
                         :params {}, :metadata {:created-by {:name "Tepi testaaja",
                                                             :oid "1.2.246.562.24.12345678901",
                                                             :date "2025-01-15T09:35:54Z"},
                                                :modified-by {:name "Tepi testaaja",
                                                              :oid "1.2.246.562.24.12345678901",
                                                              :date "2025-01-15T09:35:54Z"}},
                         :fieldType "tutkinnot"}
                        {:description {:fi "Valitse tutkintotasot...", :sv "SV:", :en ""},
                         :category "tutkinto-properties",
                         :children-of "koski-tutkinnot-wrapper",
                         :fieldClass "formPropertyField",
                         :label {:fi "Tutkintotasot", :sv "SV: Tutkintotasot", :en ""},
                         :id "58d4d1f0-26a4-4d35-82ea-7dff06ae5b0d", :exclude-from-answers true,
                         :options [{:allow-user-followups false, :label {:fi "Ammatilliset perustutkinnot", :sv "SV:", :en ""},
                                    :id "amm-perus",
                                    :followup-label {:fi "Lisäkysymykset Koskesta...", :sv "SV:", :en ""}}
                                   {:allow-user-followups false, :label {:fi "Lukiokoulutus", :sv "SV: Lukiokoulutus", :en ""},
                                    :id "lukiokoulutus",
                                    :followup-label {:fi "Lisäkysymykset Koskesta...", :sv "SV:", :en ""}}
                                   {:allow-user-followups false, :label {:fi "Alemmat korkeakoulututkinnot",
                                                                         :sv "SV: Alemmat korkeakoulututkinnot",
                                                                         :en ""},
                                    :id "kk-alemmat",
                                    :followup-label {:fi "Lisäkysymykset Koskesta...", :sv "SV:", :en ""}}
                                   ],
                         :metadata {:created-by {:name "Tepi testaaja",
                                                 :oid "1.2.246.562.24.12345678901",
                                                 :date "2025-01-15T09:35:54Z"},
                                    :modified-by {:name "Tepi testaaja",
                                                  :oid "1.2.246.562.24.12345678901",
                                                  :date "2025-01-15T09:35:54Z"}},
                         :fieldType "multipleOptions"}
                        {:params {}, :option-value nil, :fieldClass "questionGroup", :label {:fi "", :sv "", :en ""},
                         :id "amm-perus-question-group", :followup-of "58d4d1f0-26a4-4d35-82ea-7dff06ae5b0d",
                         :metadata {:created-by {:name "Tepi testaaja",
                                                 :oid "1.2.246.562.24.12345678901",
                                                 :date "2025-01-15T09:35:54Z"},
                                    :modified-by {:name "Tepi testaaja",
                                                  :oid "1.2.246.562.24.12345678901",
                                                  :date "2025-01-15T09:35:54Z"}},
                         :fieldType "embedded"}
                        {:children-of "amm-perus-question-group",
                         :params {:transparent true, :question-group-id :amm-perus-question-group},
                         :fieldClass "formField", :cannot-edit false,
                         :label {:fi "Ammatilliset perustutkinnot", :sv "SV: Ammatilliset perustutkinnot", :en ""},
                         :id "amm-perus-tutkinto-id", :cannot-view false, :fieldType "textField"}
                        {:params {}, :option-value nil, :fieldClass "questionGroup", :label {:fi "", :sv "", :en ""},
                         :id "lukiokoulutus-question-group", :followup-of "58d4d1f0-26a4-4d35-82ea-7dff06ae5b0d",
                         :metadata {:created-by {:name "Tepi testaaja",
                                                 :oid "1.2.246.562.24.12345678901",
                                                 :date "2025-01-15T09:35:54Z"},
                                    :modified-by {:name "Tepi testaaja",
                                                  :oid "1.2.246.562.24.12345678901",
                                                  :date "2025-01-15T09:35:54Z"}},
                         :fieldType "embedded"}
                        {:children-of "lukiokoulutus-question-group",
                         :params {:transparent true, :question-group-id :lukiokoulutus-question-group},
                         :fieldClass "formField", :cannot-edit false,
                         :label {:fi "Lukiokoulutus", :sv "SV: Lukiokoulutus", :en ""},
                         :id "lukiokoulutus-tutkinto-id", :cannot-view false, :fieldType "textField"}
                        {:children-of "lukiokoulutus-question-group",
                         :params {:question-group-id :lukiokoulutus-question-group}, :validators ["required"],
                         :fieldClass "formField", :cannot-edit false, :label {:fi "Miten menee??", :sv ""},
                         :id "additional-field-for-lukiokoulutus", :cannot-view false,
                         :fieldType "textField"}
                        {:params {}, :option-value nil, :fieldClass "questionGroup", :label {:fi "", :sv "", :en ""},
                         :id "kk-alemmat-question-group", :followup-of "58d4d1f0-26a4-4d35-82ea-7dff06ae5b0d",
                         :metadata {:created-by {:name "Tepi testaaja",
                                                 :oid "1.2.246.562.24.12345678901",
                                                 :date "2025-01-15T09:35:54Z"},
                                    :modified-by {:name "Tepi testaaja",
                                                  :oid "1.2.246.562.24.12345678901",
                                                  :date "2025-01-15T09:35:54Z"}},
                         :fieldType "embedded"}
                        {:children-of "kk-alemmat-question-group",
                         :params {:transparent true, :question-group-id :kk-alemmat-question-group},
                         :fieldClass "formField", :cannot-edit false, :label {:fi "Alemmat kk", :sv "SV:", :en ""},
                         :id "kk-alemmat-tutkinto-id", :cannot-view false, :fieldType "textField"}
                        {:children-of "kk-alemmat-question-group",
                         :params {:question-group-id :kk-alemmat-question-group}, :validators ["required"],
                         :fieldClass "formField", :cannot-edit false, :label {:fi "Miten menee??", :sv ""},
                         :id "additional-field-for-kk-alemmat", :cannot-view false,
                         :fieldType "textField"}
                        ])
(describe "Finding tutkinto-data from application"
          (tags :unit)
          (it "should detect tutkinto id fields in application"
              (should= true (tutkinto-util/koski-tutkinnot-in-application? (read-application-json "application-with-koski-tutkinnot.json")))
              ))

(describe "Finding tutkinto-levels without any selections"
          (tags :unit)
          (it "should detect field related to level having some selections"
              (should= false (tutkinto-util/koski-tutkinto-field-without-selections?
                               {:id "additional-field-for-kk-alemmat"}
                               flat-form-content
                               answers
                               ["amm-perus" "lukiokoulutus" "kk-alemmat"])))
          (it "should detect field related to level having no selections"
              (should= true (tutkinto-util/koski-tutkinto-field-without-selections?
                               {:id "additional-field-for-lukiokoulutus"}
                               flat-form-content
                               answers
                               ["amm-perus" "lukiokoulutus" "kk-alemmat"]))))
