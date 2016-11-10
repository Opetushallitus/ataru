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

(describe "application validation"
  (tags :unit)
  (it "fails answers with extraneous keys"
    (should= false
      (:passed? (validator/valid-application? extra-answers f)))
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
       :c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0 {:passed? true}}
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
                util/answers-by-key)
              []
              (clojure.walk/postwalk
                (fn [form]
                  (match form
                    {:id "fbe3522d-6f1d-4e05-85e3-4e716146c686"}
                    (assoc form :validators ["required"])
                    :else form))
                (:content f)))
          :fbe3522d-6f1d-4e05-85e3-4e716146c686
          :passed?))))
