(ns ataru.hakija.application-test
  (:require [cljs.test :refer-macros [deftest is]]
            [ataru.hakija.application :refer [create-initial-answers
                                              answers->valid-status
                                              create-application-to-submit
                                              flatten-form-fields
                                              extract-wrapper-sections
                                              wrapper-sections-with-validity]]))

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
                          :required true,
                          :fieldType "textField",
                          :fieldClass "formField"}
                         {:id "G__14",
                          :label {:fi "kenttä2", :sv ""},
                          :params {:size "M"},
                          :required false,
                          :fieldType "textField",
                          :fieldClass "formField"}],
              :fieldType "fieldset",
              :fieldClass "wrapperElement"}
             {:id "G__25",
              :label {:fi "ulkokenttä", :sv ""},
              :params {:size "L"},
              :required false,
              :fieldType "textField",
              :fieldClass "formField"}]})

(deftest flattens-correctly
  (let [expected   #{{:id "G__2",
                      :wrapper-id "G__1"
                      :label {:fi "kenttä1", :sv ""},
                      :params {:size "S"},
                      :required true,
                      :fieldType "textField",
                      :fieldClass "formField"}
                     {:id "G__14",
                      :wrapper-id "G__1"
                      :label {:fi "kenttä2", :sv ""},
                      :params {:size "M"},
                      :required false,
                      :fieldType "textField",
                      :fieldClass "formField"}
                     {:id "G__25",
                      :label {:fi "ulkokenttä", :sv ""},
                      :params {:size "L"},
                      :required false,
                      :fieldType "textField",
                      :fieldClass "formField"}}
        actual (set (flatten-form-fields (:content form1)))]
    (is (= expected actual))))

(deftest correct-initial-validity-for-nested-form
  (let [initial-answers (create-initial-answers form1)]
    (is (= {:G__2 {:valid false, :wrapper-id "G__1", :label {:fi "kenttä1", :sv ""}}
            :G__14 {:valid true, :wrapper-id "G__1", :label {:fi "kenttä2", :sv ""}}
            :G__25 {:valid true, :wrapper-id nil, :label {:fi "ulkokenttä", :sv ""}}}
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
                          :required true,
                          :fieldType "textField",
                          :fieldClass "formField"}
                         {:id "f2",
                          :label {:fi "kenttä2", :sv ""},
                          :params {:size "M"},
                          :required false,
                          :fieldType "textField",
                          :fieldClass "formField"}],
              :fieldType "fieldset",
              :fieldClass "wrapperElement"}
             {:id "w2",
              :label {:fi "osio2", :sv ""},
              :children [{:id "f3",
                          :label {:fi "kenttä3", :sv ""},
                          :params {:size "S"},
                          :required true,
                          :fieldType "textField",
                          :fieldClass "formField"}],
              :fieldType "fieldset",
              :fieldClass "wrapperElement"}
             {:id "f4",
              :label {:fi "ulkokenttä", :sv ""},
              :params {:size "L"},
              :required false,
              :fieldType "textField",
              :fieldClass "formField"}]})

(deftest wrappers-are-extracted-correctly
  (let [result (extract-wrapper-sections form2)
        expected '({:id "w1" :label {:fi "osio1", :sv ""}} {:id "w2" :label {:fi "osio2", :sv ""}})]
    (is (= expected result))))

(def
  answers
  {:G__2
   {:valid false :wrapper-id "G__1"}
   :G__14 {:valid true :wrapper-id "G__1"}
   :G__25 {:valid true :wrapper-id nil}})

(deftest wrapper-sections-with-validity-is-correctly-constructed
  (let [wrapper-sections '({:id "w1" :label {:fi "osio1", :sv ""}} {:id "w2" :label {:fi "osio2", :sv ""}})
        answers {:f1 {:valid true :wrapper-id "w1"}
                 :f2 {:valid false :wrapper-id "w1"}
                 :f3 {:valid true :wrapper-id "w2"}
                 :f4 {:valid true :wrapper-id "w2"}
                 :f5 {:valid true :wrapper-id nil}}
        expected '({:id "w1" :valid false :label {:fi "osio1", :sv ""}} {:id "w2" :valid true :label {:fi "osio2", :sv ""}})
        result (wrapper-sections-with-validity wrapper-sections answers)]
    (is (= expected result))))
