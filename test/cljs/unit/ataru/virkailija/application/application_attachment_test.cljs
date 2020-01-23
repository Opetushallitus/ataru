(ns ataru.virkailija.application.application-attachment-test
  (:require [cljs.test :refer-macros [deftest is]]
            [clojure.string :refer [includes?]]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.ratom :as r :refer-macros [reaction]]
            [ataru.virkailija.application.attachments.virkailija-attachment-subs :refer [liitepyynnot-for-selected-hakukohteet]]
            [ataru.util :as util]))

(def test-form-with-attachments-everywhere
  {:content [{:id              "d1"
              :fieldType       "dropdown"
              :koodisto-source {:uri     "maatjavaltiot2"
                                :version 1}
              :label           {:fi "FI: Suoritusmaa"}}
             {:id        "attachment1"
              :fieldType "attachment"
              :label     {:fi "FI: Liite 1"}}
             {:id         "ryhma-1"
              :fieldClass "questionGroup"
              :fieldType  "fieldset"
              :label      {:fi ""}
              :children   [{:id        "attachment2"
                            :fieldType "attachment"
                            :label     {:fi "FI: Liite 2"}}
                           {:id        "attachmentInvisible"
                            :fieldType "attachment"
                            :label     {:fi "FI: Liite invisible"}}]}
             {:id        "first-name"
              :fieldType "textField"
              :label     {:fi "FI: Etunimet"}}
             {:id        "last-name"
              :fieldType "textField"
              :label     {:fi "FI: Sukunimi"}}
             {:id        "attachment3"
              :fieldType "attachment"
              :label     {:fi "FI: Liite 3"}}]})

(def expected-form-attachment-fields
  [{:id        "attachment1",
    :fieldType "attachment",
    :label     {:fi "FI: Liite 1"}}
   {:id          "attachment2",
    :fieldType   "attachment",
    :label       {:fi "FI: Liite 2"},
    :children-of "ryhma-1"}
   {:id          "attachmentInvisible",
    :fieldType   "attachment",
    :label       {:fi "FI: Liite invisible"},
    :children-of "ryhma-1"}
   {:id        "attachment3",
    :fieldType "attachment",
    :label     {:fi "FI: Liite 3"}}])

(deftest extract-attachments-from-form
  (let [attachments (util/form-attachment-fields test-form-with-attachments-everywhere)]
    (is (= attachments expected-form-attachment-fields))))

(deftest liitepyynnot-for-selected-hakukohteet-are-in-correct-order
  (let [selected-hakukohde-oids ["hakukohde_oid"]
        form-attachment-fields expected-form-attachment-fields
        application {:answers
                     {:attachment1 {:key "attachment1" :values "value1"}
                      :attachment3 {:key "attachment3" :values "value3"}
                      :attachment2 {:key "attachment2" :values "value2"}}}
        liitepyynnot-for-hakukohteet {:hakukohde_oid {:attachment3 "checked"
                                                      :attachment2 "checked"
                                                      :attachment1 "checked"}}
        expected-value [{:key           :attachment1,
                         :state         "checked",
                         :values        "value1",
                         :label         {:fi "FI: Liite 1"},
                         :hakukohde-oid "hakukohde_oid"}
                        {:key           :attachment2,
                         :state         "checked",
                         :values        "value2",
                         :label         {:fi "FI: Liite 2"},
                         :hakukohde-oid "hakukohde_oid"}
                        {:key           :attachment3,
                         :state         "checked",
                         :values        "value3",
                         :label         {:fi "FI: Liite 3"},
                         :hakukohde-oid "hakukohde_oid"}]

        result (liitepyynnot-for-selected-hakukohteet
                 [selected-hakukohde-oids
                  form-attachment-fields
                  application
                  liitepyynnot-for-hakukohteet])]
    (is (= result expected-value))))
