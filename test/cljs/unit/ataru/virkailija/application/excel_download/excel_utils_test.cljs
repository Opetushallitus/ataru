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

(deftest test-get-excel-checkbox-filter-defs
  (testing "Show top-level form field without children"
    (is (= {"asdf" {:id "asdf"
                    :index 0
                    :label nil
                    :child-ids ()
                    :checked true}}
           (get-excel-checkbox-filter-defs [{:id "asdf" :fieldClass "formField"}]
                                           stub-true))))
  (testing "Hide hidden field"
    (is (= {}
           (get-excel-checkbox-filter-defs [{:id "asdf"
                                             :fieldClass "formField"
                                             :hidden true}] stub-true))))
  (testing "Hide top-level question group with only one infoElement child"
    (is (= {}
           (get-excel-checkbox-filter-defs [{:id "asdf"
                                             :fieldClass "questionGroup"
                                             :children [{:id "qwer"
                                                         :fieldClass "infoElement"}]}]
                                           stub-true))))

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
                                           stub-true))))

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
                                           (create-belongs-to "hakukohde.oid.1" nil)))))
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
                                           (create-belongs-to nil "hakukohderyhma.oid.1"))))))