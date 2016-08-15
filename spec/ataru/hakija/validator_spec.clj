(ns ataru.hakija.validator-spec
  (:require [ataru.hakija.validator :as validator]
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
      (validator/valid-application? extra-answers f))
    (should= #{:foo}
      (validator/extra-answers-not-in-original-form
        (map (comp keyword :id) (util/flatten-form-fields (:content f)))
        (keys (util/answers-by-key (:answers extra-answers))))))
  (it "fails answers with missing answers"
    (should= false
      (validator/valid-application? (assoc a :answers []) f))
    (should= false
      (validator/valid-application? (update a :answers rest) f)))

  (it "passes validation"
    (should= true
      (validator/valid-application? a f))
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
       :a3199cdf-fba3-4be1-8ab1-760f75f16d54 {:passed? true}}
      (validator/build-results answers-by-key
        []
        (:content f)))))
