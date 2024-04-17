(ns ataru.hakija.application-test
  (:require [cljs.test :refer-macros [deftest is]]
            [ataru.hakija.application :refer [create-initial-answers
                                              answers->valid-status
                                              create-application-to-submit
                                              extract-wrapper-sections]]
            [ataru.util :as util]))
(def metadata {:created-by  {:oid  "1.2.246.562.24.1000000"
                             :date "2018-03-21T15:45:29.23+02:00"
                             :name "Teppo Testinen"}
               :modified-by {:oid  "1.2.246.562.24.1000000"
                             :date "2018-03-22T07:55:08Z"
                             :name "Teppo Testinen"}})

(def form1
  {:id 37,
   :name {:fi "uusi lomake"},
   :created-time "x",
   :created-by "1.2.246.562.11.11111111111",
   :content [{:id "G__1",
              :label {:fi "osio1", :sv "Avsnitt namn"},
              :children [{:id "G__2",
                          :label {:fi "kenttä1", :sv ""},
                          :params {:size "S"},
                          :validators ["required"]
                          :fieldType "textField",
                          :fieldClass "formField"
                          :metadata metadata}
                         {:id "G__14",
                          :label {:fi "kenttä2", :sv ""},
                          :params {:size "M"},
                          :validators ["some-validator"]
                          :fieldType "textField",
                          :fieldClass "formField"
                          :metadata metadata}],
              :fieldType "fieldset",
              :fieldClass "wrapperElement"
              :metadata metadata}
             {:id "G__25",
              :label {:fi "ulkokenttä", :sv ""},
              :params {:size "L"},
              :fieldType "textField",
              :fieldClass "formField"
              :metadata metadata}]})

(def person-info-form
  {:id 22,
   :name {:fi "Testilomake"},
   :created-by "1.2.246.562.11.11111111111",
   :created-time "2016-07-15T13:48:17.815+03:00",
   :content
   [{:fieldClass "wrapperElement"
                 :metadata metadata,
     :id "5febd7b0-75f0-462c-b9a4-6cac6a4bec88",
     :fieldType "fieldset",
     :children
                 [{:fieldClass "wrapperElement"
                               :metadata metadata,
                   :id "399d9123-f15f-402a-9ce9-2749d0578399",
                   :fieldType "rowcontainer",
                   :children
                               [{:label {:fi "Etunimet", :sv "Förnamn"},
                                 :fieldClass "formField"
                                        :metadata metadata,
                                 :id "380913e2-8c93-494c-bd86-57000ed50ae8",
                                 :params {},
                                 :validators ["required"]
                                 :fieldType "textField"}
                                {:label {:fi "Kutsumanimi", :sv "Smeknamn"},
                                 :fieldClass "formField"
                                        :metadata metadata,
                                 :id "7c8388f0-7ccb-4706-8630-15405b141552",
                                 :params {:size "S"},
                                 :validators ["required"]
                                 :fieldType "textField"}],
                   :params {}}
                  {:label {:fi "Sukunimi", :sv "Efternamn"},
                   :fieldClass "formField"
                          :metadata metadata,
                   :id "d2dc3e2e-c130-4fd4-8509-7c8fbf4d1c9e",
                   :params {},
                   :validators ["required"]
                   :fieldType "textField"}],
     :params {},
     :label {:fi "Henkilötiedot", :sv "Personlig information"},
     :module "person-info"}
    {:fieldClass "wrapperElement"
                 :metadata metadata,
     :fieldType "fieldset",
     :id "036a71bb-01dc-440e-8c05-80eea0ca9640",
     :label {:fi "Osion nimi", :sv "Avsnitt namn"},
     :children [{:fieldClass "formField"
                             :metadata metadata,
                   :fieldType "textField",
                   :label {:fi "Random question", :sv ""},
                   :id "839cb685-749a-46da-b215-842bc13ed542",
                   :params {}}],
     :params {}}]})

(defn form-with-language [lang]
  {:id 37,
   :name {:fi "uusi lomake"},
   :created-time "x",
   :created-by "1.2.246.562.11.11111111111",
   :selected-language lang,
   :content [{:id "language",
                          :label {:fi "kieli", :sv ""},
                          :params {},
                          :validators ["required"],
                          :fieldType "dropdown",
                          :fieldClass "formField",
                          :koodisto-source {:uri "kieli",
                                            :version 1,
                                            :default-option "suomi"}
                          :metadata metadata}]})

(def flat-form-with-question-group
  [{:id          "G__2",
    :label       {:fi "kenttä1", :sv ""},
    :params      {:size "S" :question-group-id "1234"},
    :validators  ["required"]
    :fieldType   "singleChoice",
    :options     [{:value "0",},
                  {:value "1",}],
    :fieldClass  "formField"
    :metadata    metadata}])

(deftest flattens-correctly
  (let [expected [{:id         "G__1",
                   :label      {:fi "osio1", :sv "Avsnitt namn"},
                   :fieldType  "fieldset",
                   :fieldClass "wrapperElement"
                   :metadata   metadata},
                  {:id          "G__2",
                   :label       {:fi "kenttä1", :sv ""},
                   :params      {:size "S"},
                   :validators  ["required"]
                   :fieldType   "textField",
                   :fieldClass  "formField"
                   :children-of "G__1"
                   :metadata    metadata}
                  {:id          "G__14",
                   :label       {:fi "kenttä2", :sv ""},
                   :params      {:size "M"},
                   :validators  ["some-validator"]
                   :fieldType   "textField",
                   :fieldClass  "formField"
                   :children-of "G__1"
                   :metadata    metadata}
                  {:id         "G__25",
                   :label      {:fi "ulkokenttä", :sv ""},
                   :params     {:size "L"},
                   :fieldType  "textField",
                   :fieldClass "formField"
                   :metadata   metadata}]
        actual   (util/flatten-form-fields (:content form1))]
    (is (= expected actual))))

(deftest flattens-row-container-answers
  (let [expected [{:fieldClass "wrapperElement"
                   :metadata   metadata,
                   :id         "5febd7b0-75f0-462c-b9a4-6cac6a4bec88",
                   :fieldType  "fieldset",
                   :params     {},
                   :label      {:fi "Henkilötiedot", :sv "Personlig information"},
                   :module     "person-info"},
                  {:fieldClass  "wrapperElement"
                   :metadata    metadata,
                   :id          "399d9123-f15f-402a-9ce9-2749d0578399",
                   :fieldType   "rowcontainer",
                   :children-of "5febd7b0-75f0-462c-b9a4-6cac6a4bec88"
                   :params      {}},
                  {:label       {:fi "Etunimet", :sv "Förnamn"},
                   :fieldClass  "formField"
                   :metadata    metadata,
                   :id          "380913e2-8c93-494c-bd86-57000ed50ae8",
                   :params      {},
                   :validators  ["required"]
                   :children-of "399d9123-f15f-402a-9ce9-2749d0578399"
                   :fieldType   "textField"}
                  {:label       {:fi "Kutsumanimi", :sv "Smeknamn"},
                   :fieldClass  "formField"
                   :metadata    metadata,
                   :id          "7c8388f0-7ccb-4706-8630-15405b141552",
                   :params      {:size "S"},
                   :validators  ["required"]
                   :children-of "399d9123-f15f-402a-9ce9-2749d0578399"
                   :fieldType   "textField"}
                  {:label       {:fi "Sukunimi", :sv "Efternamn"},
                   :fieldClass  "formField"
                   :metadata    metadata,
                   :id          "d2dc3e2e-c130-4fd4-8509-7c8fbf4d1c9e",
                   :params      {},
                   :validators  ["required"]
                   :children-of "5febd7b0-75f0-462c-b9a4-6cac6a4bec88"
                   :fieldType   "textField"}
                  {:fieldClass "wrapperElement"
                   :metadata   metadata,
                   :fieldType  "fieldset",
                   :id         "036a71bb-01dc-440e-8c05-80eea0ca9640",
                   :label      {:fi "Osion nimi", :sv "Avsnitt namn"},
                   :params     {}}
                  {:fieldClass  "formField"
                   :metadata    metadata,
                   :fieldType   "textField",
                   :label       {:fi "Random question", :sv ""},
                   :id          "839cb685-749a-46da-b215-842bc13ed542",
                   :children-of "036a71bb-01dc-440e-8c05-80eea0ca9640"
                   :params      {}}]
        actual   (util/flatten-form-fields (:content person-info-form))]
    (is (= expected actual))))

(deftest correct-initial-validity-for-nested-form
  (let [initial-answers (create-initial-answers (util/flatten-form-fields (:content form1)) nil nil)]
    (is (= {:G__2  {:value  ""
                    :values {:value ""
                             :valid false}
                    :valid  false
                    :label  {:fi "kenttä1", :sv ""}}
            :G__14 {:value  ""
                    :values {:value ""
                             :valid true}
                    :valid  true
                    :label  {:fi "kenttä2", :sv ""}}
            :G__25 {:value  ""
                    :values {:value ""
                             :valid true}
                    :valid  true
                    :label  {:fi "ulkokenttä", :sv ""}}}
           initial-answers))))

(deftest correct-initial-value-for-single-choice-question-group-field
  (let [initial-answers (create-initial-answers flat-form-with-question-group nil nil)]
    (is (=
          {:G__2 {:valid false,
                  :value [nil],
                  :values [nil],
                  :label {:fi "kenttä1", :sv ""}}}
          initial-answers))))

(defn check-language-and-validity [initial-answers, lang, valid]
  (is (= {:language {:value  lang
                     :values {:value lang :valid valid}
                     :valid  valid
                     :label  {:fi "kieli", :sv ""}}}
         initial-answers)))

(deftest default-language-finnish-for-finnish-form
  (let [form (form-with-language :fi)
        flattened-form (util/flatten-form-fields (:content form))
        initial-answers (create-initial-answers flattened-form nil (:selected-language form))]
    (check-language-and-validity initial-answers "FI" true)))

(deftest default-language-swedish-for-swedish-form
  (let [form (form-with-language :sv)
        flattened-form (util/flatten-form-fields (:content form))
        initial-answers (create-initial-answers flattened-form nil (:selected-language form))]
    (check-language-and-validity initial-answers "SV" true)))

(deftest default-language-empty-for-english-form
  (let [form (form-with-language :en)
        flattened-form (util/flatten-form-fields (:content form))
        initial-answers (create-initial-answers flattened-form nil (:selected-language form))]
    (check-language-and-validity initial-answers "" false)))

(deftest answers->valid-status-gives-false-when-one-answer-is-not-valid
  (is (= {:invalid-fields '({:key :one :label {:fi "invaliidi"}})}
         (answers->valid-status {:one   {:valid false :label {:fi "invaliidi"}}
                                 :two   {:valid true}
                                 :three {:valid true}}
                                nil
                                [{:id "one"}
                                 {:id "two"}
                                 {:id "three"}]))))

(deftest answers->valid-status-gives-true-for-all-valid
  (is (= {:invalid-fields '()}
         (answers->valid-status {:one   {:valid true}
                                 :two   {:valid true}
                                 :three {:valid true}}
                                nil
                                [{:id "one"}
                                 {:id "two"}
                                 {:id "three"}]))))

(deftest answers->valid-status-gives-true-for-all-valid-and-some-extra-questions
  (is (= {:invalid-fields '()}
         (answers->valid-status {:one   {:valid true}
                                 :two   {:valid true}
                                 :three {:valid true}
                                 :four  {:valid false}}
                                nil
                                [{:id "one"}
                                 {:id "two"}
                                 {:id "three"}]))))

(def application-data-to-submit {:answers
                                 (sorted-map
                                    :G__14 {:valid true :value "Jorma"},
                                    :G__2 {:valid true :value "16"},
                                    :G__25 {:valid true :value "Joroinen"})})

(def expected-application {:form 37,
                           :strict-warnings-on-unchanged-edits? true
                           :lang "fi",
                           :hakukohde '(),
                           :haku nil,
                           :answers '({:key "G__14",
                                       :value "Jorma",
                                       :fieldType "textField",
                                       :label {:fi "kenttä2", :sv ""}}
                                      {:key "G__2",
                                       :value "16",
                                       :fieldType "textField",
                                       :label {:fi "kenttä1", :sv ""}}
                                      {:key "G__25",
                                       :value "Joroinen",
                                       :fieldType "textField",
                                       :label {:fi "ulkokenttä", :sv ""}})
                           :tunnistautunut false})

(deftest application-to-submit-is-correct
  (let [result (create-application-to-submit application-data-to-submit form1 "fi" true false)]
    (is (= expected-application result))))

(def form2
  {:id 38,
   :name {:fi "toinen lomake"},
   :created-time "y",
   :created-by "1.2.246.562.11.11111111111",
   :content [{:id "w1",
              :label {:fi "osio1", :sv ""},
              :children [{:id "f1",
                          :label {:fi "kenttä1", :sv ""},
                          :params {:size "S"},
                          :validators ["required"]
                          :fieldType "textField",
                          :fieldClass "formField"
                          :metadata metadata}
                         {:id "f2",
                          :label {:fi "kenttä2", :sv ""},
                          :params {:size "M"},
                          :fieldType "textField",
                          :fieldClass "formField"
                          :metadata metadata}],
              :fieldType "fieldset",
              :fieldClass "wrapperElement"
              :metadata metadata}
             {:id "w2",
              :label {:fi "osio2", :sv ""},
              :children [{:id "f3",
                          :label {:fi "kenttä3", :sv ""},
                          :params {:size "S"},
                          :validators ["required"]
                          :fieldType "textField",
                          :fieldClass "formField"
                          :metadata metadata}],
              :fieldType "fieldset",
              :fieldClass "wrapperElement"
              :metadata metadata}
             {:id "f4",
              :label {:fi "ulkokenttä", :sv ""},
              :params {:size "L"},
              :fieldType "textField",
              :fieldClass "formField"
              :metadata metadata}]})

(deftest wrappers-are-extracted-correctly
  (let [result (extract-wrapper-sections form2)
        expected (list {:id    "w1",
                        :label {:fi "osio1", :sv ""},
                        :children
                        [{:id         "f1",
                          :label      {:fi "kenttä1", :sv ""},
                          :params     {:size "S"},
                          :validators ["required"],
                          :fieldType  "textField",
                          :fieldClass "formField"
                          :metadata   metadata}
                         {:id         "f2",
                          :label      {:fi "kenttä2", :sv ""},
                          :params     {:size "M"},
                          :fieldType  "textField",
                          :fieldClass "formField"
                          :metadata   metadata}]}
                       {:id    "w2",
                        :label {:fi "osio2", :sv ""},
                        :children
                        [{:id         "f3",
                          :label      {:fi "kenttä3", :sv ""},
                          :params     {:size "S"},
                          :validators ["required"],
                          :fieldType  "textField",
                          :fieldClass "formField"
                          :metadata   metadata}]})]
    (is (= expected result))))

(def
  answers
  {:G__2  {:valid false}
   :G__14 {:valid true}
   :G__25 {:valid true}})
