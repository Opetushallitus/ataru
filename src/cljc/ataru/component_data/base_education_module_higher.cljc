(ns ataru.component-data.base-education-module-higher
  (:require [ataru.translations.texts :refer [general-texts virkailija-texts]]
            [ataru.translations.education-module-higher-texts :refer [texts]]
            [ataru.component-data.component :as component]
            [ataru.util :as util]
            [ataru.date :refer [current-year current-year-as-str]]))

(def higher-completed-base-education-id "higher-completed-base-education")

(defn- koski-info-notification [metadata]
  (assoc (component/info-element metadata)
    :text (:koski-vocational-info texts)))

(defn- vocational-qualification-text-field [metadata]
  (assoc (component/text-field metadata)
    :label (:vocational-qualification texts)
    :validators ["required"]))

(defn- scope-of-education-text-field [metadata label]
  (assoc (component/text-field metadata)
    :label label
    :params {:size "S" :numeric true :decimals 1}
    :validators ["numeric" "required"]))

(defn- scope-of-education-unit-dropdown [metadata]
  (assoc (component/dropdown metadata)
    :label (:scope-unit texts)
    :options [{:label (:courses texts)
               :value "0"}
              {:label (:ects-credits texts)
               :value "1"}
              {:label (:study-weeks texts)
               :value "2"}
              {:label (:competence-points texts)
               :value "3"}
              {:label (:hours texts)
               :value "4"}
              {:label (:weekly-lessons texts)
               :value "5"}
              {:label (:years texts)
               :value "6"}]
    :validators ["required"]))

(defn- estimated-graduation-date-text-field [metadata]
  (assoc (component/text-field metadata)
    :label (:estimated-graduation-date texts)
    :params {:size "S"}
    :validators ["required"]))

(defn- higher-education-text-field [metadata]
  (assoc (component/text-field metadata)
    :label (:higher-education-institution texts)
    :validators ["required"]))

(defn- education-institution-text-field [metadata]
  (assoc (component/text-field metadata)
    :label (:educational-institution texts)
    :validators ["required"]))

(defn- year-of-completion
  ([id metadata max-value min-value]
   (year-of-completion id metadata max-value min-value (:year-of-completion texts)))
  ([id metadata max-value min-value year-label]
    (assoc (component/text-field metadata)
      :id id
      :label year-label
      :params {:size      "S"
               :numeric   true
               :decimals  nil
               :max-value max-value
               :min-value min-value}
      :validators ["numeric" "required"])))

(defn- name-of-degree [metadata]
  (assoc (component/dropdown metadata)
    :koodisto-source {:uri            "tutkinto"
                      :title          "Tutkinto"
                      :version        2
                      :allow-invalid? true}
    :validators ["required"]
    :options []
    :label (:name-of-degree texts)))

(defn- name-of-degree-text-field [metadata]
  (assoc (component/text-field metadata)
    :label (:name-of-degree texts)
    :validators ["required"]))

(defn- seven-day-attachment-followup
  [id metadata label]
  (assoc (component/attachment metadata)
    :id id
    :params {:hidden    false
             :deadline  nil
             :info-text {:value    (:submit-attachment-7-days texts)
                         :enabled? true}}
    :label label))

(defn- deadline-next-to-request-attachment-followup
  [id metadata label]
  (assoc (component/attachment metadata)
    :id id
    :params {:hidden    false
             :deadline  nil
             :info-text {:value    (:deadline-next-to-attachment texts)
                         :enabled? true}}
    :label label))

(defn- are-your-attachments-in-fi-se-en-followup
  [metadata followups]
  (assoc (component/single-choice-button metadata)
    :label (:are-attachments-in-fi-en-sv texts)
    :params {:hidden false}
    :options [{:label (:yes general-texts)
               :value "0"}
              {:label     (:no general-texts)
               :value     "1"
               :followups followups}]
    :validators ["required"]))

(defn- country-of-completion
  [id metadata params]
  (assoc (component/dropdown metadata)
    :id id
    :params params
    :koodisto-source {:uri            "maatjavaltiot2"
                      :title          "Maat ja valtiot"
                      :version        2
                      :allow-invalid? true}
    :validators ["required"]
    :label (:country-of-completion texts)))

(defn- share-link-followup [metadata]
  (assoc (component/text-field metadata)
    :label (:share-link-to-my-studyinfo texts)
    :params {:size      "L"
             :hidden    false
             :info-text {:label (:how-to-share-link-to-my-studyinfo texts)}}))

(defn- have-you-graduated-with-followups
  [metadata yes-followups no-followups]
  (assoc (component/single-choice-button metadata)
    :label (:have-you-graduated texts)
    :options [{:label     (:yes general-texts)
               :value     "0"
               :followups yes-followups}
              {:label     (:have-not general-texts)
               :value     "1"
               :followups no-followups}]
    :validators ["required"]))

(defn- ammatilliset-oppilaitokset-dropdown
  [metadata label]
  (assoc (component/dropdown metadata)
    :params {:info-text {:label (:select-unknown texts)}}
    :koodisto-source {:uri            "oppilaitostyyppi"
                      :title          "Ammatilliset oppilaitokset"
                      :version        1
                      :allow-invalid? true}
    :validators ["required"]
    :label label))

(defn- finnish-higher-education-option-followups [metadata]
  [(assoc (component/question-group metadata)
     :children [(assoc (year-of-completion "pohjakoulutus_kk--completion-date" metadata (current-year-as-str) "1900")
                  :options [(assoc (component/text-field-conditional-option "0")
                              :condition {:answer-compared-to  (current-year)
                                          :comparison-operator "="}
                              :followups [(have-you-graduated-with-followups metadata
                                                                             [(seven-day-attachment-followup "pohjakoulutus_kk--attachment_transcript" metadata (:transcript-of-records-higher-finland texts))
                                                                              (seven-day-attachment-followup "pohjakoulutus_kk--attachment" metadata (:higher-education-degree-certificate texts))
                                                                              (share-link-followup metadata)]
                                                                             [(estimated-graduation-date-text-field metadata)
                                                                              (seven-day-attachment-followup "pohjakoulutus_kk--attachment_transcript_progress" metadata (:transcript-of-records-higher-finland-in-progress texts))
                                                                              (deadline-next-to-request-attachment-followup "pohjakoulutus_kk--attachment_progress" metadata (:higher-education-degree-certificate-in-progress texts))
                                                                              (share-link-followup metadata)])])
                            (assoc (component/text-field-conditional-option "1")
                              :condition {:answer-compared-to  (current-year)
                                          :comparison-operator "<"}
                              :followups [(seven-day-attachment-followup "pohjakoulutus_kk--attachment_transcript_past" metadata (:transcript-of-records-higher-finland texts))
                                          (seven-day-attachment-followup "pohjakoulutus_kk--attachment_past" metadata (:higher-education-degree-certificate texts))
                                          (share-link-followup metadata)])])
                (assoc (component/dropdown metadata)
                  :koodisto-source {:uri            "kktutkinnot"
                                    :title          "Kk-tutkinnot"
                                    :version        1
                                    :allow-invalid? false}
                  :koodisto-ordered-by-user true           ; TODO: Check order
                  :validators ["required"]
                  :label (:finnish-higher-education-degree-level texts))
                (name-of-degree metadata)
                (higher-education-text-field metadata)
                (assoc (component/info-element metadata)
                  :label (:add-more-qualifications texts))])])

(defn- finnish-vocational-or-special-option-followups [metadata]
  [(assoc (component/question-group metadata)
     :children [(assoc (year-of-completion "pohjakoulutus_amt--year-of-completion" metadata (current-year-as-str) "1900")
                  :options [(assoc (component/text-field-conditional-option "0")
                              :condition {:answer-compared-to  (current-year)
                                          :comparison-operator "="}
                              :followups [(have-you-graduated-with-followups metadata
                                                                             []
                                                                             [(estimated-graduation-date-text-field metadata)
                                                                              (seven-day-attachment-followup "pohjakoulutus_amt--attachment" metadata (:preliminary-certificate-vocational texts))])])
                            (assoc (component/text-field-conditional-option "1")
                              :condition {:answer-compared-to  2018
                                          :comparison-operator "<"}
                              :followups [(seven-day-attachment-followup "pohjakoulutus_amt--attachment_past" metadata (:vocational-or-special-diploma texts))])
                            (assoc (component/text-field-conditional-option "2")
                              :condition {:answer-compared-to  2017
                                          :comparison-operator ">"}
                              :followups [(assoc (component/info-element metadata)
                                            :text (:koski-vocational-special-info texts))])])
                (assoc (component/text-field metadata)
                  :label (:qualification texts)
                  :validators ["required"])
                (scope-of-education-text-field metadata (:scope-of-qualification texts))
                (scope-of-education-unit-dropdown metadata)
                (ammatilliset-oppilaitokset-dropdown metadata (:educational-institution texts))
                (assoc (component/info-element metadata)
                  :label (:add-more-qualifications texts))])])

(defn- vocational-upper-secondary-qualification-option-followups [metadata]
  [(assoc (component/info-element metadata)
     :text (:check-if-really-vocational texts))
   (assoc (component/question-group metadata)
     :children [(assoc (year-of-completion "pohjakoulutus_amp--year-of-completion" metadata (current-year-as-str) "1994")
                  :options [(assoc (component/text-field-conditional-option "1")
                              :condition {:answer-compared-to  (current-year)
                                          :comparison-operator "="}
                              :followups [(have-you-graduated-with-followups metadata []
                                                                             [(estimated-graduation-date-text-field metadata)
                                                                              (seven-day-attachment-followup "pohjakoulutus_amp--attachment" metadata (:preliminary-certificate-vocational-basic texts))])])
                            (assoc (component/text-field-conditional-option "2")
                              :condition {:answer-compared-to  2017
                                          :comparison-operator ">"}
                              :followups [(koski-info-notification metadata)])
                            (assoc (component/text-field-conditional-option "3")
                              :condition {:answer-compared-to  2018
                                          :comparison-operator "<"}
                              :followups [(seven-day-attachment-followup "pohjakoulutus_amp--attachment_past" metadata (:vocational-diploma texts))])])
                (vocational-qualification-text-field metadata)
                (scope-of-education-text-field metadata (:scope-of-vocational-qualification texts))
                (scope-of-education-unit-dropdown metadata)
                (ammatilliset-oppilaitokset-dropdown metadata (:educational-institution texts))
                (assoc (component/info-element metadata)
                  :label (:add-more-qualifications texts))])])

(defn- finnish-matriculation-examination-option-followups [metadata]
  [(assoc (year-of-completion "pohjakoulutus_yo--yes-year-of-completion" metadata (current-year-as-str) "1900")
     :options [(assoc (component/text-field-conditional-option "0")
                 :condition {:answer-compared-to 1990 :comparison-operator "<"}
                 :followups [(seven-day-attachment-followup "pohjakoulutus_yo--attachment" metadata (:matriculation-exam-certificate texts))])
               (assoc (component/text-field-conditional-option "1")
                 :condition {:answer-compared-to 1989 :comparison-operator ">"}
                 :followups [(assoc (component/info-element metadata)
                               :text (:automatic-matriculation-info texts))])])])

(defn- upper-secodary-double-degree-option-followups [metadata]
  [(assoc (component/question-group metadata)
     :children [(assoc (component/info-element metadata)
                  :text (:automatic-matriculation-info texts))
                (assoc (year-of-completion "pohjakoulutus_yo_ammatillinen--vocational-completion-year" metadata (current-year-as-str) "1900" (:year-of-completion-vocational texts))
                  :options [(assoc (component/text-field-conditional-option "0")
                              :condition {:answer-compared-to  (current-year)
                                          :comparison-operator "="}
                              :followups [(have-you-graduated-with-followups metadata []
                                                                             [(estimated-graduation-date-text-field metadata)
                                                                              (seven-day-attachment-followup "pohjakoulutus_yo_ammatillinen--attachment" metadata (:preliminary-certificate-vocational-basic texts))])])
                            (assoc (component/text-field-conditional-option "2")
                              :condition {:answer-compared-to  2018
                                          :comparison-operator "<"}
                              :followups [(seven-day-attachment-followup "pohjakoulutus_yo_ammatillinen--attachment_competence" metadata (:vocational-diploma texts))])
                            (assoc (component/text-field-conditional-option "3")
                              :condition {:answer-compared-to  2017
                                          :comparison-operator ">"}
                              :followups [(koski-info-notification metadata)])])
                (vocational-qualification-text-field metadata)
                (scope-of-education-text-field metadata (:scope-of-basic-vocational-qualification texts))
                (scope-of-education-unit-dropdown metadata)
                (ammatilliset-oppilaitokset-dropdown metadata (:vocational-institution texts))
                (assoc (component/info-element metadata)
                  :label (:add-more-qualifications texts))])])

(defn- gymnasium-without-yo-certificate-option-followups [metadata]
  [(assoc (year-of-completion "pohjakoulutus_lk--year-of-completion" metadata (current-year-as-str) "1900")
     :options [(assoc (component/text-field-conditional-option "0")
                 :condition {:answer-compared-to (current-year) :comparison-operator "="}
                 :followups [(have-you-graduated-with-followups metadata
                                                                [(seven-day-attachment-followup "pohjakoulutus_lk--attachment" metadata (:upper-secondary-school-attachment texts))]
                                                                [(estimated-graduation-date-text-field metadata)
                                                                 (seven-day-attachment-followup "pohjakoulutus_lk--attachment_progress" metadata (:transcript-of-records-secondary-finland texts))])])
               (assoc (component/text-field-conditional-option "1")
                 :condition {:answer-compared-to (current-year) :comparison-operator "<"}
                 :followups [(seven-day-attachment-followup "pohjakoulutus_lk--attachment_past" metadata (:upper-secondary-school-attachment texts))])])])

(defn- international-matriculation-exam-in-finland-option-followups [metadata]
  [(assoc (component/question-group metadata)
     :children [(assoc (year-of-completion "pohjakoulutus_yo_kansainvalinen_suomessa--year-of-completion" metadata (current-year-as-str) "1900")
                  :options [(assoc (component/text-field-conditional-option "0")
                              :condition {:answer-compared-to  (current-year)
                                          :comparison-operator "<"}
                              :followups [(assoc (component/single-choice-button metadata)
                                            :label (:international-matriculation-outside-finland-name texts)
                                            :options [{:label     (:international-baccalaureate texts)
                                                       :value     "0"
                                                       :followups [(seven-day-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_past_ib" metadata (:ib-diploma-finland texts))]}
                                                      {:label     (:european-baccalaureate texts)
                                                       :value     "1"
                                                       :followups [(seven-day-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_past_eb" metadata (:european-baccalaureate-diploma-finland texts))]}
                                                      {:label     (:reifeprufung texts)
                                                       :value     "2"
                                                       :followups [(seven-day-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_past_dia" metadata (:reifeprufung-diploma-finland texts))
                                                                   (seven-day-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_past-equi" metadata (:equivalency-certificate-second texts))]}]
                                            :validators ["required"])])
                            (assoc (component/text-field-conditional-option "1")
                              :condition {:answer-compared-to  (current-year)
                                          :comparison-operator "="}
                              :followups [(assoc (component/single-choice-button metadata)
                                            :label (:matriculation-exam texts)
                                            :options [{:label     (:international-baccalaureate texts)
                                                       :value     "0"
                                                       :followups [(have-you-graduated-with-followups metadata
                                                                                                      [(seven-day-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_ib" metadata (:ib-diploma-finland texts))]
                                                                                                      [(estimated-graduation-date-text-field metadata)
                                                                                                       (seven-day-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_grades_ib" metadata (:predicted-grades-ib-finland texts))
                                                                                                       (deadline-next-to-request-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_progress_ib" metadata (:diploma-programme-ib-finland texts))])]}
                                                      {:label     (:european-baccalaureate texts)
                                                       :value     "1"
                                                       :followups [(have-you-graduated-with-followups metadata
                                                                                                      [(seven-day-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_eb" metadata (:european-baccalaureate-diploma-finland texts))]
                                                                                                      [(estimated-graduation-date-text-field metadata)
                                                                                                       (seven-day-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_grades_eb" metadata (:predicted-grades-eb-finland texts))
                                                                                                       (deadline-next-to-request-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_progress_eb" metadata (:diploma-programme-eb-finland texts))])]}
                                                      {:label     (:abiturprufung texts)
                                                       :value     "2"
                                                       :followups [(have-you-graduated-with-followups metadata
                                                                                                      [(seven-day-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_dia" metadata (:dia-diploma-finland texts))
                                                                                                       (seven-day-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_equi" metadata (:equivalency-certificate-second-dia texts))]
                                                                                                      [(estimated-graduation-date-text-field metadata)
                                                                                                       (seven-day-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_grades_dia" metadata (:grade-page-dia-finland texts))
                                                                                                       (deadline-next-to-request-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_progress_dia" metadata (:dia-diploma-finland texts))
                                                                                                       (seven-day-attachment-followup "pohjakoulutus_yo_kansainvalinen_suomessa--attachment_progress_equi" metadata (:equivalency-certificate-second-dia texts))])]}]
                                            :validators ["required"])])])
                (education-institution-text-field metadata)
                (assoc (component/info-element metadata)
                  :label (:add-more-qualifications texts))])])

(defn- vocational-upper-secondary-qualification-finland-option-followups [metadata]
  [(assoc (component/info-element metadata)
     :text (:check-if-really-opistoaste texts))
   (assoc (component/question-group metadata)
     :children [(year-of-completion "pohjakoulutus_amv--year-of-completion" metadata "2005" "1900")
                (assoc (component/dropdown metadata)
                  :label (:type-of-vocational texts)
                  :params {:hidden false}
                  :options [{:label     (:kouluaste-of-vocational-qualification texts)
                             :value     "0"
                             :followups [(assoc (component/info-element metadata)
                                           :text (:check-if-really-kouluaste texts))]}
                            {:label     (:vocational-opistoaste-qualification texts)
                             :value     "1"
                             :followups [(assoc (component/info-element metadata)
                                           :text (:check-if-really-post-secondary texts))]}
                            {:label     (:vocational-korkea-aste-qualification texts)
                             :value     "2"
                             :followups [(assoc (component/info-element metadata)
                                           :text (:check-if-really-higher-vocational texts))]}]
                  :validators ["required"])
                (vocational-qualification-text-field metadata)
                (scope-of-education-text-field metadata (:scope-of-vocational-qualification texts))
                (scope-of-education-unit-dropdown metadata)
                (ammatilliset-oppilaitokset-dropdown metadata (:educational-institution texts))
                (seven-day-attachment-followup "pohjakoulutus_amv--attachment" metadata (:vocational-qualification-diploma texts))
                (assoc (component/info-element metadata)
                  :label (:add-more-qualifications texts))])])

(defn- upper-secondary-qualification-not-finland-option-followups [metadata]
  [(assoc (component/question-group metadata)
     :children [(assoc (year-of-completion "pohjakoulutus_ulk--year-of-completion" metadata (current-year-as-str) "1900")
                  :options [(assoc (component/text-field-conditional-option "0")
                              :condition {:answer-compared-to  (current-year)
                                          :comparison-operator "="}
                              :followups [(have-you-graduated-with-followups metadata
                                                                             [(seven-day-attachment-followup "pohjakoulutus_ulk--attachment" metadata (:upper-secondary-education-diploma texts))
                                                                              (assoc (component/single-choice-button metadata)
                                                                                :label (:diploma-in-fi-sv-en texts)
                                                                                :params {:hidden false}
                                                                                :options [{:label (:yes general-texts)
                                                                                           :value "0"}
                                                                                          {:label     (:no general-texts)
                                                                                           :value     "1"
                                                                                           :followups [(seven-day-attachment-followup "pohjakoulutus_ulk--attachment_translation" metadata (:translation-of-diploma texts))]}]
                                                                                :validators ["required"])]
                                                                             [(estimated-graduation-date-text-field metadata)
                                                                              (seven-day-attachment-followup "pohjakoulutus_ulk--attachment_transcript" metadata (:transcript-of-records-upper-secondary texts))
                                                                              (deadline-next-to-request-attachment-followup "pohjakoulutus_ulk--attachment_progress" metadata (:original-upper-secondary-diploma texts))
                                                                              (are-your-attachments-in-fi-se-en-followup metadata [(seven-day-attachment-followup "pohjakoulutus_ulk--attachment_transcript_translation" metadata (:translation-of-study-records texts))
                                                                                                                                   (deadline-next-to-request-attachment-followup "pohjakoulutus_ulk--attachment_progress_translation" metadata (:translation-of-diploma texts))])])])
                            (assoc (component/text-field-conditional-option "1")
                              :condition {:answer-compared-to  (current-year)
                                          :comparison-operator "<"}
                              :followups [(seven-day-attachment-followup "pohjakoulutus_ulk--attachment_past" metadata (:upper-secondary-education-diploma texts))
                                          (are-your-attachments-in-fi-se-en-followup metadata [(seven-day-attachment-followup "pohjakoulutus_ulk--attachment_past_translation" metadata (:translation-of-diploma texts))])])])
                (name-of-degree-text-field metadata)
                (education-institution-text-field metadata)
                (country-of-completion "pohjakoulutus_ulk-country" metadata {})
                (assoc (component/info-element metadata)
                  :label (:add-more-qualifications texts))])])

(defn- internation-matriculation-examination-option-followups [metadata]
  [(assoc (component/question-group metadata)
     :children [(assoc (year-of-completion "pohjakoulutus_yo_ulkomainen--year-of-completion" metadata (current-year-as-str) "1900")
                  :options [(assoc (component/text-field-conditional-option "0")
                              :condition {:answer-compared-to  (current-year)
                                          :comparison-operator "<"}
                              :followups [(assoc (component/single-choice-button metadata)
                                            :label (:matriculation-exam texts)
                                            :options [{:label     (:international-baccalaureate texts)
                                                       :value     "0"
                                                       :followups [(seven-day-attachment-followup "pohjakoulutus_yo_ulkomainen--attachment_past_ib" metadata (:ib-diploma texts))]}
                                                      {:label     (:european-baccalaureate texts)
                                                       :value     "1"
                                                       :followups [(seven-day-attachment-followup "pohjakoulutus_yo_ulkomainen--attachment_past_eb" metadata (:european-baccalaureate-diploma texts))]}
                                                      {:label     (:reifeprufung texts)
                                                       :value     "2"
                                                       :followups [(seven-day-attachment-followup "pohjakoulutus_yo_ulkomainen--attachment_past_dia" metadata (:reifeprufung-diploma texts))]}]
                                            :validators ["required"])])
                            (assoc (component/text-field-conditional-option "1")
                              :condition {:answer-compared-to  (current-year)
                                          :comparison-operator "="}
                              :followups [(assoc (component/single-choice-button metadata)
                                            :label (:matriculation-exam texts)
                                            :options [{:label     (:international-baccalaureate texts)
                                                       :value     "0"
                                                       :followups [(have-you-graduated-with-followups metadata
                                                                                                      [(seven-day-attachment-followup "pohjakoulutus_yo_ulkomainen--attachment_ib" metadata (:ib-diploma texts))]
                                                                                                      [(estimated-graduation-date-text-field metadata)
                                                                                                       (deadline-next-to-request-attachment-followup "pohjakoulutus_yo_ulkomainen--attachment_grades_ib" metadata (:predicted-grades-ib texts))
                                                                                                       (deadline-next-to-request-attachment-followup "pohjakoulutus_yo_ulkomainen--attachment_progress_ib" metadata (:diploma-programme-ib texts))])]}
                                                      {:label     (:european-baccalaureate texts)
                                                       :value     "1"
                                                       :followups [(have-you-graduated-with-followups metadata
                                                                                                      [(seven-day-attachment-followup "pohjakoulutus_yo_ulkomainen--attachment_eb" metadata (:european-baccalaureate-diploma texts))]
                                                                                                      [(estimated-graduation-date-text-field metadata)
                                                                                                       (deadline-next-to-request-attachment-followup "pohjakoulutus_yo_ulkomainen--attachment_grades_eb" metadata (:predicted-grades-eb texts))
                                                                                                       (deadline-next-to-request-attachment-followup "pohjakoulutus_yo_ulkomainen--attachment_progress_eb" metadata (:european-baccalaureate-diploma texts))])]}
                                                      {:label     (:abiturprufung texts)
                                                       :value     "2"
                                                       :followups [(have-you-graduated-with-followups metadata
                                                                                                      [(seven-day-attachment-followup "pohjakoulutus_yo_ulkomainen--attachment_dia" metadata (:dia-diploma-completed texts))]
                                                                                                      [(estimated-graduation-date-text-field metadata)
                                                                                                       (deadline-next-to-request-attachment-followup "pohjakoulutus_yo_ulkomainen--attachment_grades_dia" metadata (:grade-page-dia texts))
                                                                                                       (deadline-next-to-request-attachment-followup "pohjakoulutus_yo_ulkomainen--attachment_progress_dia" metadata (:dia-diploma texts))])]}]
                                            :validators ["required"])])])
                (education-institution-text-field metadata)
                (country-of-completion "pohjakoulutus_yo-country" metadata {})
                (assoc (component/info-element metadata)
                  :label (:add-more-qualifications texts))])])

(defn- non-finnish-higher-education-option-followups [metadata]
  [(assoc (component/question-group metadata)
     :children [(assoc (year-of-completion "pohjakoulutus_kk_ulk--year-of-completion" metadata (current-year-as-str) "1900")
                  :options [(assoc (component/text-field-conditional-option "0")
                              :condition {:answer-compared-to  (current-year)
                                          :comparison-operator "="}
                              :followups [(have-you-graduated-with-followups metadata
                                                                             [(seven-day-attachment-followup "pohjakoulutus_kk_ulk--attachment_transcript" metadata (:transcript-of-records-higher texts))
                                                                              (seven-day-attachment-followup "pohjakoulutus_kk_ulk--attachment" metadata (:higher-education-degree-certificate texts))
                                                                              (are-your-attachments-in-fi-se-en-followup metadata [(seven-day-attachment-followup "pohjakoulutus_kk_ulk--attachment_translation" metadata (:translation-of-certificate texts))])]
                                                                             [(estimated-graduation-date-text-field metadata)
                                                                              (seven-day-attachment-followup "pohjakoulutus_kk_ulk--attachment_transcript_progress" metadata (:transcript-of-records-in-progress texts))
                                                                              (deadline-next-to-request-attachment-followup "pohjakoulutus_kk_ulk--attachment_progress" metadata (:higher-education-degree-certificate-alien-in-progress texts))
                                                                              (are-your-attachments-in-fi-se-en-followup metadata [(seven-day-attachment-followup "pohjakoulutus_kk_ulk--attachment_transcript_progress_translation" metadata (:translation-of-transcript-of-records texts))
                                                                                                                                   (deadline-next-to-request-attachment-followup "pohjakoulutus_kk_ulk--attachment_progress_translation" metadata (:translation-of-degree-higher texts))])])])
                            (assoc (component/text-field-conditional-option "1")
                              :condition {:answer-compared-to  (current-year)
                                          :comparison-operator "<"}
                              :followups [(seven-day-attachment-followup "pohjakoulutus_kk_ulk--attachment_transcript_past" metadata (:transcript-of-records-higher texts))
                                          (seven-day-attachment-followup "pohjakoulutus_kk_ulk--attachment_past" metadata (:higher-education-degree-certificate-alien texts))
                                          (are-your-attachments-in-fi-se-en-followup metadata [(seven-day-attachment-followup "pohjakoulutus_kk_ulk--attachment_translation_past" metadata (:translation-of-certificate texts))])])])
                (name-of-degree-text-field metadata)
                (assoc (component/dropdown metadata)
                  :koodisto-source {:uri            "kktutkinnot"
                                    :title          "Kk-tutkinnot"
                                    :version        1
                                    :allow-invalid? false}
                  :koodisto-ordered-by-user true            ; TODO: check if this is required then order options
                  :validators ["required"]
                  :label (:finnish-higher-education-degree-level texts))
                (higher-education-text-field metadata)
                (country-of-completion "pohjakoulutus_kk_ulk-country" metadata {})
                (assoc (component/info-element metadata)
                  :label (:add-more-qualifications texts))])])

(defn- open-university-option-followups [metadata]
  [(assoc (component/question-group metadata)
     :children [(year-of-completion "pohjakoulutus_avoin--year-of-completion" metadata (current-year-as-str) "1900")
                (assoc (component/text-field metadata)
                  :label {:en "Study field" :fi "Ala" :sv "Bransch"}
                  :validators ["required"])
                (higher-education-text-field metadata)
                (assoc (component/text-field metadata)
                  :label (:module texts)
                  :validators ["required"])
                (assoc (component/text-field metadata)
                  :label (:scope-of-studies texts)
                  :validators ["required"])
                (seven-day-attachment-followup "pohjakoulutus_avoin--attachment" metadata (:certificate-open-studies texts))
                (assoc (component/info-element metadata)
                  :label (:add-more-studies texts))])])

(defn- other-eligibility-option-followups [metadata]
  [(assoc (component/question-group metadata)
     :children [(year-of-completion "pohjakoulutus_muu--year-of-completion" metadata (current-year-as-str) "1900")
                (assoc (component/text-area metadata)
                  :label (:base-education-other-description texts)
                  :params {:max-length "500"}
                  :validators ["required"])
                (seven-day-attachment-followup "pohjakoulutus_muu--attachment" metadata (:other-eligibility-attachment texts))
                (assoc (component/info-element metadata)
                  :label (:add-more-wholes texts))])])

(defn- education-question [metadata]
  (assoc (component/multiple-choice metadata)
    :id higher-completed-base-education-id
    :params {:hidden    false
             :info-text {:label (:read-who-can-apply texts)}}
    :koodisto-source {:uri            "pohjakoulutuskklomake"
                      :title          "Kk-pohjakoulutusvaihtoehdot"
                      :version        2
                      :allow-invalid? false}
    :koodisto-ordered-by-user true
    :validators ["required"]
    :label (:completed-education texts)
    :options [{:label     (:pohjakoulutus_yo virkailija-texts)
               :value     "pohjakoulutus_yo"
               :followups (finnish-matriculation-examination-option-followups metadata)}
              {:label     (:pohjakoulutus_amp virkailija-texts)
               :value     "pohjakoulutus_amp"
               :followups (vocational-upper-secondary-qualification-option-followups metadata)}
              {:label     (:pohjakoulutus_amt virkailija-texts)
               :value     "pohjakoulutus_amt"
               :followups (finnish-vocational-or-special-option-followups metadata)}
              {:label     (:pohjakoulutus_kk virkailija-texts)
               :value     "pohjakoulutus_kk"
               :followups (finnish-higher-education-option-followups metadata)}
              {:label     (:pohjakoulutus_yo_ammatillinen virkailija-texts)
               :value     "pohjakoulutus_yo_ammatillinen"
               :followups (upper-secodary-double-degree-option-followups metadata)}
              {:label     (:pohjakoulutus_lk virkailija-texts)
               :value     "pohjakoulutus_lk"
               :followups (gymnasium-without-yo-certificate-option-followups metadata)}
              {:label     (:pohjakoulutus_yo_kansainvalinen_suomessa virkailija-texts)
               :value     "pohjakoulutus_yo_kansainvalinen_suomessa"
               :followups (international-matriculation-exam-in-finland-option-followups metadata)}
              {:label     (:pohjakoulutus_amv virkailija-texts)
               :value     "pohjakoulutus_amv"
               :followups (vocational-upper-secondary-qualification-finland-option-followups metadata)}
              {:label     (:pohjakoulutus_ulk virkailija-texts)
               :value     "pohjakoulutus_ulk"
               :followups (upper-secondary-qualification-not-finland-option-followups metadata)}
              {:label     (:pohjakoulutus_yo_ulkomainen virkailija-texts)
               :value     "pohjakoulutus_yo_ulkomainen"
               :followups (internation-matriculation-examination-option-followups metadata)}
              {:label     (:pohjakoulutus_kk_ulk virkailija-texts)
               :value     "pohjakoulutus_kk_ulk"
               :followups (non-finnish-higher-education-option-followups metadata)}
              {:label     (:pohjakoulutus_avoin virkailija-texts)
               :value     "pohjakoulutus_avoin"
               :followups (open-university-option-followups metadata)}
              {:label     (:pohjakoulutus_muu virkailija-texts)
               :value     "pohjakoulutus_muu"
               :followups (other-eligibility-option-followups metadata)}]))

(defn- education-statistics-question [metadata]
  (assoc (component/single-choice-button metadata)
    :id "secondary-completed-base-education"
    :label (:have-you-completed texts)
    :params {:info-text {:label (:required-for-statistics texts)}}
    :options [{:label     (:yes general-texts)
               :value     "0"
               :followups [(country-of-completion "secondary-completed-base-education–country" metadata {:info-text {:label (:choose-country-of-latest-qualification texts)}})]}
              {:label (:have-not general-texts)
               :value "1"}]
    :validators ["required"]
    ))

(defn- name-of-higher-education-institution [metadata]
  (assoc (component/text-field metadata)
    :label (:higher-education-institution texts)
    :validators ["required"]))

(defn- education-question-before-2003 [metadata]
  (assoc (component/single-choice-button metadata)
    :id "finnish-vocational-before-1995"
    :label (:have-you-completed-before-2003 texts)
    :params {:info-text
             {:label (:write-completed-before-2003 texts)}}
    :options [{:label     (:yes general-texts)
               :value     "0"
               :followups [(year-of-completion "finnish-vocational-before-1995--year-of-completion" metadata "2002" "1900")
                           (name-of-degree metadata)
                           (name-of-higher-education-institution metadata)]}
              {:label (:have-not general-texts)
               :value "1"}]
    :validators ["required"]))

(defn base-education-module-higher [metadata]
  (assoc (component/form-section metadata)
    :id "higher-base-education-module"
    :label (:educational-background texts)
    :children [(education-question metadata)
               (education-statistics-question metadata)
               (education-question-before-2003 metadata)]))

(def higher-education-base-education-questions
  (->> (base-education-module-higher {})
       :children
       util/flatten-form-fields
       (map (comp name :id))
       set))

(defn non-yo-attachment-ids
  [form]
  (->> (util/find-field (:content form) higher-completed-base-education-id)
       :options
       (remove #(contains? #{"pohjakoulutus_yo"
                             "pohjakoulutus_yo_ammatillinen"
                             "pohjakoulutus_yo_kansainvalinen_suomessa"
                             "pohjakoulutus_yo_ulkomainen"}
                           (:value %)))
       (mapcat :followups)
       util/flatten-form-fields
       (filter #(= "attachment" (:fieldType %)))
       (map :id)
       set))