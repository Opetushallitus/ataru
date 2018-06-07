(ns ataru.fixtures.form
  (:require [ataru.db.migrations :as migrations]
            [ataru.component-data.component :as component]))

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
  {:name       {:fi (clojure.string/join "" (get-names elements))}
   :created-by "DEVELOPER"
   :locked     nil
   :locked-by  nil
   :content    elements})

(defn create-wrapper-element [& elements]
  (let [name (str "W" (clojure.string/join "" (get-names elements)))]
    {:id         name
     :label      {:fi name}
     :metadata   metadata
     :children   elements
     :fieldType  "fieldset"
     :fieldClass "questionGroup"}))

(def form-with-content
  {:name       {:fi "Test fixture!"}
   :created-by "DEVELOPER"
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
   :created-by       "DEVELOPER"
   :organization-oid "1.2.246.562.10.2.45"
   :created-time     "2016-07-28T09:58:34.217+03:00"
   :locked           nil
   :locked-by        nil
   :content          [(component/hakukohteet)
                      (ataru.component-data.person-info-module/person-info-module)
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
                       :fieldType              "attachment"}]})

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
   :created-by "DEVELOPER"
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
   :created-by "DEVELOPER"
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
