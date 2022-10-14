(ns ataru.component-data.base-education-module-kk
  (:require [ataru.translations.texts :refer [general-texts virkailija-texts]]
            [ataru.translations.education-module-higher-texts :refer [texts]]
            [ataru.component-data.component :as component]))

(defn- koski-info-notification [metadata]
  (assoc (component/info-element metadata)
    :text (:koski-vocational-info texts)))

(defn- vocational-qualification-text-field [metadata]
  (assoc (component/text-field metadata)
    :label (:vocational-qualification texts)
    :validators ["required"]))

(defn- scope-of-education-unit-dropdown [metadata]
  (assoc (component/dropdown metadata)
    :label (:scope-unit texts)
    :options [{:label {:courses texts},
               :value "0"}
              {:label (:ects-credits texts),
               :value "1"}
              {:label (:study-weeks texts),
               :value "2"}
              {:label (:competence-points texts),
               :value "3"}
              {:label (:hours texts),
               :value "4"}
              {:label (:weekly-lessons texts),
               :value "5"}
              {:label (:years texts),
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

(defn- year-of-completion [metadata max-value min-value]
  (assoc (component/text-field metadata)
    :label (:year-of-completion texts)
    :params {:size "S"
             :numeric true
             :decimals nil
             :max-value max-value
             :min-value min-value}
    :validators ["numeric" "required"]))

(defn- name-of-degree [metadata]
  (assoc (component/dropdown metadata)
    :koodisto-source {:uri "tutkinto",
                      :title "Tutkinto",
                      :version 2,
                      :allow-invalid? true}
    :validators ["required"]
    :options []
    :label (:name-of-degree texts)))

(defn- name-of-degree-text-field [metadata]
  (assoc (component/text-field metadata)
    :label (:name-of-degree texts)
    :validators ["required"]))

(defn- seven-day-attachment-followup
  [metadata label]
  (assoc (component/attachment metadata)
          :params {:hidden false
                   :deadline nil
                   :info-text {:value (:submit-attachment-7-days texts)
                               :enabled? true}}
          :label label))

(defn- deadline-next-to-request-attachment-followup
  [metadata label]
  (assoc (component/attachment metadata)
    :params {:hidden false
            :deadline nil
            :info-text {:value (:deadline-next-to-attachment texts)
                        :enabled? true}}
   :label label))

(defn- are-your-attachments-in-fi-se-en-followup
  [metadata followups]
  (assoc (component/single-choice-button metadata)
    :label (:are-attachments-in-fi-en-sv texts)
   :params {:hidden false}
   :options [{:label (:yes general-texts)
              :value "0"}
             {:label (:no general-texts)
              :value "1"
              :followups followups}]
   :validators ["required"]))

(defn- country-of-completion
  [metadata params]
  (assoc (component/dropdown metadata)
    :params params
    :koodisto-source {:uri "maatjavaltiot2"
                     :title "Maat ja valtiot"
                     :version 2
                     :allow-invalid? true}
    :validators ["required"]
    :label (:country-of-completion texts)))

(defn- share-link-followup [metadata]
  (assoc (component/text-field metadata)
    :label (:share-link-to-studyinfo texts)
    :params {:size "L",
            :hidden false,
            :info-text {:label (:how-to-share-link-to-my-studyinfo texts)}}))

(defn- finnish-higher-education-option-followups [metadata]
  [(assoc (component/question-group metadata)
    :children [(assoc (year-of-completion metadata "2022" "1900")
                :options [(assoc (component/text-field-conditional-option "0")
                           :condition {:answer-compared-to 2022,
                                       :comparison-operator "="},
                           :followups [(assoc (component/single-choice-button metadata)
                                         :label (:have-you-graduated texts)
                                        :options [{:label (:yes general-texts)
                                                   :value "0",
                                                   :followups [(seven-day-attachment-followup metadata (:transcript-of-records-higher-finland texts))
                                                               (seven-day-attachment-followup metadata (:higher-education-degree-certificate texts))
                                                               (share-link-followup metadata)]}
                                                  {:label (:have-not general-texts)
                                                   :value "1",
                                                   :followups [(estimated-graduation-date-text-field metadata)
                                                               (seven-day-attachment-followup metadata (:transcript-of-records-higher-finland-in-progress texts))
                                                               (deadline-next-to-request-attachment-followup metadata (:higher-education-degree-certificate-in-progress texts))
                                                               (share-link-followup metadata)]}]
                                        :validators ["required"])])
                          (assoc (component/text-field-conditional-option "1")
                           :condition {:answer-compared-to 2022,
                                       :comparison-operator "<"},
                           :followups [(seven-day-attachment-followup metadata (:transcript-of-records-higher-finland texts))
                                       (seven-day-attachment-followup metadata (:higher-education-degree-certificate texts))
                                       (share-link-followup metadata)])])
               (assoc (component/dropdown metadata)
                 :koodisto-source {:uri "kktutkinnot",
                                  :title "Kk-tutkinnot",
                                  :version 1,
                                  :allow-invalid? false},
                :koodisto-ordered-by-user true,; TODO: Check order
                :validators ["required"],
                :label (:finnish-higher-education-degree-level texts))
               (name-of-degree metadata)
               (higher-education-text-field metadata)
               (assoc (component/info-element metadata)
                :label (:add-more-qualifications texts))])])

(defn- finnish-vocational-or-special-option-followups [metadata]
  [(assoc (component/question-group metadata)
    :children [(assoc (year-of-completion metadata "2022" "1900")
                :options [(assoc (component/text-field-conditional-option "0")
                           :condition {:answer-compared-to 2022,
                                       :comparison-operator "="}
                           :followups [(assoc (component/single-choice-button metadata)
                                         :label (:have-you-graduated texts)
                                        :options [{:label (:yes general-texts)
                                                   :value "0",
                                                   :followups []}
                                                  {:label (:have-not general-texts)
                                                   :value "1",
                                                   :followups [(estimated-graduation-date-text-field metadata)
                                                               (seven-day-attachment-followup metadata (:preliminary-certificate-vocational texts))]}]
                                        :validators ["required"])])
                          (assoc (component/text-field-conditional-option "1")
                           :condition {:answer-compared-to 2018,
                                       :comparison-operator "<"},
                           :followups [(seven-day-attachment-followup metadata (:vocational-or-special-diploma texts))])
                          (assoc (component/text-field-conditional-option "2")
                           :condition {:answer-compared-to 2017,
                                       :comparison-operator ">"},
                           :followups [(assoc (component/info-element metadata)
                                         :text (:koski-vocational-special-info texts))])])
               (assoc (component/text-field metadata)
                      :label (:qualification texts)
                      :validators ["required"])
               (assoc (component/text-field metadata)
                      :label (:scope-of-qualification texts)
                      :params {:size "S", :numeric true, :decimals 1}
                      :validators ["numeric"])
               (scope-of-education-unit-dropdown metadata)
               (assoc (component/dropdown metadata)
                 :params {:info-text {:label (:select-unknown texts)}}
                :koodisto-source {:uri "oppilaitostyyppi"
                                  :title "Ammatilliset oppilaitokset"
                                  :version 1
                                  :allow-invalid? true}
                :validators ["required"],
                :label (:educational-institution texts))
               (assoc (component/info-element metadata)
                :label (:add-more-qualifications texts))])])

(defn- vocational-upper-secondary-qualification-option-followups [metadata]
  [(assoc (component/info-element metadata)
          :text (:check-if-really-vocational texts))
   (assoc (component/question-group metadata)
          :children [(assoc (year-of-completion metadata "2022" "1994")
                      :options [(assoc (component/text-field-conditional-option "1")
                           :condition {:answer-compared-to 2022,
                                       :comparison-operator "="},
                           :followups [(assoc (component/single-choice-button metadata)
                                         :label (:have-you-graduated texts)
                                        :options [{:label (:yes general-texts),
                                                   :value "0",
                                                   :followups []}
                                                  {:label (:have-not general-texts),
                                                   :value "1",
                                                   :followups [(estimated-graduation-date-text-field metadata)
                                                               (seven-day-attachment-followup metadata (:preliminary-certificate-vocational-basic texts))]}],
                                        :validators ["required"])])
                          (assoc (component/text-field-conditional-option "2")
                           :condition {:answer-compared-to 2017,
                                       :comparison-operator ">"},
                           :followups [(koski-info-notification metadata)])
                          (assoc (component/text-field-conditional-option "3")
                           :condition {:answer-compared-to 2017,
                                       :comparison-operator "="},
                           :followups [(assoc (component/single-choice-button metadata)
                                         :label (:have-competence-based-qualification texts)
                                        :options [{:label (:yes general-texts)
                                                   :value "0"
                                                   :followups [(assoc (component/info-element metadata)
                                                                 :text (:notification-competence-based-qualification texts))
                                                               (seven-day-attachment-followup metadata (:vocational-diploma texts))]}
                                                  {:label (:have-not general-texts),
                                                   :value "1",
                                                   :followups [(koski-info-notification metadata)]}]
                                        :validators ["required"])])])
               (vocational-qualification-text-field metadata)
               (assoc (component/text-field metadata)
                       :label (:scope-of-vocational-qualification texts)
                :params {:size "S", :numeric true, :decimals 1}
                :validators ["numeric" "required"])
               (scope-of-education-unit-dropdown metadata)
               (assoc (component/dropdown metadata)
                       :params {:info-text {:label (:select-unknown texts)}}
                        :koodisto-source {:uri "oppilaitostyyppi",
                                  :title "Ammatilliset oppilaitokset",
                                  :version 1,
                                  :allow-invalid? true},
                        :validators ["required"],
                        :label (:educational-institution texts))
               (assoc (component/info-element metadata)
                :label (:add-more-qualifications texts))])])

(defn- finnish-matriculation-examination-option-followups [metadata]
  [(assoc (year-of-completion metadata "2022" "1900")
     :options [(assoc (component/text-field-conditional-option "0")
                :condition {:answer-compared-to 1990, :comparison-operator "<"},
                :followups [(seven-day-attachment-followup metadata (:matriculation-exam-certificate texts))])
               (assoc (component/text-field-conditional-option "1")
                :condition {:answer-compared-to 1989, :comparison-operator ">"},
                :followups [(assoc (component/info-element metadata)
                              :text (:automatic-matriculation-info texts))])])])

(defn- upper-secodary-double-degree-option-followups [metadata]
  [(assoc (component/question-group metadata)
    :children [(assoc (component/info-element metadata)
                 :text (:automatic-marticulation-info texts))
               (assoc (year-of-completion metadata "2022" "1900")
                :options [(assoc (component/text-field-conditional-option "0")
                           :condition {:answer-compared-to 2022,
                                       :comparison-operator "="},
                           :followups [(assoc (component/single-choice-button metadata)
                                         :label (:have-you-graduated texts)
                                        :options [{:label (:yes general-texts)
                                                   :value "0",
                                                   :followups []}
                                                  {:label (:have-not general-texts)
                                                   :value "1",
                                                   :followups [(estimated-graduation-date-text-field metadata)
                                                               (seven-day-attachment-followup metadata (:preliminary-certificate-vocational-basic texts))]}]
                                        :validators ["required"])])
                          (assoc (component/text-field-conditional-option "2")
                           :condition {:answer-compared-to 2017,
                                       :comparison-operator "="},
                           :followups [(assoc (component/single-choice-button metadata)
                                         :label (:have-competence-based-qualification texts)
                                        :params {:info-text {:label nil}},
                                        :options [{:label (:yes general-texts)
                                                   :value "0",
                                                   :followups [(seven-day-attachment-followup metadata (:vocational-diploma texts))]}
                                                  {:label (:have-not general-texts)
                                                   :value "1",
                                                   :followups [(koski-info-notification metadata)]}])])
                          (assoc (component/text-field-conditional-option "3")
                           :condition {:answer-compared-to 2017,
                                       :comparison-operator ">"},
                           :followups [(koski-info-notification metadata)])])
               (vocational-qualification-text-field metadata)
               (assoc (component/text-field metadata)
                :label (:scope-of-basic-vocational-qualification texts)
                :params {:size "S", :numeric true, :decimals 1},
                :validators ["numeric" "required"])
               (scope-of-education-unit-dropdown metadata)
               (assoc (component/dropdown metadata)
                  :params {:info-text {:label (:select-unknown texts)}},
                  :koodisto-source {:uri "oppilaitostyyppi",
                                    :title "Ammatilliset oppilaitokset",
                                    :version 1,
                                    :allow-invalid? true},
                  :validators ["required"]
                  :label (:vocational-institution texts))
               (assoc (component/info-element metadata)
                :label (:add-more-qualifications texts))])])

(defn- gymnasium-without-yo-certificate-option-followups [metadata]
  [(assoc (year-of-completion metadata "2022" "1900")
    :options [(assoc (component/text-field-conditional-option "0")
               :condition {:answer-compared-to 2022, :comparison-operator "="},
               :followups [(assoc (component/single-choice-button metadata)
                             :label (:have-you-graduated texts)
                            :options [{:label (:yes general-texts)
                                       :value "0",
                                       :followups [(seven-day-attachment-followup metadata (:upper-secondary-school-attachment texts))]}
                                      {:label (:have-not general-texts)
                                       :value "1",
                                       :followups [(estimated-graduation-date-text-field metadata)
                                                   (seven-day-attachment-followup metadata (:transcript-of-records-secondary-finland texts))]}]
                            :validators ["required"])])
              {:label {:fi "", :sv ""},
               :value "1",
               :condition {:answer-compared-to 2022, :comparison-operator "<"},
               :followups [(seven-day-attachment-followup metadata (:upper-secondary-school-attachment texts))]}])])

(defn- international-matriculation-exam-in-finland-option-followups [metadata]
  [(assoc (component/question-group metadata)
    :children [(assoc (year-of-completion metadata "2022" "1900")
                :options [(assoc (component/text-field-conditional-option "0")
                           :condition {:answer-compared-to 2022,
                                       :comparison-operator "<"},
                           :followups [(assoc (component/single-choice-button metadata)
                                         :label (:international-matriculation-outside-finland-name texts)
                                        :options [{:label (:international-baccalaureate texts)
                                                   :value "0",
                                                   :followups [(seven-day-attachment-followup metadata (:ib-diploma-finland texts))]}
                                                  {:label (:european-baccalaureate texts)
                                                   :value "1",
                                                   :followups [(seven-day-attachment-followup metadata (:ib-diploma-finland texts))]}
                                                  {:label (:reifeprufung texts)
                                                   :value "2",
                                                   :followups [(seven-day-attachment-followup metadata (:reifeprufung-diploma-finland texts))
                                                               (seven-day-attachment-followup metadata (:equivalency-certificate-second texts))]}]
                                        :validators ["required"])])
                          (assoc (component/text-field-conditional-option "1")
                           :condition {:answer-compared-to 2022,
                                       :comparison-operator "="},
                           :followups [(assoc (component/single-choice-button metadata)
                                         :label (:matriculation-exam texts)
                                        :options [{:label (:international-baccalaureate texts)
                                                   :value "0",
                                                   :followups [(assoc (component/single-choice-button metadata)
                                                                 :label (:have-you-graduated texts)
                                                                :options [{:label (:yes general-texts)
                                                                           :value "0",
                                                                           :followups [(seven-day-attachment-followup metadata (:ib-diploma-finland texts))]}
                                                                          {:label (:have-not general-texts)
                                                                           :value "1",
                                                                           :followups [(estimated-graduation-date-text-field metadata)
                                                                                       (seven-day-attachment-followup metadata (:predicted-grades-ib-finland texts))
                                                                                       (deadline-next-to-request-attachment-followup metadata (:diploma-programme-ib-finland texts))]}]
                                                                :validators ["required"])]}
                                                  {:label (:european-baccalaureate texts)
                                                   :value "1",
                                                   :followups [(assoc (component/single-choice-button metadata)
                                                                 :label (:have-you-graduated texts)
                                                                :options [{:label (:yes general-texts)
                                                                           :value "0",
                                                                           :followups [(seven-day-attachment-followup metadata (:european-baccalaureate-diploma-finland texts))]}
                                                                          {:label (:have-not general-texts)
                                                                           :value "1",
                                                                           :followups [(estimated-graduation-date-text-field metadata)
                                                                                       (seven-day-attachment-followup metadata (:predicted-grades-eb-finland texts))
                                                                                       (deadline-next-to-request-attachment-followup metadata (:diploma-programme-eb-finland texts))]}]
                                                                :validators ["required"])]}
                                                  {:label (:reifeprufung texts)
                                                   :value "2",
                                                   :followups [(assoc (component/single-choice-button metadata)
                                                                 :label (:have-you-graduated texts)
                                                                :options [{:label (:yes general-texts)
                                                                           :value "0",
                                                                           :followups [(seven-day-attachment-followup metadata (:dia-diploma-finland texts))
                                                                                       (seven-day-attachment-followup metadata (:equivalency-certificate-second-dia texts))]}
                                                                          {:label (:have-not general-texts)
                                                                           :value "1",
                                                                           :followups [(estimated-graduation-date-text-field metadata)
                                                                                       (seven-day-attachment-followup metadata (:grade-page-dia-finland texts))
                                                                                       (deadline-next-to-request-attachment-followup metadata (:dia-diploma-finland texts))
                                                                                       (seven-day-attachment-followup metadata (:equivalency-certificate-second-dia texts))]}]
                                                                :validators ["required"])]}]
                                        :validators ["required"])])])
               (education-institution-text-field metadata)
               (assoc (component/info-element metadata)
                :label (:add-more-qualifications texts))])])

(defn- vocational-upper-secondary-qualification-finland-option-followups [metadata]
  [(assoc (component/info-element metadata)
     :text (:check-if-really-opistoaste texts))
   (assoc (component/question-group metadata)
    :children [(year-of-completion metadata "2005" "1900")
               (assoc (component/dropdown metadata)
                 :label (:type-of-vocational texts)
                :params {:hidden false},
                :options [{:label (:kouluaste-of-vocational-qualification texts)
                           :value "0"
                           :followups [(assoc (component/info-element metadata)
                                         :text (:check-if-really-kouluaste texts))]}
                          {:label (:vocational-opistoaste-qualification texts)
                           :value "1"
                           :followups [(assoc (component/info-element metadata)
                                         :text (:check-if-really-post-secondary texts))]}
                          {:label (:vocational-korkea-aste-qualification texts)
                           :value "2"
                           :followups [(assoc (component/info-element metadata)
                                         :text (:check-if-really-higher-vocational texts))]}]
                :validators ["required"])
               (vocational-qualification-text-field metadata)
               (assoc (component/text-field metadata)
                 :label (:scope-of-vocational-qualification texts)
                :params {:size "S", :numeric true, :decimals 1}
                :validators ["numeric" "required"])
               (scope-of-education-unit-dropdown metadata)
               (assoc (component/dropdown metadata)
                :params {:info-text {:label (:select-unknown texts)}}
                :koodisto-source {:uri "oppilaitostyyppi"
                                  :title "Ammatilliset oppilaitokset"
                                  :version 1
                                  :allow-invalid? true}
                :validators ["required"]
                :label (:educational-institution texts))
               (seven-day-attachment-followup metadata (:vocational-qualification-diploma texts))
               (assoc (component/info-element metadata)
                :label (:add-more-qualifications texts))])])

(defn- upper-secondary-qualification-not-finland-option-followups [metadata]
  [(assoc (component/question-group metadata)
    :children [(assoc (year-of-completion metadata "2022" "1900")
                :options [(assoc (component/text-field-conditional-option "0")
                           :condition {:answer-compared-to 2022,
                                       :comparison-operator "="},
                           :followups [(assoc (component/single-choice-button metadata)
                                         :label (:have-you-graduated texts)
                                        :options [{:label (:yes general-texts)
                                                   :value "0",
                                                   :followups [(seven-day-attachment-followup metadata (:upper-secondary-education-diploma texts))
                                                               (assoc (component/single-choice-button metadata)
                                                                 :label (:diploma-in-fi-sv-en texts)
                                                                 :params {:hidden false},
                                                                 :options [{:label (:yes general-texts)
                                                                           :value "0"}
                                                                          {:label (:no general-texts)
                                                                           :value "1"
                                                                           :followups [(seven-day-attachment-followup metadata (:translation-of-diploma texts))]}],
                                                                :validators ["required"])]}
                                                  {:label (:have-not general-texts)
                                                   :value "1"
                                                   :followups [(estimated-graduation-date-text-field metadata)
                                                               (seven-day-attachment-followup metadata (:transcript-of-records-upper-secondary texts))
                                                               (deadline-next-to-request-attachment-followup metadata (:original-upper-secondary-diploma texts))
                                                               (are-your-attachments-in-fi-se-en-followup metadata [(seven-day-attachment-followup metadata (:translation-of-study-records texts))
                                                                                                           (deadline-next-to-request-attachment-followup metadata (:translation-of-diploma texts))])]}]
                                        :validators ["required"])])
                          (assoc (component/text-field-conditional-option "1")
                           :condition {:answer-compared-to 2022,
                                       :comparison-operator "<"}
                           :followups [(seven-day-attachment-followup metadata (:upper-secondary-education-diploma texts))
                                       (are-your-attachments-in-fi-se-en-followup metadata [(seven-day-attachment-followup metadata (:translation-of-diploma texts))])])])
               (name-of-degree-text-field metadata)
               (education-institution-text-field metadata)
               (country-of-completion metadata {})
               (assoc (component/info-element metadata)
                :label (:add-more-qualifications texts))])])

(defn- internation-matriculation-examination-option-followups [metadata]
  [(assoc (component/question-group metadata)
    :children [(assoc (year-of-completion metadata "2022" "1900")
                :options [(assoc (component/text-field-conditional-option "0")
                           :condition {:answer-compared-to 2022,
                                       :comparison-operator "<"},
                           :followups [(assoc (component/single-choice-button metadata)
                                         :label (:matriculation-exam texts)
                                         :options [{:label (:international-baccalaureate texts)
                                                   :value "0"
                                                   :followups [(seven-day-attachment-followup metadata (:ib-diploma texts))]}
                                                  {:label (:european-baccalaureate texts)
                                                   :value "1",
                                                   :followups [(seven-day-attachment-followup metadata (:european-baccalaureate-diploma texts))]}
                                                  {:label (:reifeprufung texts),
                                                   :value "2",
                                                   :followups [(seven-day-attachment-followup metadata (:reifeprufung-diploma texts))]}],
                                        :validators ["required"])])
                          (assoc (component/text-field-conditional-option "1")
                           :condition {:answer-compared-to 2022,
                                       :comparison-operator "="},
                           :followups [(assoc (component/single-choice-button metadata)
                                        :label (:matriculation-exam texts)
                                        :options [{:label (:international-baccalaureate texts)
                                                   :value "0",
                                                   :followups [(assoc (component/single-choice-button metadata)
                                                                 :label (:have-you-graduated texts)
                                                                :options [{:label (:yes general-texts)
                                                                           :value "0",
                                                                           :followups [(seven-day-attachment-followup metadata (:ib-diploma texts))]}
                                                                          {:label (:have-not general-texts)
                                                                           :value "1",
                                                                           :followups [(estimated-graduation-date-text-field metadata)
                                                                                       (deadline-next-to-request-attachment-followup metadata (:predicted-grades-ib texts))
                                                                                       (deadline-next-to-request-attachment-followup metadata (:diploma-programme-ib texts))]}]
                                                                :validators ["required"])]}
                                                  {:label (:european-baccalaureate texts)
                                                   :value "1",
                                                   :followups [(assoc (component/single-choice-button metadata)
                                                                 :label (:have-you-graduated texts)
                                                                :options [{:label (:yes general-texts)
                                                                           :value "0",
                                                                           :followups [(seven-day-attachment-followup metadata (:european-baccalaureate-diploma texts))]}
                                                                          {:label (:have-not general-texts)
                                                                           :value "1",
                                                                           :followups [(estimated-graduation-date-text-field metadata)
                                                                                       (deadline-next-to-request-attachment-followup metadata (:predicted-grades-eb texts))
                                                                                       (deadline-next-to-request-attachment-followup metadata (:european-baccalaureate-diploma texts))]}],
                                                                 :validators ["required"])]}
                                                  {:label (:reifeprufung texts)
                                                   :value "2",
                                                   :followups [(assoc (component/single-choice-button metadata)
                                                                 :label (:have-you-graduated texts)
                                                                :options [{:label (:yes general-texts)
                                                                           :value "0",
                                                                           :followups [(seven-day-attachment-followup metadata (:reifeprufung-diploma texts))]}
                                                                          {:label (:have-not general-texts)
                                                                           :value "1",
                                                                           :followups [(estimated-graduation-date-text-field metadata)
                                                                                       (deadline-next-to-request-attachment-followup metadata (:grade-page-dia texts))
                                                                                       (deadline-next-to-request-attachment-followup metadata (:dia-diploma texts))]}]
                                                                 :validators ["required"])]}]
                                        :validators ["required"])])])
               (education-institution-text-field metadata)
               (country-of-completion metadata {})
               (assoc (component/info-element metadata)
                :label (:add-more-qualifications texts))])])

(defn- non-finnish-higher-education-option-followups [metadata]
  [(assoc (component/question-group metadata)
    :children [(assoc (year-of-completion metadata "2022" "1900")
                :options [(assoc (component/text-field-conditional-option "0")
                           :condition {:answer-compared-to 2022
                                       :comparison-operator "="}
                           :followups [(assoc (component/single-choice-button metadata)
                                         :label (:have-you-graduated texts)
                                        :options [{:label (:yes general-texts)
                                                   :value "0",
                                                   :followups [(seven-day-attachment-followup metadata (:transcript-of-records-higher texts))
                                                               (seven-day-attachment-followup metadata (:higher-education-degree-certificate texts))
                                                               (are-your-attachments-in-fi-se-en-followup metadata[(seven-day-attachment-followup metadata (:translation-of-certificate texts))])]}
                                                  {:label (:have-not general-texts)
                                                   :value "1",
                                                   :followups [(estimated-graduation-date-text-field metadata)
                                                               (seven-day-attachment-followup metadata (:transcript-of-records-in-progress texts))
                                                               (deadline-next-to-request-attachment-followup metadata (:higher-education-degree-certificate-alien-in-progress texts))
                                                               (are-your-attachments-in-fi-se-en-followup metadata [(seven-day-attachment-followup metadata (:translation-of-transcript-of-records texts))
                                                                                                           (deadline-next-to-request-attachment-followup metadata (:translation-of-degree-higher texts))])]}],
                                        :validators ["required"])])
                          (assoc (component/text-field-conditional-option "1")
                           :condition {:answer-compared-to 2022,
                                       :comparison-operator "<"}
                           :followups [(seven-day-attachment-followup metadata (:transcript-of-records-higher texts))
                                       (seven-day-attachment-followup metadata (:higher-education-degree-certificate-alien texts))
                                       (are-your-attachments-in-fi-se-en-followup metadata [(seven-day-attachment-followup metadata (:translation-of-certificate texts))])])])
               (name-of-degree-text-field metadata)
               (assoc (component/dropdown metadata)
                :koodisto-source {:uri "kktutkinnot"
                                  :title "Kk-tutkinnot"
                                  :version 1
                                  :allow-invalid? false}
                :koodisto-ordered-by-user true ; TODO: check if this is required, then order options
                :validators ["required"],
                :label (:finnish-higher-education-degree-level texts))
               (higher-education-text-field metadata)
               (country-of-completion metadata {})
               (assoc (component/info-element metadata)
                 :label (:add-more-qualifications texts))])])

(defn- open-university-option-followups [metadata]
  [(assoc (component/question-group metadata)
      :children [(year-of-completion metadata "2022" "1900")
                 (assoc (component/text-field metadata)
                    :label {:en "Study field", :fi "Ala", :sv "Bransch"}
                    :validators ["required"])
                 (higher-education-text-field metadata)
                 (assoc (component/text-field metadata)
                    :label (:module texts)
                    :validators ["required"])
                 (assoc (component/text-field metadata)
                    :label (:scope-of-studies texts)
                    :validators ["required"])
                 (seven-day-attachment-followup metadata (:certificate-open-studies texts))
                 (assoc (component/info-element metadata)
                        :label (:add-more-studies texts))])])

(defn- other-eligibility-option-followups [metadata]
  [(assoc (component/question-group metadata)
    :children [(year-of-completion metadata "2022" "1900")
               (assoc (component/text-area metadata)
                      :label (:base-education-other-description texts)
                      :params {:max-length "500"}
                      :validators ["required"])
               (seven-day-attachment-followup metadata (:other-eligibility-attachment texts))
               (assoc (component/info-element metadata)
                :label (:add-more-wholes texts))])])

(defn- education-question [metadata]
  (assoc (component/multiple-choice metadata)
  :params {:hidden false,
                        :info-text {:label (:read-who-can-apply texts)}},
               :koodisto-source {:uri "pohjakoulutuskklomake",
                                 :title "Kk-pohjakoulutusvaihtoehdot",
                                 :version 2,
                                 :allow-invalid? false},
               :koodisto-ordered-by-user true,
               :validators ["required"],
               :label (:completed-education texts),
               :options [{:label (:matriculation-exam-in-finland texts),
                          :value "pohjakoulutus_yo",
                          :followups (finnish-matriculation-examination-option-followups metadata)}
                         {:label (:pohjakoulutus_amp virkailija-texts),
                          :value "pohjakoulutus_amp",
                          :followups (vocational-upper-secondary-qualification-option-followups metadata)}
                         {:label (:finnish-vocational-or-special texts),
                          :value "pohjakoulutus_amt",
                          :followups (finnish-vocational-or-special-option-followups metadata)}
                         {:label {:en "Bachelor’s/Master’s/Doctoral degree completed in Finland",
                                  :fi "Suomessa suoritettu korkeakoulututkinto",
                                  :sv "Högskoleexamen som avlagts i Finland"},
                          :value "pohjakoulutus_kk",
                          :followups (finnish-higher-education-option-followups metadata)}
                         {:label {:en "Upper secondary double degree completed in Finland (kaksoistutkinto)",
                                  :fi "Suomessa suoritettu kaksoistutkinto (ammatillinen perustutkinto ja ylioppilastutkinto)",
                                  :sv "Dubbelexamen som avlagts i Finland"},
                          :value "pohjakoulutus_yo_ammatillinen",
                          :followups (upper-secodary-double-degree-option-followups metadata)}
                         {:label {:en "General upper secondary school syllabus completed in Finland (lukion oppimäärä ilman ylioppilastutkintoa)",
                                  :fi "Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa",
                                  :sv "Gymnasiets lärokurs som avlagts i Finland utan studentexamen"},
                          :value "pohjakoulutus_lk",
                          :followups (gymnasium-without-yo-certificate-option-followups metadata)}
                         {:label {:en "International matriculation examination completed in Finland (IB, EB and RP/DIA)",
                                  :fi "Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)",
                                  :sv "Internationell studentexamen som avlagts i Finland (IB, EB och RP/DIA)"},
                          :value "pohjakoulutus_yo_kansainvalinen_suomessa",
                          :followups (international-matriculation-exam-in-finland-option-followups metadata)}
                         {:label {:en "Vocational upper secondary qualification completed in Finland (kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto)",
                                  :fi "Suomessa suoritettu kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto",
                                  :sv "Yrkesinriktad examen på skolnivå, examen på institutsnivå eller yrkesinriktad examen på högre nivå som avlagts i Finland"},
                          :value "pohjakoulutus_amv",
                          :followups (vocational-upper-secondary-qualification-finland-option-followups metadata)}
                         {:label {:en "Upper secondary education completed outside Finland (general or vocational)",
                                  :fi "Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun",
                                  :sv "Övrig examen som avlagts annanstans än i Finland, och ger behörighet för högskolestudier i ifrågavarande land"},
                          :value "pohjakoulutus_ulk",
                          :followups (upper-secondary-qualification-not-finland-option-followups metadata)}
                         {:label {:en "International matriculation examination completed outside Finland (IB, EB and RP/DIA)",
                                  :fi "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto (IB, EB ja RP/DIA)",
                                  :sv "Internationell studentexamen som avlagts annanstans än i Finland (IB, EB och RP/DIA)"},
                          :value "pohjakoulutus_yo_ulkomainen",
                          :followups (internation-matriculation-examination-option-followups metadata)}
                         {:label {:en "Bachelor’s/Master’s/Doctoral degree completed outside Finland",
                                  :fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto",
                                  :sv "Högskoleexamen som avlagts annanstans än i Finland"},
                          :value "pohjakoulutus_kk_ulk",
                          :followups (non-finnish-higher-education-option-followups metadata)}
                         {:label {:en "Open university/UAS studies required by the higher education institution",
                                  :fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot",
                                  :sv "Studier som högskolan kräver vid en öppen högskola"},
                          :value "pohjakoulutus_avoin",
                          :followups (open-university-option-followups metadata)}
                         {:label {:en "Other eligibility for higher education",
                                  :fi "Muu korkeakoulukelpoisuus",
                                  :sv "Övrig högskolebehörighet"},
                          :value "pohjakoulutus_muu",
                          :followups (other-eligibility-option-followups metadata)}]))

(defn- education-statistics-question [metadata]
  (assoc (component/single-choice-button metadata)
         :label (:have-you-completed texts)
         :params {:info-text {:label (:required-for-statistics texts)}}
         :options [{:label (:yes general-texts)
                    :value "0",
                    :followups [(country-of-completion metadata {:info-text {:label (:choose-country-of-latest-qualification texts)}})]}
                   {:label (:have-not general-texts)
                    :value "1"}]
         :validators ["required"]
    ))

(defn- name-of-higher-education-institution [metadata]
  (assoc  (component/text-field metadata)
          :label (:higher-education-institution texts)
          :validators ["required"]))

(defn- education-question-before-2003 [metadata]
  (assoc (component/single-choice-button metadata)
          :label (:have-you-completed-before-2003 texts)
          :params {:info-text
                    {:label (:write-completed-before-2003 texts)}},
          :options [{:label (:yes general-texts),
                     :value "0",
                     :followups [(year-of-completion metadata "2002" "1900")
                                 (name-of-degree metadata)
                                 (name-of-higher-education-institution metadata)]}
                   {:label (:have-not general-texts)
                    :value "1"}]
          :validators ["required"]))

(defn base-education-module-higher [metadata]
  (assoc (component/form-section metadata)
          :label (:educational-background texts)
          :children [(education-question metadata)
                     (education-statistics-question metadata)
                     (education-question-before-2003 metadata)]))
