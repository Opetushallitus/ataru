(ns ataru.virkailija.application.excel-download.excel-utils-test
  (:require [ataru.excel-common :refer [form-field-belongs-to-hakukohde]]
            [ataru.virkailija.application.excel-download.excel-utils :refer [get-excel-checkbox-filter-defs]]
            [cljs.test :refer-macros [deftest testing is]]))

(def all-hakukohteet {"hakukohde.oid.1" {:oid "hakukohde.oid.1"
                                         :ryhmaliitokset ["hakukohderyhma.oid.1"]}
                      "hakukohde.oid.2" {:oid "hakukohde.oid.2"
                                         :ryhmaliitokset []}})

(defn- create-belongs-to
  [hakukohde-oid hakukohderyhma-oid]
  (fn [form-field] (form-field-belongs-to-hakukohde form-field hakukohde-oid hakukohderyhma-oid (delay all-hakukohteet))))

(defn stub-true [& _] true)
(def form-properties {:tutkinto-properties {:selected-option-ids ["kk-alemmat", "itse-syotetty", "tohtori"]}})
(def tutkinto-wrapper [{:id "koski-tutkinnot-wrapper"
                        :fieldClass "wrapperElement"
                        :fieldType "tutkinnot"
                        :children [{:fieldClass  "formPropertyField"
                                    :fieldType   "multipleOptions"
                                    :category    "tutkinto-properties"
                                    :id          "tutkinto-properties"
                                    :exclude-from-answers  true
                                    :options [{:id "kk-alemmat"
                                               :followups [{:id "kk-alemmat-question-group"
                                                            :fieldClass "questionGroup"
                                                            :fieldType "embedded"
                                                            :children [{:id "kk-alemmat-tutkinto-id"
                                                                        :fieldClass "formField"
                                                                        :fieldType "textField"
                                                                        :params {:transparent true}}
                                                                       {:id "kk-alemmat-additional-text-field"
                                                                        :fieldClass "formField"
                                                                        :fieldType "textField"}]}]}
                                              {:id "itse-syotetty"
                                               :followups [{:id "itse-syotetty-question-group"
                                                            :fieldClass "questionGroup"
                                                            :fieldType "tutkintofieldset"
                                                            :children [{:id "itse-syotetty-additional-text-field1"
                                                                        :fieldClass "formField"
                                                                        :fieldType "textField"}
                                                                       {:id "itse-syotetty-additional-text-field2"
                                                                        :fieldClass "formField"
                                                                        :fieldType "textField"}]}]}]}]}])

(deftest test-get-excel-checkbox-filter-defs
  (testing "Show top-level form field without children"
    (is (= {"asdf" {:id "asdf"
                    :index 0
                    :label nil
                    :child-ids ()
                    :checked true}}
           (get-excel-checkbox-filter-defs [{:id "asdf" :fieldClass "formField"}]
                                           stub-true
                                           form-properties))))
  
  (testing "Hide hidden field"
    (is (= {}
           (get-excel-checkbox-filter-defs [{:id "asdf"
                                             :fieldClass "formField"
                                             :hidden true}] stub-true form-properties))))
  
  (testing "Hide top-level question group with only one infoElement child"
    (is (= {}
           (get-excel-checkbox-filter-defs [{:id "asdf"
                                             :fieldClass "questionGroup"
                                             :children [{:id "qwer"
                                                         :fieldClass "infoElement"}]}]
                                           stub-true
                                           form-properties))))

  (testing "Show top-level question group with form field child"
    (is (= {"asdf" {:id "asdf"
                    :index 0
                    :label nil
                    :checked true
                    :child-ids ["qwer"]}
            "qwer" {:id "qwer"
                    :index 1
                    :label nil
                    :checked true
                    :parent-id "asdf"
                    :child-ids []}}
           (get-excel-checkbox-filter-defs [{:id "asdf"
                                             :fieldClass "questionGroup"
                                             :children [{:id "qwer"
                                                         :fieldClass "formField"}]}]
                                           stub-true
                                           form-properties))))

  (testing "Hide top-level question which doesn't belong to hakukohde"
    (is (= {"eka" {:id "eka"
                   :index 0
                   :label nil
                   :child-ids ()
                   :checked true}}
           (get-excel-checkbox-filter-defs [{:id "eka"
                                             :belongs-to-hakukohteet ["hakukohde.oid.1"]
                                             :fieldClass "formField"}
                                            {:id "toka"
                                             :belongs-to-hakukohteet ["hakukohde.oid.2"]
                                             :fieldClass "formField"}]
                                           (create-belongs-to "hakukohde.oid.1" nil)
                                           form-properties))))
  
  (testing "Hide top-level question which doesn't belong to hakukohderyhma"
    (is (= {"eka" {:id "eka"
                   :index 0
                   :label nil
                   :child-ids ()
                   :checked true}}
           (get-excel-checkbox-filter-defs [{:id "eka"
                                             :belongs-to-hakukohderyhma ["hakukohderyhma.oid.1"]
                                             :fieldClass "formField"}
                                            {:id "toka"
                                             :belongs-to-hakukohderyhma ["hakukohderyhma.oid.unknown"]
                                             :fieldClass "formField"}]
                                           (create-belongs-to nil "hakukohderyhma.oid.1")
                                           form-properties))))

  (testing "Show tutkinto-fields"
    (is (= {"koski-tutkinnot-wrapper" {:id "koski-tutkinnot-wrapper"
                                       :index 0
                                       :label nil
                                       :checked true
                                       :child-ids ["kk-alemmat-tutkinto-id" "kk-alemmat-additional-text-field"
                                                   "itse-syotetty-additional-text-field1"
                                                   "itse-syotetty-additional-text-field2"]}
                                      "kk-alemmat-tutkinto-id"
                                             {:id "kk-alemmat-tutkinto-id"
                                              :index 2
                                              :label nil
                                              :checked true
                                              :parent-id "koski-tutkinnot-wrapper"
                                              :child-ids []}
                                      "kk-alemmat-additional-text-field"
                                             {:id "kk-alemmat-additional-text-field"
                                              :index 3
                                              :label nil
                                              :checked true
                                              :parent-id "koski-tutkinnot-wrapper"
                                              :child-ids []}
                                      "itse-syotetty-additional-text-field1"
                                             {:id "itse-syotetty-additional-text-field1"
                                              :index 4
                                              :label nil
                                              :checked true
                                              :parent-id "koski-tutkinnot-wrapper"
                                              :child-ids []}
                                      "itse-syotetty-additional-text-field2"
                                             {:id "itse-syotetty-additional-text-field2"
                                              :index 5
                                              :label nil
                                              :checked true
                                              :parent-id "koski-tutkinnot-wrapper"
                                              :child-ids []}}
           (get-excel-checkbox-filter-defs tutkinto-wrapper
                                           stub-true
                                           form-properties)))))