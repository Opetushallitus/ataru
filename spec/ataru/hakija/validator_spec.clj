(ns ataru.hakija.validator-spec
  (:require [ataru.hakija.validator :as validator]
            [clojure.core.match :refer [match]]
            [clojure.core.async :as async]
            [clojure.walk :as walk]
            [ataru.cache.cache-service :as cache-service]
            [speclj.core :refer [describe tags it should should= should-not]]
            [ataru.util :as util]
            [ataru.fixtures.answer :refer [answer]]
            [ataru.fixtures.person-info-form :refer [form]]
            [ataru.fixtures.form :refer [tutkinto-test-form]]))

(def f form)
(def a answer)
(def flattened-form (util/flatten-form-fields (:content form)))
(def extra-answers (update a :answers conj {:key "foo" :value "barbara"}))
(def answers-by-key (util/answers-by-key (:answers a)))
(def base-oppija-session {:data {:fields {:first-name  {:value  "Timo"
                                                        :locked true}
                                          :postal-code {:value  nil
                                                        :locked false}
                                          :address     {:value  "Niin pitkä ulkomaalaalainen osoite kuin on mahdollista antaa, jotta asikas voisi"
                                                        :locked false}
                                          :ssn         {:value  "120997-9998"
                                                        :locked true}
                                          :email       {:value  nil
                                                        :locked false}}}})

(def hakukohde-specific-question {:id "d2a26771-de96-4f34-867e-d112c09cbd6b"
                                  :label {:fi "Kerro lyhyesti masennuksestasi"
                                          :sv ""}
                                  :params {:repeatable false}
                                  :fieldType "textField"
                                  :fieldClass "formField"
                                  :validators ["required"]
                                  :belongs-to-hakukohteet ["1.2.246.562.20.352373851710"]})

(def hakukohde-specific-question-answer {:key "d2a26771-de96-4f34-867e-d112c09cbd6b"
                                         :value "tsers"
                                         :fieldType "textField"})

(def hakukohde-specific-question-another-hakukohde (assoc hakukohde-specific-question :belongs-to-hakukohteet ["1.2.246.562.20.352373851711"]))

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

(def hakukohde-answer {:key "hakukohteet"
                       :label {:en ""
                               :fi "Hakukohteet"
                               :sv ""}
                       :value ["1.2.246.562.20.352373851710"]
                       :fieldType "hakukohteet"})

(def hakukohde-specific-dropdown-with-followups {:id "ce1864c0-ce3f-4c1d-8405-5c0adff7ca2b"
                                                 :label {:fi "Miksi masennuit?"
                                                         :sv ""}
                                                 :options [{:label {:fi "En osaa sanoa"
                                                                    :sv ""}
                                                             :value "En osaa sanoa"
                                                             :followups [{:id "cc150f67-8a7e-4502-b17f-34f43d4198b1"
                                                                          :label {:fi "etkö?"
                                                                                  :sv ""}
                                                                          :params {}
                                                                          :fieldType "textField"
                                                                          :fieldClass "formField"
                                                                          :validators ["required"]}]}
                                                           {:label {:fi "Faija skitsoo"
                                                                    :sv ""}
                                                            :value "Faija skitsoo"}]
                                                 :fieldType "dropdown"
                                                 :fieldClass "formField"
                                                 :belongs-to-hakukohteet ["1.2.246.562.20.352373851710"]})

(def per-hakukohde-specific-dropdown {:id "ce1864c0-ce3f-4c1d-8405-5c4ydff7ca2c"
                                      :label {:fi "Mikä on vointisi?"}
                                      :options [{:label {:fi "Hyvä"}
                                                 :value "Hyvä"}
                                                {:label {:fi "Huono"}
                                                 :value "Huono"
                                                 :followups [{:id "ce1864c0-ce3f-4c1d-8405-5c4ydff7dff2"
                                                              :label {:fi "Mikä mättää?"}
                                                              :fieldType "textField"
                                                              :fieldClass "formField"
                                                              :params {}
                                                              :validators ["required"]}]}]
                                      :fieldType "dropdown"
                                      :validators ["required"]
                                      :fieldClass "formField"
                                      :per-hakukohde true
                                      :belongs-to-hakukohderyhma ["1.2.246.562.20.352373851710"]})

(def per-hakukohde-specific-dropdown-answer {:key "ce1864c0-ce3f-4c1d-8405-5c4ydff7ca2c_1.2.246.562.20.352373851710",
                      :label {:fi "Mikä on vointisi?"}
                      :value "Huono"
                      :duplikoitu-kysymys-hakukohde-oid "1.2.246.562.20.352373851710"
                      :original-question "ce1864c0-ce3f-4c1d-8405-5c4ydff7ca2c"
                      :fieldType "dropdown"})
(def per-hakukohde-specific-followup-answer {:key "ce1864c0-ce3f-4c1d-8405-5c4ydff7dff2_1.2.246.562.20.352373851710",
                                             :label {:fi "Mikä mättää?"}
                                             :value "Pikkuvarvas osui lipaston jalkaan"
                                             :duplikoitu-followup-hakukohde-oid "1.2.246.562.20.352373851710"
                                             :original-followup "ce1864c0-ce3f-4c1d-8405-5c4ydff7dff2"
                                             :fieldType "textField"})

(def dropdown-answer {:key "ce1864c0-ce3f-4c1d-8405-5c0adff7ca2b",
                      :label {:fi "Miksi masennuit?"
                              :sv ""}
                      :value "En osaa sanoa"
                      :fieldType "dropdown"})

(def dropdown-followup-answer {:key "cc150f67-8a7e-4502-b17f-34f43d4198b1",
                               :label {:fi "etkö?"
                                       :sv ""}
                               :value "en.."
                               :fieldType "textField"})

(def required-hakija-question {:id         "sahkoisen-asioinnin-lupa"
                               :label      {:en "Opiskelijavalinnan tulokset saa lähettää minulle sähköisesti."
                                            :fi "Opiskelijavalinnan tulokset saa lähettää minulle sähköisesti."
                                            :sv "Opiskelijavalinnan tulokset saa lähettää minulle sähköisesti."}
                               :options    [{:label {:en "Yes"
                                                     :fi "Kyllä"
                                                     :sv "Ja"}
                                             :value "Kyllä"}]
                               :fieldType  "singleChoice"
                               :fieldClass "formField"
                               :validators ["required-hakija"]})

(def required-hakija-answer {:key       "sahkoisen-asioinnin-lupa"
                             :value     "Kyllä"
                             :fieldType "singleChoice"})

(def hakukohde-answer-ordered {:key "hakukohteet"
                       :label {:en ""
                               :fi "Hakukohteet"
                               :sv ""}
                       :value ["1.2.246.562.20.352373851710, 1.2.246.562.20.352373851711"]
                       :fieldType "hakukohteet"})

(def hakukohde-answer-ordered-inverted {:key "hakukohteet"
                               :label {:en ""
                                       :fi "Hakukohteet"
                                       :sv ""}
                               :value ["1.2.246.562.20.352373851711, 1.2.246.562.20.352373851710"]
                               :fieldType "hakukohteet"})

(defn- has-never-applied [] (async/go false))

(defn- set-can-submit-multiple-applications
  [multiple? haku-oid field]
  (cond-> (assoc-in field [:params :can-submit-multiple-applications] multiple?)
    (not multiple?) (assoc-in [:params :haku-oid] haku-oid)))

(defn- map-if-ssn-or-email
  [f field]
  (if (or (= "ssn" (:id field))
          (= "email" (:id field)))
    (f field)
    field))

(defn- populate-can-submit-multiple-applications
  [form multiple?]
  (update form :content
          (fn [content]
            (walk/prewalk
             (partial map-if-ssn-or-email
                      (partial set-can-submit-multiple-applications
                               multiple? "dummy-haku-oid"))
             content))))

(def koodisto-cache (reify cache-service/Cache
                      (get-from [_ _])
                      (get-many-from [_ _])
                      (remove-from [_ _])
                      (clear-all [_])))

(def tutkinto-form (-> tutkinto-test-form
                       (assoc-in [:properties :tutkinto-properties :selected-option-ids]
                                 ["lukiokoulutus", "kk-alemmat", "kk-ylemmat", "itse-syotetty"])
                       (assoc-in  [:content 0 :children 1 :property-options 9 :followups 1]
                                  {:validators ["required"],
                                   :fieldClass "formField", :fieldType "textField",
                                   :cannot-edit false, :cannot-view false,
                                   :label {:fi "Millai muuten??", :sv ""},
                                   :id "additional-itse-syotetty-field"})))

(def flattened-tutkinto-form (util/flatten-form-fields (:content tutkinto-form)))

(describe "application validation"
  (tags :unit :backend-validation)
  (it "fails answers with extraneous keys"
    (should= false
      (-> (validator/valid-application? koodisto-cache has-never-applied extra-answers f #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY")
        :passed?))
    (should= #{:foo}
      (validator/extra-answers-not-in-original-form
        (map (comp keyword :id) (util/flatten-form-fields (:content f)))
        (keys (util/answers-by-key (:answers extra-answers))))))
  (it "fails answers with missing answers"
    (should= false
      (:passed? (validator/valid-application? koodisto-cache has-never-applied (assoc a :answers []) f #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY")))
    (should= false
      (:passed? (validator/valid-application? koodisto-cache has-never-applied (update a :answers rest) f #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "passes validation"
    (should (:passed? (validator/valid-application? koodisto-cache has-never-applied a f #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY")))
    (should= {}
             (validator/build-results koodisto-cache
                                      has-never-applied
                                      answers-by-key
                                      f
                                      (:content f)
                                      flattened-form
                                      #{}
                                      false)))

  (it "passes validation on hidden"
    (should= {}
             (validator/build-results koodisto-cache
                                      has-never-applied
                                      (dissoc answers-by-key :c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0)
                                      f
                                      (clojure.walk/postwalk
                                       (fn [form]
                                         (match form
                                           {:id "c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0"}
                                           (-> form
                                               (assoc :validators ["required"])
                                               (assoc-in [:params :hidden] true))
                                           :else form))
                                       (:content f))
                                      flattened-form
                                      #{}
                                      false)))

  (it "fail validation on hidden with answer"
    (should= {:c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0
              (assoc (:c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0 answers-by-key)
                     :value ["Ensimmäinen vaihtoehto"])}
             (validator/build-results koodisto-cache
                                      has-never-applied
                                      (assoc-in answers-by-key [:c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0 :value] ["Ensimmäinen vaihtoehto"])
                                      f
                                      (clojure.walk/postwalk
                                       (fn [form]
                                         (match form
                                           {:id "c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0"}
                                           (-> form
                                               (assoc :validators ["required"])
                                               (assoc-in [:params :hidden] true))
                                           :else form))
                                       (:content f))
                                      flattened-form
                                      #{}
                                      false)))

  (it "passes validation on multipleChoice answer being empty"
    (should= {}
             (validator/build-results koodisto-cache
                                      has-never-applied
                                      (assoc-in answers-by-key [:c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0 :value] [])
                                      f
                                      (:content f)
                                      flattened-form
                                      #{}
                                      false)))

  (it "passes validation on dropdown answer being empty"
    (should= {}
             (validator/build-results koodisto-cache
                                      has-never-applied
                                      (assoc-in answers-by-key [:gender :value] "")
                                      f
                                      (clojure.walk/postwalk
                                       (fn [form]
                                         (match form
                                           {:id "gender"}
                                           (dissoc form :validators)
                                           :else form))
                                       (:content f))
                                      flattened-form
                                      #{}
                                      false)))

  (it "fails validation on multipleChoice answer being empty and required set to true"
    (should= {:c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0
              (assoc (:c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0 answers-by-key) :value [])}
             (validator/build-results koodisto-cache
                                      has-never-applied
                                      (assoc-in answers-by-key [:c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0 :value] [])
                                      f
                                      (clojure.walk/postwalk
                                       (fn [form]
                                         (match form
                                           {:id "c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0"}
                                           (assoc form :validators ["required"])
                                           :else form))
                                       (:content f))
                                      flattened-form
                                      #{}
                                      false)))

  (it "fails validation on dropdown answer being empty and required set to true"
    (should= {:gender
              (assoc (:gender answers-by-key) :value "")}
             (validator/build-results koodisto-cache
                                      has-never-applied
                                      (assoc-in answers-by-key [:gender :value] "")
                                      f
                                      (clojure.walk/postwalk
                                       (fn [form]
                                         (match form
                                           {:id "gender"}
                                           (assoc form :validators ["required"])
                                           :else form))
                                       (:content f))
                                      flattened-form
                                      #{}
                                      false)))

  (it "passes validation on repeatable answer being empty"
    (should= {}
             (validator/build-results koodisto-cache
                                      has-never-applied
                                      (assoc-in answers-by-key [:047da62c-9afe-4e28-bfe8-5b50b21b4277 :value] [""])
                                      f
                                      (:content f)
                                      flattened-form
                                      #{}
                                      false)))

  (it "fails validation on repeatable answer being empty and required set to true"
    (should= {:047da62c-9afe-4e28-bfe8-5b50b21b4277
              (assoc (:047da62c-9afe-4e28-bfe8-5b50b21b4277 answers-by-key) :value [""])}
             (validator/build-results koodisto-cache
                                      has-never-applied
                                      (assoc-in answers-by-key [:047da62c-9afe-4e28-bfe8-5b50b21b4277 :value] [""])
                                      f
                                      (clojure.walk/postwalk
                                       (fn [form]
                                         (match form
                                           {:id "047da62c-9afe-4e28-bfe8-5b50b21b4277"}
                                           (assoc form :validators ["required"])
                                           :else form))
                                       (:content f))
                                      flattened-form
                                      #{}
                                      false)))

  (it "fails validation when followup field is set required and no answer"
    (should= {:fbe3522d-6f1d-4e05-85e3-4e716146c686
              (:fbe3522d-6f1d-4e05-85e3-4e716146c686 answers-by-key)}
             (validator/build-results koodisto-cache
                                      has-never-applied
                                      (assoc-in answers-by-key [:b05a6057-2c65-40a8-9312-c837429f44bb :value] "Ammatillinen peruskoulu")
                                      f
                                      (clojure.walk/postwalk
                                       (fn [form]
                                         (match form
                                           {:id "fbe3522d-6f1d-4e05-85e3-4e716146c686"}
                                           (assoc form :validators ["required"])
                                           :else form))
                                       (:content f))
                                      flattened-form
                                      #{}
                                      false)))

  (it "passes validation when followup field is set required and answer is provided"
    (should= {}
             (validator/build-results koodisto-cache
                                      has-never-applied
                                      (-> answers-by-key
                                          (assoc-in [:fbe3522d-6f1d-4e05-85e3-4e716146c686 :value] "perustelu")
                                          (assoc-in [:b05a6057-2c65-40a8-9312-c837429f44bb :value] "Ammatillinen peruskoulu"))
                                      f
                                      (clojure.walk/postwalk
                                       (fn [form]
                                         (match form
                                           {:id "fbe3522d-6f1d-4e05-85e3-4e716146c686"}
                                           (assoc form :validators ["required"])
                                           :else form))
                                       (:content f))
                                      flattened-form
                                      #{}
                                      false)))

  (it "fails validation when incorrect birth-date data is used with :birthdate-and-gender-component validation"
    (should= {:ssn        (:ssn answers-by-key)
              :birth-date (assoc (:birth-date answers-by-key) :value "02.02.2222")}
             (validator/build-results koodisto-cache
                                      has-never-applied
                                      (assoc-in answers-by-key [:birth-date :value] "02.02.2222")
                                      f
                                      (clojure.walk/postwalk
                                       (fn [form-field]
                                         (match form-field
                                           {:id "a3199cdf-fba3-4be1-8ab1-760f75f16d54"}
                                           (assoc form-field :child-validator "birthdate-and-gender-component")
                                           :else form-field))
                                       (:content f))
                                      flattened-form
                                      #{}
                                      false)))

  (it "passes validation when no hakukohde is selected, a question belongs to a hakukohde a but has no value"
      (should (:passed? (validator/valid-application? koodisto-cache has-never-applied
                         a
                         (update f :content conj hakukohde-specific-question) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "fails when no hakukohde is selected, a question belongs to a hakukohde and has a value"
      (should-not (:passed? (validator/valid-application? koodisto-cache has-never-applied
                             (update a :answers conj hakukohde-specific-question-answer)
                             (update f :content conj hakukohde-specific-question) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "passes validation when hakukohde is selected and no answers are specified to a hakukohde"
      (should (:passed? (validator/valid-application? koodisto-cache has-never-applied a (update f :content conj hakukohde-specific-question) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "passes validation when hakukohde is selected and an answer belongs to it"
      (should (:passed? (validator/valid-application? koodisto-cache has-never-applied
                         (update a :answers conj hakukohde-specific-question-answer hakukohde-answer)
                         (update f :content conj hakukohde-question hakukohde-specific-question) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "passes validation when hakukohde is selected, a question belongs to different hakukohde but has no value"
      (should (:passed? (validator/valid-application? koodisto-cache has-never-applied
                         (update a :answers conj hakukohde-answer)
                         (update f :content conj hakukohde-question hakukohde-specific-question-another-hakukohde) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "fails validation when hakukohde is selected, a question belongs to different hakukohde but has a value"
      (should-not (:passed? (validator/valid-application? koodisto-cache has-never-applied
                             (update a :answers conj hakukohde-specific-question-answer hakukohde-answer)
                             (update f :content conj hakukohde-question hakukohde-specific-question-another-hakukohde) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "passes validation when a dropdown question is hakukohde specific, no answers"
      (should (:passed? (validator/valid-application? koodisto-cache has-never-applied
                         a
                         (update f :content conj hakukohde-specific-dropdown-with-followups) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "passes validation when a dropdown question is hakukohde specific and has answers",
      (should (:passed? (validator/valid-application? koodisto-cache has-never-applied
                         (update a :answers conj hakukohde-answer dropdown-answer dropdown-followup-answer)
                         (update f :content conj hakukohde-question hakukohde-specific-dropdown-with-followups) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "fails validation when a dropdown question is hakukohde specific and has no required followup answers",
      (should-not (:passed? (validator/valid-application? koodisto-cache has-never-applied
                             (update a :answers conj hakukohde-answer dropdown-answer)
                             (update f :content conj hakukohde-question hakukohde-specific-dropdown-with-followups) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "passes validation when a dropdown question is hakukohde specific to wrong hakukohde and has no answers",
      (should (:passed? (validator/valid-application? koodisto-cache has-never-applied
                         (update a :answers conj hakukohde-answer)
                         (update f :content conj hakukohde-question (assoc hakukohde-specific-dropdown-with-followups
                                                                          :belongs-to-hakukohteet
                                                                          ["1.2.246.562.20.352373851711"])) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "fails validation when a dropdown question is hakukohde specific to wrong hakukohde and has answers",
      (should-not (:passed? (validator/valid-application? koodisto-cache has-never-applied
                             (update a :answers conj hakukohde-answer dropdown-answer dropdown-followup-answer)
                             (update f :content conj hakukohde-question (assoc hakukohde-specific-dropdown-with-followups
                                                                               :belongs-to-hakukohteet
                                                                               ["1.2.246.562.20.352373851711"])) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "fails validation when cannot submit multiple applications and has applied"
      (let [has-applied (fn [_ _] (async/go true))
            form (populate-can-submit-multiple-applications f false)
            answers (update a :answers (partial remove #(= "birth-date" (:key %))))]
        (should-not (:passed? (validator/valid-application? koodisto-cache has-applied answers form #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY")))))

  (it "passes validation when cannot submit multiple applications and has not applied"
      (let [has-applied (fn [_ _] (async/go false))
            form (populate-can-submit-multiple-applications f false)
            answers (update a :answers (partial remove #(= "birth-date" (:key %))))]
        (should (:passed? (validator/valid-application? koodisto-cache has-applied answers form #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY")))))

  (it "passes validation when can submit multiple applications and has applied"
      (let [has-applied (fn [_ _] (async/go true))
            form (populate-can-submit-multiple-applications f true)
            answers (update a :answers (partial remove #(= "birth-date" (:key %))))]
        (should (:passed? (validator/valid-application? koodisto-cache has-applied answers form #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY")))))

  (it "passes validation when can submit multiple applications and has not applied"
      (let [has-applied (fn [_ _] (async/go false))
            form (populate-can-submit-multiple-applications f true)
            answers (update a :answers (partial remove #(= "birth-date" (:key %))))]
        (should (:passed? (validator/valid-application? koodisto-cache has-applied answers form #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY")))))

  (it "fails validation when validating required-hakija question with no answer when applying as hakija"
      (should-not (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                          a
                                                          (update f :content conj required-hakija-question)
                                                          #{}
                                                          false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "passes validation when validating required-hakija question with an answer when applying as hakija"
      (should (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                          (update a :answers conj required-hakija-answer)
                                                          (update f :content conj required-hakija-question)
                                                          #{}
                                                          false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "passes validation when validating required-hakija question with no answer when applying as virkailija"
      (should (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                      a
                                                      (update f :content conj required-hakija-question)
                                                      #{}
                                                      true "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "fails validation when validating required per-hakukohde answers without hakukohde"
      (should-not (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                          (update a :answers conj hakukohde-answer (assoc per-hakukohde-specific-dropdown-answer :duplikoitu-kysymys-hakukohde-oid "MISSING_HAKUKOHDE"))
                                                          (update f :content conj hakukohde-question per-hakukohde-specific-dropdown) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "fails validation when validating required per-hakukohde answers with missing value"
      (should-not (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                      (update a :answers conj hakukohde-answer (assoc per-hakukohde-specific-dropdown-answer :value nil))
                                                      (update f :content conj hakukohde-question per-hakukohde-specific-dropdown) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "passes validation when validating required per-hakukohde answers"
      (should (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                          (update a :answers conj hakukohde-answer per-hakukohde-specific-dropdown-answer)
                                                          (update f :content conj hakukohde-question per-hakukohde-specific-dropdown) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "fails validation when validating required per-hakukohde answer followup with missing value"
      (should-not (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                          (update a :answers conj hakukohde-answer per-hakukohde-specific-dropdown-answer (assoc per-hakukohde-specific-followup-answer :value nil))
                                                          (update f :content conj hakukohde-question per-hakukohde-specific-dropdown) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "fails validation when validating required per-hakukohde answer followup with missing hakukohde"
      (should-not (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                          (update a :answers conj hakukohde-answer per-hakukohde-specific-dropdown-answer (assoc per-hakukohde-specific-followup-answer :duplikoitu-followup-hakukohde-oid "MISSING_HAKUKOHDE"))
                                                          (update f :content conj hakukohde-question per-hakukohde-specific-dropdown) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "fails validation when validating required per-hakukohde answer followup with missing parent"
      (should-not (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                          (update a :answers conj hakukohde-answer per-hakukohde-specific-followup-answer)
                                                          (update f :content conj hakukohde-question per-hakukohde-specific-dropdown) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "fails validation when validating required per-hakukohde answer followup with existing parent but different parent option value"
      (should-not (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                          (update a :answers conj hakukohde-answer (assoc per-hakukohde-specific-dropdown-answer :value "Hyvä") per-hakukohde-specific-followup-answer)
                                                          (update f :content conj hakukohde-question per-hakukohde-specific-dropdown) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "fails validation when there are multiple errors"
      (should-not (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                          (update a :answers conj hakukohde-answer (assoc per-hakukohde-specific-dropdown-answer :duplikoitu-followup-hakukohde-oid "MISSING_HAKUKOHDE") (assoc per-hakukohde-specific-followup-answer :duplikoitu-followup-hakukohde-oid "MISSING_HAKUKOHDE"))
                                                          (update f :content conj hakukohde-question per-hakukohde-specific-dropdown) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "passes validation when validating required per-hakukohde followup"
      (should (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                      (update a :answers conj hakukohde-answer per-hakukohde-specific-dropdown-answer per-hakukohde-specific-followup-answer)
                                                      (update f :content conj hakukohde-question per-hakukohde-specific-dropdown) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "passes validation when hakukohteet in answers section and under :hakukohteet are in sync"
      (print (update (update a :answers conj hakukohde-answer) :hakukohde conj (:value hakukohde-answer)))
      (should (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                      (assoc (update a :answers conj hakukohde-answer-ordered) :hakukohde (:value hakukohde-answer-ordered))
                                                      (update f :content conj hakukohde-question) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY"))))

  (it "passes validation when hakukohteet in answers section and under :hakukohteet are not in sync"
      (print (update (update a :answers conj hakukohde-answer) :hakukohde conj (:value hakukohde-answer)))
      (should (not (:passed? (validator/valid-application? koodisto-cache has-never-applied
                                                      (assoc (update a :answers conj hakukohde-answer-ordered) :hakukohde (:value hakukohde-answer-ordered-inverted))
                                                      (update f :content conj hakukohde-question) #{} false "NEW_APPLICATION_ID" "NEW_APPLICATION_KEY")))))


  (it "fails validation when validating fields from oppija-session when locked field has changed"
      (should= '("Hakemus-answer '020202A0202' does not equal session-answer '120997-9998' for key :ssn" "Hakemus-answer 'Eemil' does not equal session-answer 'Timo' for key :first-name")
               (validator/validate-tunnistautunut-oppija-fields answers-by-key base-oppija-session)))

  (it "passes validation when validating fields from oppija-session when locked fields have not changed"
      (should= '() (validator/validate-tunnistautunut-oppija-fields answers-by-key
                                                                    (-> base-oppija-session
                                                                        (assoc-in [:data :fields :first-name] (get-in answers-by-key [:first-name :value]))
                                                                        (assoc-in [:data :fields :ssn] (get-in answers-by-key [:ssn :value]))))))

  (it "passes validation when valid koski tutkinnot"
      (should= {}
               (validator/build-results koodisto-cache
                                        has-never-applied
                                        (-> answers-by-key
                                            (assoc-in [:kk-ylemmat-tutkinto-id :value]
                                                      [["1.2.246.562.10.78305677532_623404_2010-09-20"]])
                                            (assoc-in [:itse-syotetty-tutkinto-nimi :value] [["tutkinto"]])
                                            (assoc-in [:itse-syotetty-koulutusohjelma :value] [["ohjelma"]])
                                            (assoc-in [:itse-syotetty-oppilaitos :value] [["laitos"]])
                                            (assoc-in [:itse-syotetty-valmistumispvm :value] [["11.02.2025"]])
                                            (assoc-in
                                              [(keyword
                                                 (get-in tutkinto-form
                                                         [:content 0 :children 1 :property-options 9 :followups 0 :children 4 :id]))
                                               :value]
                                              [["something.pdf"]])
                                            (assoc-in [:additional-itse-syotetty-field :value] "joo"))
                                        tutkinto-form
                                        (:content tutkinto-form)
                                        flattened-tutkinto-form
                                        #{}
                                        false)))

  (it "passes validation when not mandatory"
      (let [form (assoc-in tutkinto-form [:content 0 :children 1 :mandatory] false)]
        (should= {}
               (validator/build-results koodisto-cache
                                        has-never-applied
                                        answers-by-key
                                        form
                                        (:content form)
                                        (util/flatten-form-fields (:content form))
                                        #{}
                                        false))))

  (it "fails validation when mandatory and no values given"
      (let [tutkintotodistus-id (keyword (get-in tutkinto-form
                                                 [:content 0 :children 1 :property-options 9 :followups 0 :children 4 :id]))]
        (should= {:lukiokoulutus-tutkinto-id nil, :kk-ylemmat-tutkinto-id nil, :kk-alemmat-tutkinto-id nil,
                  :itse-syotetty-tutkinto-nimi nil, :itse-syotetty-oppilaitos nil, :itse-syotetty-valmistumispvm nil,
                  :itse-syotetty-koulutusohjelma nil, tutkintotodistus-id nil, :additional-itse-syotetty-field nil}
                 (validator/build-results koodisto-cache
                                          has-never-applied
                                          answers-by-key
                                          tutkinto-form
                                          (:content tutkinto-form)
                                          flattened-tutkinto-form
                                          #{}
                                          false))))
  (it "fails validation when required values missing"
      (let [tutkintotodistus-id (keyword (get-in tutkinto-form
                                                 [:content 0 :children 1 :property-options 9 :followups 0 :children 4 :id]))]
        (should= {:additional-field-for-kk-alemmat nil, :additional-field-for-lukiokoulutus nil,
                  :itse-syotetty-valmistumispvm nil, :itse-syotetty-koulutusohjelma nil, :itse-syotetty-oppilaitos nil,
                  tutkintotodistus-id nil, :additional-itse-syotetty-field nil,
                  :tohtori-tutkinto-id {:value [["not-allowed-because-level-not-selected"]]},
                  :yo-tutkinto-id {:value [["not-allowed-because-level-not-selected"]]}}
                 (validator/build-results koodisto-cache
                                          has-never-applied
                                          (-> answers-by-key
                                              (assoc-in [:lukiokoulutus-tutkinto-id :value]
                                                        [["1.2.246.562.10.78305677532_623404_2010-09-20"]])
                                              (assoc-in [:kk-alemmat-tutkinto-id :value]
                                                        [["1.2.246.562.10.78305677532_623404_2010-09-20"]])
                                              (assoc-in [:itse-syotetty-tutkinto-nimi :value] [["tutkinto"]])
                                              (assoc-in [:yo-tutkinto-id :value]
                                                        [["not-allowed-because-level-not-selected"]])
                                              (assoc-in [:tohtori-tutkinto-id :value]
                                                        [["not-allowed-because-level-not-selected"]]))
                                          tutkinto-form
                                          (:content tutkinto-form)
                                          flattened-tutkinto-form
                                          #{}
                                          false))))

          )
