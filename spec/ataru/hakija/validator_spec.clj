(ns ataru.hakija.validator-spec
  (:require [ataru.hakija.validator :as validator]
            [clojure.core.match :refer [match]]
            [speclj.core :refer :all]
            [ataru.util :as util]
            [ataru.fixtures.answer :refer [answer]]
            [ataru.fixtures.person-info-form :refer [form]]
            [taoensso.timbre :refer [spy debug]]))

(def f form)
(def a answer)
(def extra-answers (update a :answers conj {:key "foo" :value "barbara"}))
(def answers-by-key (util/answers-by-key (:answers a)))

(def hakukohde-specific-question {:id "d2a26771-de96-4f34-867e-d112c09cbd6b"
                                  :label {:fi "Kerro lyhyesti masennuksestasi"
                                          :sv ""}
                                  :params {:repeatable false}
                                  :fieldType "textField"
                                  :fieldClass "formField"
                                  :validators ["required"]
                                  :belongs-to-hakukohteet ["1.2.246.562.20.352373851710"]})

(def hakukohde-specific-question-another-hakukohde (assoc hakukohde-specific-question :belongs-to-hakukohteet ["1.2.246.562.20.352373851711"]))

(def hakukohde-specific-question-answer {:key "d2a26771-de96-4f34-867e-d112c09cbd6b"
                                         :value "tsers"
                                         :fieldType "textField"})

(def hakukohde-specific-question-answer-nil-value (assoc hakukohde-specific-question-answer :value nil))


(def hakukohde-answer {:key "hakukohteet"
                       :label {:en ""
                               :fi "Hakukohteet"
                               :sv ""}
                       :value ["1.2.246.562.20.352373851710"]
                       :fieldType "hakukohteet"})


(def hakukohde-question {:id "hakukohteet"
                         :label {:en ""
                                 :fi "Hakukohteet"
                                 :sv ""}
                         :params {}
                         :options []
                         :fieldType "hakukohteet"
                         :fieldClass "formField"
                         :validators ["required"]
                         :exclude-from-answers-if-hidden true})

(describe "application validation"
  (tags :unit)
  (it "fails answers with extraneous keys"
    (should= false
      (-> (validator/valid-application? extra-answers f)
        :passed?))
    (should= #{:foo}
      (validator/extra-answers-not-in-original-form
        (map (comp keyword :id) (util/flatten-form-fields (:content f)))
        (keys (util/answers-by-key (:answers extra-answers))))))
  (it "fails answers with missing answers"
    (should= false
      (:passed? (validator/valid-application? (assoc a :answers []) f)))
    (should= false
      (:passed? (validator/valid-application? (update a :answers rest) f))))

  (it "passes validation"
    (should= true
      (:passed? (validator/valid-application? a f)))
    (should=
      {:address                              {:passed? true}
       :email                                {:passed? true}
       :preferred-name                       {:passed? true}
       :last-name                            {:passed? true}
       :phone                                {:passed? true}
       :nationality                          {:passed? true}
       :first-name                           {:passed? true}
       :postal-code                          {:passed? true}
       :language                             {:passed? true}
       :gender                               {:passed? true}
       :postal-office                        {:passed? true}
       :home-town                            {:passed? true}
       ; ssn+birthdate container
       :a3199cdf-fba3-4be1-8ab1-760f75f16d54 {:passed? true}
       ; repeatable text field
       :047da62c-9afe-4e28-bfe8-5b50b21b4277 {:passed? true}
       ; multipleChoice
       :c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0 {:passed? true}
       ; dropdown "Pohjakoulutus"
       :b05a6057-2c65-40a8-9312-c837429f44bb {:passed? true}}
      (validator/build-results answers-by-key
        []
        (:content f))))

  (it "passes validation on multipleChoice answer being empty"
    (should
      (-> (validator/build-results
            (update
              answers-by-key
              :c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0
              assoc
              :value "")
            []
            (:content f))
          :c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0
          :passed?)))

  (it "passes validation on dropdown answer being empty"
      (should
        (-> (validator/build-results
              (update
                answers-by-key
                :gender
                assoc
                :value "")
              []
              (clojure.walk/postwalk
                (fn [form]
                  (match form
                    {:id "gender"}
                    (dissoc form :validators)
                    :else form))
                (:content f)))
            :gender
            :passed?)))

  (it "fails validation on multipleChoice answer being empty and required set to true"
    (should-not
      (-> (validator/build-results
            (update
              answers-by-key
              :c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0
              assoc
              :value "")
            []
            (clojure.walk/postwalk
              (fn [form]
                (match form
                  {:id "c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0"}
                  (assoc form :validators ["required"])
                  :else form))
              (:content f)))
          :c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0
          :passed?)))

  (it "fails validation on dropdown answer being empty and required set to true"
      (should-not
        (-> (validator/build-results
              (update
                answers-by-key
                :gender
                assoc
                :value "")
              []
              (clojure.walk/postwalk
                (fn [form]
                  (match form
                    {:id "gender"}
                    (assoc form :validators ["required"])
                    :else form))
                (:content f)))
          :gender
          :passed?)))

  (it "passes validation on repeatable answer being empty"
    (should
      (-> (validator/build-results
            (update
              answers-by-key
              :047da62c-9afe-4e28-bfe8-5b50b21b4277
              assoc
              :value [])
            []
            (:content f))
          :047da62c-9afe-4e28-bfe8-5b50b21b4277
          :passed?)))

  (it "fails validation on repeatable answer being empty and required set to true"
      (should-not
        (-> (validator/build-results
              (update
                answers-by-key
                :047da62c-9afe-4e28-bfe8-5b50b21b4277
                assoc
                :value [])
              []
              (clojure.walk/postwalk
                (fn [form]
                  (match form
                    {:id "047da62c-9afe-4e28-bfe8-5b50b21b4277"}
                    (assoc form :validators ["required"])
                    :else form))
                (:content f)))
          :047da62c-9afe-4e28-bfe8-5b50b21b4277
          :passed?)))

  (it "fails validation when followup field is set required and no answer"
      (should-not
        (-> (validator/build-results
              answers-by-key
              []
              (clojure.walk/postwalk
                (fn [form]
                  (match form
                    {:id "fbe3522d-6f1d-4e05-85e3-4e716146c686"}
                    (assoc form :validators ["required"])
                    :else form))
                (:content f)))
            :fbe3522d-6f1d-4e05-85e3-4e716146c686
            :passed?)))

  (it "passes validation when followup field is set required and answer is provided"
      (should
        (-> (validator/build-results
              (->
                (update a :answers conj {:key "fbe3522d-6f1d-4e05-85e3-4e716146c686" :value "perustelu"})
                :answers
                util/answers-by-key
                (assoc :b05a6057-2c65-40a8-9312-c837429f44bb {:key "b05a6057-2c65-40a8-9312-c837429f44bb", :fieldType "dropdown", :value "Ammatillinen peruskoulu"}))
              []
              (clojure.walk/postwalk
                (fn [form]
                  (match form
                    {:id "fbe3522d-6f1d-4e05-85e3-4e716146c686"}
                    (assoc form :validators ["required"])
                    :else form))
                (:content f)))
          :fbe3522d-6f1d-4e05-85e3-4e716146c686
          :passed?)))

  (it "fails validation when incorrect birth-date data is used with :birthdate-and-gender-component validation"
      (should-not
        (-> (validator/build-results
              (->
                a
                :answers
                util/answers-by-key
                (assoc :birth-date {:key       "birth-date",
                                    :value     "02.02.2022",
                                    :fieldType "textField",
                                    :label     {:fi "Syntymäaika", :sv "Födelsetid"}}))
              []
              (clojure.walk/postwalk
                (fn [form-field]
                  (match form-field
                         {:id "a3199cdf-fba3-4be1-8ab1-760f75f16d54"}
                         (assoc form-field :child-validator "birthdate-and-gender-component")
                         :else form-field))
                (:content f)))
            :a3199cdf-fba3-4be1-8ab1-760f75f16d54
            :passed?)))

  (it "passes validation when no hakukohde selected (and no answers are specified to a hakukohde)"
      (should= true
               (:passed? (validator/valid-application? a (update-in f [:content] conj hakukohde-specific-question)))))

  (it "passes validation when no hakukohde is selected, a question belongs to a question a but has no value"
      (should= true
               (:passed? (validator/valid-application?
                           (update-in a [:answers] conj hakukohde-specific-question-answer-nil-value)
                           (update-in f [:content] conj hakukohde-specific-question)))))

  (it "fails when no hakukohde is selected, a question belongs to a question and has a value"
      (should= false
               (:passed? (validator/valid-application?
                           (update-in a [:answers] conj hakukohde-specific-question-answer)
                           (update-in f [:content] conj hakukohde-specific-question)))))


  (it "passes validation when hakukohde is selected and no answers are specified to a hakukohde"
      (should= true
               (:passed? (validator/valid-application? a (update-in f [:content] conj hakukohde-specific-question)))))

  (it "passes validation when hakukohde is selected and an answer belongs to it"
      (should= true
               (:passed? (validator/valid-application?
                           (update-in a [:answers] conj hakukohde-specific-question-answer hakukohde-answer)
                           (update-in f [:content] conj hakukohde-question hakukohde-specific-question)))))

  (it "passes validation when hakukohde is selected, a question belongs to different hakukohde but has no value"
      (should= true
               (:passed? (validator/valid-application?
                           (update-in a [:answers] conj hakukohde-specific-question-answer-nil-value hakukohde-answer)
                           (update-in f [:content] conj hakukohde-question hakukohde-specific-question-another-hakukohde)))))

  (it "fails validation when hakukohde is selected, a question belongs to different hakukohde but has a value"
      (should= false
               (:passed? (validator/valid-application?
                          (update-in a [:answers] conj hakukohde-specific-question-answer hakukohde-answer)
                          (update-in f [:content] conj hakukohde-question hakukohde-specific-question-another-hakukohde))))))
