(ns ataru.tutkinto.tutkinto-util-spec
  (:require [ataru.util :as util]
            [speclj.core :refer :all]
            [cheshire.core :as json]
            [ataru.tutkinto.tutkinto-util :as tutkinto-util]
            [ataru.fixtures.form :as form-fixtures]))

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

(def itse-syotetty-tutkinto-nimi-answer {:valid true, :label {:fi "Tutkinto",
                                                              :sv "SV: Tutkinto",
                                                              :en ""},
                                         :value [["Supertutkinto"]],
                                         :values [[{:value "Supertutkinto", :valid true}]],
                                         :original-value [[""]], :errors []})

(def itse-syotetty-nongrouped-answer {:valid true, :label {:fi "Miten menee??", :sv "", :en ""},
                                      :value "ihan ok",
                                      :values {:value "ihan ok", :valid true},
                                      :original-value "", :errors []})

(def flat-form-content (util/flatten-form-fields (:content form-fixtures/tutkinto-test-form)))
(def flat-form-content-with-nongrouped-itsesyotetty
  (util/flatten-form-fields (:content (assoc-in form-fixtures/tutkinto-test-form [:content 0 :children 1 :options 9 :followups]
                              [{:params {},
                                :validators [],
                                :fieldClass "formField", :fieldType "textField", :cannot-edit false, :cannot-view false,
                                :label {:fi "Miten menee??", :sv ""},
                                :id "itse-syotetty-question-1"}
                               {:params {},
                                :validators [],
                                :fieldClass "formField", :fieldType "textField", :cannot-edit false, :cannot-view false,
                                :label {:fi "Miten menee huomenna??", :sv ""},
                                :id "itse-syotetty-question-2"}]))))
(defn find-from-flat-content [id flat-form-content]
  (some #(when (= id (:id %)) %) flat-form-content))

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

(describe "Resolving tutkinto-level selections"
          (tags :unit)
          (it "should detect selected koski-tutkinto level"
              (should= true
                       (tutkinto-util/tutkinto-option-selected nil
                                                               (find-from-flat-content "kk-alemmat-question-group" flat-form-content)
                                                               flat-form-content
                                                               answers)))
          (it "should ignore non-selected koski-tutkinto level"
              (should= false
                       (tutkinto-util/tutkinto-option-selected nil
                                                               (find-from-flat-content "perusopetus-question-group" flat-form-content)
                                                               flat-form-content
                                                               answers)))
          (it "should detect itse syötetty as selected when values in question group"
              (should= true
                       (tutkinto-util/tutkinto-option-selected nil
                                                               (find-from-flat-content "itse-syotetty-question-group" flat-form-content)
                                                               flat-form-content
                                                               (assoc answers :itse-syotetty-tutkinto-nimi itse-syotetty-tutkinto-nimi-answer))))
          (it "should detect itse syötetty as non-selected when no values in question group"
              (should= false
                       (tutkinto-util/tutkinto-option-selected nil
                                                               (find-from-flat-content "itse-syotetty-question-group" flat-form-content)
                                                               flat-form-content
                                                               answers)))
          (it "should detect itse syötetty as selected when non grouped values"
              (should= true
                       (tutkinto-util/tutkinto-option-selected nil
                                                               (find-from-flat-content "itse-syotetty-question-2" flat-form-content-with-nongrouped-itsesyotetty)
                                                               flat-form-content-with-nongrouped-itsesyotetty
                                                               (assoc answers :itse-syotetty-question-1 itse-syotetty-tutkinto-nimi-answer))))
          (it "should detect itse syötetty as non-selected when no values (neither non-grouped nor in question group)"
              (should= false
                       (tutkinto-util/tutkinto-option-selected nil
                                                               (find-from-flat-content "itse-syotetty-question-1" flat-form-content-with-nongrouped-itsesyotetty)
                                                               flat-form-content-with-nongrouped-itsesyotetty
                                                               answers))))