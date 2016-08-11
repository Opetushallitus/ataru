(ns ataru.hakija.application-test
  (:require [cljs.test :refer-macros [deftest is]]
            [ataru.hakija.application :refer [create-initial-answers
                                              answers->valid-status
                                              create-application-to-submit
                                              extract-wrapper-sections
                                              wrapper-sections-with-validity]]
            [ataru.util :as util]))

(def form1
  {:id 37,
   :name "uusi lomake",
   :modified-time "x",
   :modified-by "DEVELOPER",
   :content [{:id "G__1",
              :label {:fi "osio1", :sv "Avsnitt namn"},
              :children [{:id "G__2",
                          :label {:fi "kenttä1", :sv ""},
                          :params {:size "S"},
                          :validators ["required"]
                          :fieldType "textField",
                          :fieldClass "formField"}
                         {:id "G__14",
                          :label {:fi "kenttä2", :sv ""},
                          :params {:size "M"},
                          :validators ["some-validator"]
                          :fieldType "textField",
                          :fieldClass "formField"}],
              :fieldType "fieldset",
              :fieldClass "wrapperElement"}
             {:id "G__25",
              :label {:fi "ulkokenttä", :sv ""},
              :params {:size "L"},
              :fieldType "textField",
              :fieldClass "formField"}]})

(def person-info-form
  {:id 22,
   :name "Testilomake",
   :modified-by "DEVELOPER",
   :modified-time "2016-07-15T13:48:17.815+03:00",
   :content
   [{:fieldClass "wrapperElement",
     :id "5febd7b0-75f0-462c-b9a4-6cac6a4bec88",
     :fieldType "fieldset",
     :children
                 [{:fieldClass "wrapperElement",
                   :id "399d9123-f15f-402a-9ce9-2749d0578399",
                   :fieldType "rowcontainer",
                   :children
                               [{:label {:fi "Etunimet", :sv "Förnamn"},
                                 :fieldClass "formField",
                                 :id "380913e2-8c93-494c-bd86-57000ed50ae8",
                                 :params {},
                                 :validators ["required"]
                                 :fieldType "textField"}
                                {:label {:fi "Kutsumanimi", :sv "Smeknamn"},
                                 :fieldClass "formField",
                                 :id "7c8388f0-7ccb-4706-8630-15405b141552",
                                 :params {:size "S"},
                                 :validators ["required"]
                                 :fieldType "textField"}],
                   :params {}}
                  {:label {:fi "Sukunimi", :sv "Efternamn"},
                   :fieldClass "formField",
                   :id "d2dc3e2e-c130-4fd4-8509-7c8fbf4d1c9e",
                   :params {},
                   :validators ["required"]
                   :fieldType "textField"}],
     :params {},
     :label {:fi "Henkilötiedot", :sv "Personlig information"},
     :module "person-info"}
    {:fieldClass "wrapperElement",
     :fieldType "fieldset",
     :id "036a71bb-01dc-440e-8c05-80eea0ca9640",
     :label {:fi "Osion nimi", :sv "Avsnitt namn"},
     :children [{:fieldClass "formField",
                   :fieldType "textField",
                   :label {:fi "Random question", :sv ""},
                   :id "839cb685-749a-46da-b215-842bc13ed542",
                   :params {}}],
     :params {}}]})

(deftest flattens-correctly
  (let [expected   [{:id "G__2",
                      :label {:fi "kenttä1", :sv ""},
                      :params {:size "S"},
                      :validators ["required"]
                      :fieldType "textField",
                      :fieldClass "formField"}
                     {:id "G__14",
                      :label {:fi "kenttä2", :sv ""},
                      :params {:size "M"},
                      :validators ["some-validator"]
                      :fieldType "textField",
                      :fieldClass "formField"}
                     {:id "G__25",
                      :label {:fi "ulkokenttä", :sv ""},
                      :params {:size "L"},
                      :fieldType "textField",
                      :fieldClass "formField"}]
        actual (util/flatten-form-fields (:content form1))]
    (is (= expected actual))))

(deftest flattens-row-container-answers
  (let [expected [{:label {:fi "Etunimet", :sv "Förnamn"},
                   :fieldClass "formField",
                   :id "380913e2-8c93-494c-bd86-57000ed50ae8",
                   :params {},
                   :validators ["required"]
                   :fieldType "textField"}
                  {:label {:fi "Kutsumanimi", :sv "Smeknamn"},
                   :fieldClass "formField",
                   :id "7c8388f0-7ccb-4706-8630-15405b141552",
                   :params {:size "S"},
                   :validators ["required"]
                   :fieldType "textField"}
                  {:label {:fi "Sukunimi", :sv "Efternamn"},
                   :fieldClass "formField",
                   :id "d2dc3e2e-c130-4fd4-8509-7c8fbf4d1c9e",
                   :params {},
                   :validators ["required"]
                   :fieldType "textField"}
                  {:fieldClass "formField",
                   :fieldType "textField",
                   :label {:fi "Random question", :sv ""},
                   :id "839cb685-749a-46da-b215-842bc13ed542",
                   :params {}}]
        actual (util/flatten-form-fields (:content person-info-form))]
    (is (= expected actual))))

(deftest correct-initial-validity-for-nested-form
  (let [initial-answers (create-initial-answers form1)]
    (is (= {:G__2 {:valid false, :label {:fi "kenttä1", :sv ""} :order-idx 0}
            :G__14 {:valid true, :label {:fi "kenttä2", :sv ""} :order-idx 1}
            :G__25 {:valid true, :label {:fi "ulkokenttä", :sv ""} :order-idx 2}}
           initial-answers))))

(deftest answers->valid-status-gives-false-when-one-answer-is-not-valid
  (let [result (answers->valid-status {:one {:valid false :label {:fi "invaliidi"}}, :two {:valid true}, :three {:valid true}})]
    (is (= {:valid false :invalid-fields '({:key :one :label {:fi "invaliidi"}})} result))))

(deftest answers->valid-status-gives-false-for-empty-map
  (is (= {:valid false  :invalid-fields '()} (answers->valid-status {}))))

(deftest answers->valid-status-gives-true-for-all-valid
  (let [result (answers->valid-status {:one {:valid true}, :two {:valid true}, :three {:valid true}})]
    (is (= {:valid true :invalid-fields '()} result))))

(def application-data-to-submit {:answers
                                 (sorted-map
                                    :G__14 {:valid true :value "Jorma"},
                                    :G__2 {:valid true :value "16"},
                                    :G__25 {:valid true :value "Joroinen"})})

(def expected-application {:form 37,
                           :lang "fi",
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
                                       :label {:fi "ulkokenttä", :sv ""}})})

(deftest application-to-submit-is-correct
  (let [result (create-application-to-submit application-data-to-submit form1 "fi")]
    (is (= expected-application result))))

(def form2
  {:id 38,
   :name "toinen lomake",
   :modified-time "y",
   :modified-by "DEVELOPER",
   :content [{:id "w1",
              :label {:fi "osio1", :sv ""},
              :children [{:id "f1",
                          :label {:fi "kenttä1", :sv ""},
                          :params {:size "S"},
                          :validators ["required"]
                          :fieldType "textField",
                          :fieldClass "formField"}
                         {:id "f2",
                          :label {:fi "kenttä2", :sv ""},
                          :params {:size "M"},
                          :fieldType "textField",
                          :fieldClass "formField"}],
              :fieldType "fieldset",
              :fieldClass "wrapperElement"}
             {:id "w2",
              :label {:fi "osio2", :sv ""},
              :children [{:id "f3",
                          :label {:fi "kenttä3", :sv ""},
                          :params {:size "S"},
                          :validators ["required"]
                          :fieldType "textField",
                          :fieldClass "formField"}],
              :fieldType "fieldset",
              :fieldClass "wrapperElement"}
             {:id "f4",
              :label {:fi "ulkokenttä", :sv ""},
              :params {:size "L"},
              :fieldType "textField",
              :fieldClass "formField"}]})

(deftest wrappers-are-extracted-correctly
  (let [result (extract-wrapper-sections form2)
        expected '({:id "w1" :label {:fi "osio1", :sv ""}} {:id "w2" :label {:fi "osio2", :sv ""}})]
    (is (= expected result))))

(def
  answers
  {:G__2  {:valid false}
   :G__14 {:valid true}
   :G__25 {:valid true}})

(deftest wrapper-sections-with-validity-is-correctly-constructed
  (let [wrapper-sections '({:id "w1" :label {:fi "osio1", :sv ""}} {:id "w2" :label {:fi "osio2", :sv ""}})
        answers {:f1 {:valid true}
                 :f2 {:valid false}
                 :f3 {:valid true}
                 :f4 {:valid true}
                 :f5 {:valid true}}
        expected '({:id "w1" :valid false :label {:fi "osio1", :sv ""}} {:id "w2" :valid true :label {:fi "osio2", :sv ""}})
        result (wrapper-sections-with-validity wrapper-sections answers)]
    (is (= expected result))))
