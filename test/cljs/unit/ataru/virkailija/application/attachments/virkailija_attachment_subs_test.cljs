(ns ataru.virkailija.application.attachments.virkailija-attachment-subs-test
  (:require [cljs.test :refer-macros [deftest is]]
            [ataru.virkailija.application.attachments.virkailija-attachment-subs :refer [liitepyynnot-for-selected-hakukohteet
                                                                                        liitepyynnot-hakemuksen-hakutoiveille]]
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
                                                      :attachment2 "attachment-missing"
                                                      :attachment1 "checked"}}
        expected-value [{:key           :attachment1,
                         :state         "checked",
                         :values        "value1",
                         :label         {:fi "FI: Liite 1"},
                         :hakukohde-oid "hakukohde_oid"}
                        {:key           :attachment2,
                         :state         "attachment-missing",
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

(deftest extracts-attachments-from-application
  (let [form {:key "1234"}
        forms {"1234" {:flat-form-fields [{:id "liite-vastaus" :params {:mail-attachment? true :attachment-type "lt-1"}}]}}
        hakukohteet {"4321" {:oid "4321" :liitteet [{:toimitetaan-erikseen true :tyyppi "lt-1"}]}}
        answers {"liite-vastaus" {:key "liite-vastaus" :original-followup nil :original-question nil :duplikoitu-kysymys-hakukohde-oid nil :duplikoitu-followup-hakukohde-oid nil}}
        result (liitepyynnot-hakemuksen-hakutoiveille
                 [["4321"] hakukohteet [] answers form forms])
        expected-result {"lt-1" [{:toimitetaan-erikseen true,
                                  :tyyppi "lt-1",
                                  :hakukohde {:oid "4321", :name nil, :tarjoaja nil},
                                  :tyyppi-label "lt-1"}]}]
    (is (= result expected-result))))

(deftest extracts-no-attachments-from-application-with-no-attachment-answers
  (let [form {:key "1234"}
        forms {"1234" {:flat-form-fields [{:id "liite-vastaus" :params {:mail-attachment? true :attachment-type "lt-1"}}
                                          {:id "joku-vastaus"}]}}
        hakukohteet {"4321" {:oid "4321" :liitteet [{:toimitetaan-erikseen true :tyyppi "lt-1"}]}}
        answers {"joku-vastaus" {:key "joku-vastaus" :original-followup nil :original-question nil :duplikoitu-kysymys-hakukohde-oid nil :duplikoitu-followup-hakukohde-oid nil}}
        result (liitepyynnot-hakemuksen-hakutoiveille
                 [["4321"] hakukohteet [] answers form forms])
        expected-result {}]
    (is (= result expected-result))))

(deftest extracts-attachments-from-application-with-duplicates
  (let [form {:key "1234"}
        forms {"1234" {:flat-form-fields [{:id "liite-vastaus" :params {:mail-attachment? true :attachment-type "lt-1"}}]}}
        hakukohteet {"4321" {:oid "4321" :liitteet [{:toimitetaan-erikseen true :tyyppi "lt-1"}]}}
        answers {"liite-vastaus_4321" {:key "liite-vastaus_4321" :original-followup nil :original-question "liite-vastaus" :duplikoitu-kysymys-hakukohde-oid "4321" :duplikoitu-followup-hakukohde-oid nil}}
        result (liitepyynnot-hakemuksen-hakutoiveille
                 [["4321"] hakukohteet [] answers form forms])
        expected-result {"lt-1" [{:toimitetaan-erikseen true,
                                  :tyyppi "lt-1",
                                  :hakukohde {:oid "4321", :name nil, :tarjoaja nil},
                                  :tyyppi-label "lt-1"}]}]
    (is (= result expected-result))))

(deftest extracts-attachments-from-application-with-duplicate-followups
  (let [form {:key "1234"}
        forms {"1234" {:flat-form-fields [{:id "liite-vastaus" :params {:mail-attachment? true :attachment-type "lt-1"}}]}}
        hakukohteet {"4321" {:oid "4321" :liitteet [{:toimitetaan-erikseen true :tyyppi "lt-1"}]}}
        answers {"liite-vastaus_4321" {:key "liite-vastaus_4321" :original-followup "liite-vastaus" :original-question nil :duplikoitu-kysymys-hakukohde-oid nil :duplikoitu-followup-hakukohde-oid "4321"}}
        result (liitepyynnot-hakemuksen-hakutoiveille
                 [["4321"] hakukohteet [] answers form forms])
        expected-result {"lt-1" [{:toimitetaan-erikseen true,
                                  :tyyppi "lt-1",
                                  :hakukohde {:oid "4321", :name nil, :tarjoaja nil},
                                  :tyyppi-label "lt-1"}]}]
    (is (= result expected-result))))

(deftest extracts-multiple-attachments-from-application
  (let [form {:key "1234"}
        forms {"1234" {:flat-form-fields [{:id "liite-vastaus" :params {:mail-attachment? true :attachment-type "lt-1"}}
                                          {:id "liite-vastaus2" :params {:mail-attachment? true :attachment-type "lt-2"}}]}}
        hakukohteet {"4321" {:oid "4321" :liitteet [{:toimitetaan-erikseen true :tyyppi "lt-1"}]}
                     "5432" {:oid "5432" :liitteet [{:toimitetaan-erikseen true :tyyppi "lt-1"}]}
                     "6543" {:oid "6543" :liitteet [{:toimitetaan-erikseen true :tyyppi "lt-2"}]}}
        answers {"liite-vastaus_4321" {:key "liite-vastaus_4321" :original-followup "liite-vastaus" :original-question nil :duplikoitu-kysymys-hakukohde-oid nil :duplikoitu-followup-hakukohde-oid "4321"}
                 "liite-vastaus_5432" {:key "liite-vastaus_5432" :original-followup "liite-vastaus" :original-question nil :duplikoitu-kysymys-hakukohde-oid nil :duplikoitu-followup-hakukohde-oid "5432"}
                 "liite-vastaus2" {:key "liite-vastaus2" :original-followup nil :original-question nil :duplikoitu-kysymys-hakukohde-oid nil :duplikoitu-followup-hakukohde-oid nil}}
        result (liitepyynnot-hakemuksen-hakutoiveille
                 [["4321" "5432" "6543" "7654"] hakukohteet [] answers form forms])
        expected-result {"lt-1" [{:toimitetaan-erikseen true,
                                  :tyyppi "lt-1",
                                  :hakukohde {:oid "4321", :name nil, :tarjoaja nil},
                                  :tyyppi-label "lt-1"}
                                 {:toimitetaan-erikseen true,
                                  :tyyppi "lt-1",
                                  :hakukohde {:oid "5432", :name nil, :tarjoaja nil},
                                  :tyyppi-label "lt-1"}],
                         "lt-2" [{:toimitetaan-erikseen true,
                                  :tyyppi "lt-2",
                                  :hakukohde {:oid "6543", :name nil, :tarjoaja nil},
                                  :tyyppi-label "lt-2"}]}]
    (is (= result expected-result))))