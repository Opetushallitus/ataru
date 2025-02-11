(ns ataru.fixtures.form
  (:require [ataru.component-data.component :as component]
            [ataru.component-data.base-education-module-higher :refer [base-education-module-higher]]
            [ataru.component-data.person-info-module :as person-info-module]
            [ataru.component-data.kk-application-payment-module :as payment-module]
            [ataru.component-data.koski-tutkinnot-module :as ktm]
            [clojure.string :as string]))

(def metadata {:created-by  {:oid  "1.2.246.562.24.1000000"
                             :date "2018-03-21T15:45:29.23+02:00"
                             :name "Teppo Testinen"}
               :modified-by {:oid  "1.2.246.562.24.1000000"
                             :date "2018-03-22T07:55:08Z"
                             :name "Teppo Testinen"}})

(defn get-names [content]
  (map #(get-in % [:label :fi]) content))

(defn create-element [name]
  {:id         name
   :label      {:fi name}
   :metadata   metadata
   :fieldType  "textField"
   :fieldClass "formField"})

(defn create-form [& elements]
  {:name       {:fi (string/join "" (get-names elements))}
   :created-by "1.2.246.562.11.11111111111"
   :locked     nil
   :locked-by  nil
   :content    elements})

(defn create-wrapper-element [& elements]
  (let [name (str "W" (string/join "" (get-names elements)))]
    {:id         name
     :label      {:fi name}
     :metadata   metadata
     :children   elements
     :fieldType  "fieldset"
     :fieldClass "questionGroup"}))

(def minimal-form
  (merge (create-form)
         {:organization-oid "1.2.246.562.10.0439845"
          :id 5}))

(def form-with-content
  {:name       {:fi "Test fixture!"}
   :created-by "1.2.246.562.11.11111111111"
   :locked     nil
   :locked-by  nil
   :content    [(component/hakukohteet)
                {:fieldClass "formField"
                 :metadata   metadata
                 :label      {:fi "tekstiä" :sv ""}
                 :id         "G__19"
                 :fieldType  "textField"}
                {:fieldClass "wrapperElement"
                 :metadata   metadata
                 :fieldType  "fieldset"
                 :id         "G__31"
                 :label      {:fi "Osion nimi" :sv "Avsnitt namn"}
                 :children   [{:fieldClass "formField"
                               :metadata   metadata
                               :label      {:fi "" :sv ""}
                               :id         "G__32"
                               :fieldType  "textField"}]}]})

(def person-info-form
  {:id               2147483647                             ;; shouldn't clash with serial sequence id. Tests also create forms which use serial id, and the previous id 15 caused serious issues.
   :key              "41101b4f-1762-49af-9db0-e3603adae3ae"
   :name             {:fi "Uusi lomake"}
   :created-by       "1.2.246.562.11.11111111111"
   :organization-oid "1.2.246.562.10.2.45"
   :created-time     "2016-07-28T09:58:34.217+03:00"
   :locked           nil
   :locked-by        nil
   :content          [(component/hakukohteet)
                      (person-info-module/person-info-module)
                      (payment-module/kk-application-payment-module)
                      {:fieldClass "wrapperElement"
                       :metadata   metadata
                       :fieldType  "fieldset"
                       :id         "6a3bd67e-a4ec-436e-9d70-c107df28932b"
                       :label      {:fi "Toinen osio" :sv "Avsnitt namn"}
                       :children   [{:fieldClass "formField"
                                     :metadata   metadata
                                     :fieldType  "textField"
                                     :label      {:fi "Tekstikysymys" :sv ""}
                                     :id         "b0839467-a6e8-4294-b5cc-830756bbda8a"
                                     :params     {}
                                     :validators ["required"]}]
                       :params     {}}
                      {:label                  {:fi "Eka liite"
                                                :sv ""}
                       :fieldClass             "formField"
                       :metadata               metadata
                       :belongs-to-hakukohteet ["1.2.246.562.20.49028196523" "1.2.246.562.20.49028196524"]
                       :id                     "164954b5-7b23-4774-bd44-dee14071316b"
                       :params                 {}
                       :options                []
                       :fieldType              "attachment"}
                      {:fieldClass       "formField"
                       :fieldType        "textArea"
                       :metadata         metadata
                       :label            {:fi "Salainen kysymys" :sv ""}
                       :id               "87834771-34da-40a4-a9f6-sensitive"
                       :sensitive-answer true}]})

(def form-with-followup-inside-a-question-group
  {:id               2147483646
   :key              "41101b4f-1762-49af-9db0-e3603adae3af"
   :name             {:fi "Uusi lomake"}
   :created-by       "1.2.246.562.11.11111111111"
   :organization-oid "1.2.246.562.10.2.45"
   :created-time     "2016-07-28T09:58:34.217+03:00"
   :locked           nil
   :locked-by        nil
   :content          [(component/hakukohteet)
                      (person-info-module/person-info-module)
                      (assoc (component/question-group metadata)
                             :children [(assoc (component/single-choice-button metadata)
                                               :id "choice"
                                               :options [{:value     "0"
                                                          :label     {:fi "A"}
                                                          :followups [(assoc (component/text-field metadata)
                                                                             :id "text")]}
                                                         {:value     "1"
                                                          :label     {:fi "B"}
                                                          :followups []}])])]})

(def form-hidden-attachment [{:label                  {:fi "Toka liite"
                                                      :sv ""}
                             :fieldClass             "formField"
                             :metadata               metadata
                             :belongs-to-hakukohteet ["1.2.246.562.20.49028196523" "1.2.246.562.20.49028196524"]
                             :id                     "164954b5-7b23-4774-bd44-hidden"
                             :params                 {:hidden true}
                             :options                []
                             :fieldType              "attachment"}])

(def more-questions
  [{:fieldClass "formField"
    :metadata   metadata
    :fieldType  "attachment"
    :id         "more-questions-attachment-id"
    :label      {:fi "Eka liite" :sv ""}
    :validators ["required"]}
   {:fieldClass "formField"
    :metadata   metadata
    :fieldType  "dropdown"
    :id         "more-answers-dropdown-id"
    :label      {:fi "droparii" :sv ""}
    :options    [{:label     {:fi "eka vaihtoehto" :sv ""}
                  :value     "eka vaihtoehto"
                  :followups [{:fieldClass "formField"
                               :metadata   metadata
                               :fieldType  "attachment"
                               :id         "dropdown-followup-1"
                               :label      {:fi "Dropdown liite" :sv ""}}
                              {:fieldClass "formField"
                               :metadata   metadata
                               :fieldType  "singleChoice"
                               :id         "dropdown-followup-2"
                               :label      {:fi "Dropdown painikkeet required" :sv ""}
                               :validators ["required"]
                               :options    [{:label {:fi "eka" :sv ""}
                                             :value "eka"}
                                            {:label {:fi "toka" :sv ""}
                                             :value "toka"}]}]}
                 {:label {:fi "toka vaihtoehto" :sv ""}
                  :value "toka vaihtoehto"}]}
   {:fieldClass "wrapperElement"
    :metadata   metadata
    :fieldType  "adjacentfieldset"
    :id         "adjacent-quesiton-id"
    :label      {:fi "vierekkäiset kentät"}
    :children   [{:fieldClass "formField"
                  :metadata   metadata
                  :fieldType  "textField"
                  :id         "adjacent-answer-1"
                  :label      {:fi "Vierekkäinen Kenttä1" :sv ""}
                  :params     {:adjacent true}
                  :validators ["required"]}
                 {:fieldClass "formField"
                  :metadata   metadata
                  :fieldType  "textField"
                  :id         "adjacent-answer-2"
                  :label      {:fi "Vierekkäinen Kenttä2" :sv ""}
                  :params     {:adjacent true}}]}
   {:fieldClass "formField"
    :metadata   metadata
    :fieldType  "textField"
    :id         "repeatable-required"
    :label      {:fi "Toistuva pakollinen" :sv ""}
    :params     {:repeatable true}
    :validators ["required"]}])

(def person-info-form-with-more-questions
  (update person-info-form :content concat more-questions))

(def version-test-form
  {:name       {:fi "Test fixture!"}
   :created-by "1.2.246.562.11.11111111111"
   :locked     nil
   :locked-by  nil
   :content    [(component/hakukohteet)
                {:id         "G__119"
                 :fieldClass "formField"
                 :metadata   metadata
                 :fieldType  "textField"
                 :label      {:fi "Eka kysymys"}}
                {:id         "G__117"
                 :fieldClass "formField"
                 :metadata   metadata
                 :fieldType  "textField"
                 :label      {:fi "Toistuva kysymys"}
                 :params     {:repeatable true}}
                {:id         "G__224"
                 :fieldClass "formField"
                 :metadata   metadata
                 :fieldType  "textField"
                 :label      {:fi "Toistuva kysymys ryhmässä"}
                 :params     {:repeatable true}}]})

(def attachment-test-form
  {:name       {:fi "Attachment test form"}
   :created-by "1.2.246.562.11.11111111111"
   :locked     nil
   :locked-by  nil
   :content    [(component/hakukohteet)
                {:id         "att__1"
                 :fieldClass "formField"
                 :metadata   metadata
                 :fieldType  "attachment"
                 :label      {:fi "Liite"}}
                {:id         "att__2"
                 :fieldClass "formField"
                 :metadata   metadata
                 :fieldType  "attachment"
                 :label      {:fi "Liite ilman vastausta"}}]})

(def visible-attachment-test-form
  {:name       {:fi "Visible attachment test form"}
   :created-by "1.2.246.562.11.11111111111"
   :locked     nil
   :locked-by  nil
   :content    [(component/hakukohteet)
                (assoc-in (ktm/koski-tutkinnot-module metadata)
                          [:children 1 :property-options 9 :followups 0 :children 4 :id]
                          "itse-syotetty-tutkintotodistus")
                (assoc (component/single-choice-button metadata)
                       :id "choice_1"
                       :options [{:value     "0"
                                  :followups [(assoc (component/question-group metadata)
                                                     :id "group_1"
                                                     :children [(assoc (component/attachment metadata)
                                                                       :id "attachment_1")])]}
                                 {:value     "1"
                                  :followups [(assoc (component/question-group metadata)
                                                     :id "group_2"
                                                     :children [(assoc (component/attachment metadata)
                                                                       :id "attachment_2")])]}
                                 {:value     "2"
                                  :followups [(assoc (component/attachment metadata)
                                                     :id "attachment_3")]}])]})

(def base-education-attachment-test-form
  {:name       {:fi "Attachment and Base Education test form"}
   :created-by "1.2.246.562.11.11111111111"
   :content    [(component/hakukohteet)
                (person-info-module/person-info-module)
                (base-education-module-higher metadata)
                {:id         "att__1"
                 :fieldClass "formField"
                 :metadata   metadata
                 :fieldType  "attachment"
                 :label      {:fi "Liite"}}
                {:id         "att__2"
                 :fieldClass "formField"
                 :metadata   metadata
                 :fieldType  "attachment"
                 :label      {:fi "Liite ilman vastausta"}}]})

(def form-with-koodisto-source
  {:id 981230123
   :name {:fi "Test fixture for options vs koodisto"}
   :key "koodisto-test-form"
   :created-by       "1.2.246.562.11.11111111111"
   :organization-oid "1.2.246.562.10.2.45"
   :created-time     "2016-07-28T09:58:34.217+03:00"
   :locked           nil
   :locked-by        nil
   :content [{:id "kysymys_1",
              :options [{:value "2"},
                        {:value "1"},
                        {:value "4"},
                        {:value "3"},
                        {:value "5"}],
              :metadata metadata,
              :fieldtype "dropdown",
              :fieldClass "formField",
              :validators ["required"],
              :koodisto-source {:uri "kktutkinnot",
                                :title "Kk-tutkinnot",
                                :version 1,
                                :allow-invalid? false}}]})

(def synthetic-application-test-form
  {:id         808808
   :key        "synthetic-application-test-form"
   :name       {:fi "Synthetic application test form"}
   :locked     nil
   :locked-by  nil
   :created-by "1.2.246.562.11.11111111111"
   :organization-oid "1.2.246.562.10.0439845"
   :content    [(component/hakukohteet)
                (person-info-module/person-info-module)
                {:id "secondary-completed-base-education"
                 :metadata metadata
                 :label {:en "Have you completed general upper secondary education or a vocational qualification?"
                         :fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon?"
                         :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"}
                 :options [{:label {:en "Yes" :fi "Kyllä" :sv "Ja"}
                            :value "0"
                            :followups [{:id "secondary-completed-base-education–country"
                                         :metadata metadata
                                         :label {:en "Country of completion" :fi "Suoritusmaa" :sv "Land där examen är avlagd"}
                                         :fieldType "dropdown"
                                         :fieldClass "formField"
                                         :validators ["required"],
                                         :koodisto-source {:uri "maatjavaltiot2" :title "Maat ja valtiot" :version 2 :allow-invalid? true}}]}
                           {:label {:en "No" :fi "En" :sv "Nej"}
                            :value "1"}],
                 :fieldType "singleChoice"
                 :fieldClass "formField"
                 :validators ["required"]}
                {:id "asiointikieli"
                 :metadata metadata
                 :label {:en "Contact language" :fi "Asiointikieli" :sv "Ärendespråk"}
                 :options [{:label {:en "Finnish" :fi "Suomi" :sv "Finska"}
                            :value "1"}
                           {:label {:en "Swedish" :fi "Ruotsi" :sv "Svenska"}
                            :value "2"}
                           {:label {:en "English" :fi "Englanti" :sv "Engelska"}
                            :value "3"}],
                 :fieldType "dropdown"
                 :fieldClass "formField"
                 :validators ["required"]}]})

(def payment-properties-test-form
  (merge minimal-form
         {:key "payment-properties-test-form"}))

(def payment-exemption-test-form
  (merge minimal-form
         {:id  909909
          :key "payment-exemption-test-form"}))

(def tutkinto-test-form
  (merge minimal-form
         (-> {:content [(ktm/koski-tutkinnot-module metadata)]}
             (assoc-in [:content 0 :children 1 :property-options 1 :followups 0 :children 1]
                       {:params {:question-group-id :lukiokoulutus-question-group},
                        :validators ["required"],
                        :fieldClass "formField", :fieldType "textField", :cannot-edit false, :cannot-view false,
                        :label {:fi "Miten menee??", :sv ""},
                        :id "additional-field-for-lukiokoulutus"})
             (assoc-in [:content 0 :children 1 :property-options 6 :followups 0 :children 1]
                       {:params {:question-group-id :kk-alemmat-question-group},
                        :validators ["required"],
                        :fieldClass "formField", :fieldType "textField", :cannot-edit false, :cannot-view false,
                        :label {:fi "Miten menee??", :sv ""},
                        :id "additional-field-for-kk-alemmat"}))))
