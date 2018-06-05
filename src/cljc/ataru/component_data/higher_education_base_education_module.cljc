(ns ataru.component-data.higher-education-base-education-module
  (:require [ataru.component-data.component :refer [form-section
                                                    multiple-choice
                                                    single-choice-button
                                                    text-field
                                                    text-area
                                                    info-element
                                                    attachment
                                                    dropdown
                                                    question-group]]
            [ataru.translations.texts :refer [higher-base-education-module-texts general-texts]]))

(defn module [metadata]
  (merge (form-section metadata)
         {:id       "higher-base-education-module"
          :label    (:educational-background higher-base-education-module-texts)
          :children [(merge (multiple-choice metadata)
                            {:id                       "higher-completed-base-education"
                             :koodisto-source          {:uri "pohjakoulutuskklomake" :title "Kk-pohjakoulutusvaihtoehdot" :version 1}
                             :koodisto-ordered-by-user true
                             :validators               ["required"]
                             :label                    (:completed-education higher-base-education-module-texts)
                             :options                  [{:label     (:marticulation-exam-in-finland higher-base-education-module-texts)
                                                         :value     "pohjakoulutus_yo"
                                                         :followups [(merge (single-choice-button metadata)
                                                                            {:id         "pohjakoulutus_yo"
                                                                             :label      (:completed-marticaulation-before-1990? higher-base-education-module-texts)
                                                                             :options    [{:label     (:yes general-texts)
                                                                                           :value     "Yes"
                                                                                           :followups [(merge (text-field metadata)
                                                                                                              {:id         "pohjakoulutus_yo--yes-year-of-completion"
                                                                                                               :label      (:year-of-completion higher-base-education-module-texts)
                                                                                                               :params     {:size "S" :numeric true :decimals nil}
                                                                                                               :validators ["required" "numeric"]})
                                                                                                       (merge (info-element metadata)
                                                                                                              {:text (:automatic-marticulation-info higher-base-education-module-texts)})]}
                                                                                          {:label     (:no general-texts)
                                                                                           :value     "No"
                                                                                           :followups [(merge (text-field metadata)
                                                                                                              {:id         "pohjakoulutus_yo--no-year-of-completion"
                                                                                                               :label      (:year-of-completion higher-base-education-module-texts)
                                                                                                               :params     {:size "S" :numeric true}
                                                                                                               :validators ["numeric" "required"]})
                                                                                                       (merge (attachment metadata)
                                                                                                              {:params {:info-text {:label    (:submit-your-attachments higher-base-education-module-texts)
                                                                                                                                    :enabled? true}}
                                                                                                               :label  (:marticaulation-before-1990 higher-base-education-module-texts)
                                                                                                               :id     "pohjakoulutus-yo--attachment"})]}]
                                                                             :validators ["required"]})]}
                                                        {:label     (:general-upper-secondary-school higher-base-education-module-texts)
                                                         :value     "pohjakoulutus_lk"
                                                         :followups [(merge (text-field metadata)
                                                                            {:id         "pohjakoulutus_lk--year-of-completion"
                                                                             :label      (:secondary-school-year-of-completion higher-base-education-module-texts)
                                                                             :params     {:size      "S"
                                                                                          :numeric   true
                                                                                          :info-text {:label (:fill-year-of-completion higher-base-education-module-texts)}}
                                                                             :validators ["required" "numeric"]})
                                                                     (merge (text-field metadata)
                                                                            {:id         "pohjakoulutus_lk--educational-institution"
                                                                             :label      (:educational-institution higher-base-education-module-texts)
                                                                             :validators ["required"]})
                                                                     (merge (attachment metadata)
                                                                            {:id     "pohjakoulutus_lk--attachment"
                                                                             :label  (:upper-secondary-school-attachment higher-base-education-module-texts)
                                                                             :params {:info-text {:label    (:submit-your-attachments higher-base-education-module-texts)
                                                                                                  :enabled? true}}})]}
                                                        {:label     (:international-marticulation-exam higher-base-education-module-texts)
                                                         :value     "pohjakoulutus_yo_kansainvalinen_suomessa"
                                                         :followups [(merge (text-field metadata)
                                                                            {:id         "pohjakoulutus_yo_kansainvalinen_suomessa--year-of-completion"
                                                                             :label      (:year-of-completion higher-base-education-module-texts)
                                                                             :params     {:size "S" :numeric true}
                                                                             :validators ["numeric" "required"]})
                                                                     (merge (dropdown metadata)
                                                                            {:id         "pohjakoulutus_yo_kansainvalinen_suomessa--exam-type"
                                                                             :label      (:marticulation-exam higher-base-education-module-texts)
                                                                             :options    [{:label (:international-baccalaureate higher-base-education-module-texts)
                                                                                           :value "International Baccalaureate -diploma"}
                                                                                          {:label (:european-baccalaureate higher-base-education-module-texts)
                                                                                           :value "European Baccalaureate -diploma"}
                                                                                          {:label (:reifeprufung higher-base-education-module-texts)
                                                                                           :value "Reifeprüfung - diploma/ Deutsche Internationale Abiturprüfung"}]
                                                                             :validators ["required"]})
                                                                     (merge (text-field metadata)
                                                                            {:id         "pohjakoulutus_yo_kansainvalinen_suomessa--institution"
                                                                             :label      (:educational-institution higher-base-education-module-texts)
                                                                             :validators ["required"]})
                                                                     (merge (attachment metadata)
                                                                            {:id     "pohjakoulutus_yo_kansainvalinen_suomessa--attachment"
                                                                             :label  (:request-attachment-international-exam higher-base-education-module-texts)
                                                                             :params {:info-text {:label    (:submit-your-attachments higher-base-education-module-texts)
                                                                                                  :enabled? true}}})]}
                                                        {:label     (:double-degree higher-base-education-module-texts)
                                                         :value     "pohjakoulutus_yo_ammatillinen"
                                                         :followups [(merge (text-field metadata)
                                                                            {:id         "pohjakoulutus_yo_ammatillinen--marticulation-year-of-completion"
                                                                             :label      (:marticulation-completion-year higher-base-education-module-texts)
                                                                             :params     {:size "S" :numeric true}
                                                                             :validators ["numeric" "required"]})
                                                                     (merge (text-field metadata)
                                                                            {:id         "pohjakoulutus_yo_ammatillinen--vocational-completion-year"
                                                                             :label      (:vocational-completion-year higher-base-education-module-texts)
                                                                             :params     {:size "S" :numeric true}
                                                                             :validators ["numeric" "required"]})
                                                                     (merge (text-field metadata)
                                                                            {:id         "pohjakoulutus_yo_ammatillinen--vocational-qualification"
                                                                             :label      (:vocational-qualification higher-base-education-module-texts)
                                                                             :validators ["required"]})
                                                                     (merge (text-field metadata)
                                                                            {:id         "pohjakoulutus_yo_ammatillinen--scope-of-qualification"
                                                                             :label      (:scope-of-qualification higher-base-education-module-texts)
                                                                             :params     {:size "S" :numeric true}
                                                                             :validators ["numeric" "required"]})
                                                                     (merge (dropdown metadata)
                                                                            {:id         "pohjakoulutus_yo_ammatillinen--scope-of-qualification-units"
                                                                             :label      {:fi ""}
                                                                             :options    [{:label (:courses higher-base-education-module-texts)
                                                                                           :value "Courses"}
                                                                                          {:label (:ects-credits higher-base-education-module-texts)
                                                                                           :value "ECTS credits"}
                                                                                          {:label (:study-weeks higher-base-education-module-texts)
                                                                                           :value "Study weeks"}
                                                                                          {:label (:competence-points higher-base-education-module-texts)
                                                                                           :value "Competence points"}
                                                                                          {:label (:hours higher-base-education-module-texts)
                                                                                           :value "Hours"}
                                                                                          {:label (:weekly-lessons higher-base-education-module-texts)
                                                                                           :value "Weekly lessons per year"}
                                                                                          {:label (:years higher-base-education-module-texts)
                                                                                           :value "Years"}]
                                                                             :validators ["required"]})
                                                                     (merge (text-field metadata)
                                                                            {:id         "pohjakoulutus_yo_ammatillinen--educational-institution"
                                                                             :label      (:educational-institution higher-base-education-module-texts)
                                                                             :validators ["required"]})
                                                                     (merge (attachment metadata)
                                                                            {:id     "pohjakoulutus_yo_ammatillinen--vocational-attachment"
                                                                             :label  (:double-degree-vocational-attachment higher-base-education-module-texts)
                                                                             :params {:info-text {:label    (:submit-your-attachments higher-base-education-module-texts)
                                                                                                  :enabled? true}}})
                                                                     (merge (attachment metadata)
                                                                            {:id     "pohjakoulutus_yo_ammatillinen--marticulation-attachment"
                                                                             :label  (:double-degree-marticulation-attachment higher-base-education-module-texts)
                                                                             :params {:info-text {:label    (:submit-your-attachments higher-base-education-module-texts)
                                                                                                  :enabled? true}}})]}
                                                        {:label     (:finnish-vocational higher-base-education-module-texts)
                                                         :value     "pohjakoulutus_am"
                                                         :followups [(merge (single-choice-button metadata)
                                                                            {:id         "pohjakoulutus_am"
                                                                             :label      (:finnish-vocational-2017-or-after higher-base-education-module-texts)
                                                                             :options    [{:label     (:yes general-texts)
                                                                                           :value     "Yes"
                                                                                           :followups [(merge (info-element metadata)
                                                                                                              {:text (:automatic-qualification-info higher-base-education-module-texts)})]}
                                                                                          {:label     (:no general-texts)
                                                                                           :value     "No"
                                                                                           :followups [(merge (question-group metadata)
                                                                                                              {:id       "pohjakoulutus_am--followups"
                                                                                                               :children [(merge (text-field metadata)
                                                                                                                                 {:id         "pohjakoulutus_am--year-of-completion"
                                                                                                                                  :label      (:year-of-completion higher-base-education-module-texts)
                                                                                                                                  :params     {:size "S" :numeric true}
                                                                                                                                  :validators ["numeric" "required"]})
                                                                                                                          (merge (text-field metadata)
                                                                                                                                 {:id         "pohjakoulutus_am--qualification"
                                                                                                                                  :label      (:qualification higher-base-education-module-texts)
                                                                                                                                  :validators ["required"]})
                                                                                                                          (merge (text-field metadata)
                                                                                                                                 {:id         "pohjakoulutus_am--scope-of-qualification"
                                                                                                                                  :label      (:scope-of-qualification higher-base-education-module-texts)
                                                                                                                                  :params     {:size "S" :numeric true}
                                                                                                                                  :validators ["numeric" "required"]})
                                                                                                                          (merge (dropdown metadata)
                                                                                                                                 {:id         "pohjakoulutus_am--scope-of-qualification-units"
                                                                                                                                  :label      {:fi ""}
                                                                                                                                  :options    [{:label (:courses higher-base-education-module-texts)
                                                                                                                                                :value "Courses"}
                                                                                                                                               {:label (:ects-credits higher-base-education-module-texts)
                                                                                                                                                :value "ECTS credits"}
                                                                                                                                               {:label (:study-weeks higher-base-education-module-texts)
                                                                                                                                                :value "Study weeks"}
                                                                                                                                               {:label (:competence-points higher-base-education-module-texts)
                                                                                                                                                :value "Competence points"}
                                                                                                                                               {:label (:hours higher-base-education-module-texts)
                                                                                                                                                :value "Hours"}
                                                                                                                                               {:label (:weekly-lessons higher-base-education-module-texts)
                                                                                                                                                :value "Weekly lessons per year"}
                                                                                                                                               {:label (:years higher-base-education-module-texts)
                                                                                                                                                :value "Years"}]
                                                                                                                                  :validators ["required"]})
                                                                                                                          (merge (text-field metadata)
                                                                                                                                 {:id         "pohjakoulutus_am--institution"
                                                                                                                                  :label      (:educational-institution higher-base-education-module-texts)
                                                                                                                                  :validators ["required"]})
                                                                                                                          (merge (single-choice-button metadata)
                                                                                                                                 {:id         "pohjakoulutus_am--completed"
                                                                                                                                  :label      (:finnish-vocational-completed higher-base-education-module-texts)
                                                                                                                                  :options    [{:label (:yes general-texts)
                                                                                                                                                :value "Yes"}
                                                                                                                                               {:label (:no general-texts)
                                                                                                                                                :value "No"}]
                                                                                                                                  :validators ["required"]})
                                                                                                                          (merge (attachment metadata)
                                                                                                                                 {:id     "pohjakoulutus_am--attachment"
                                                                                                                                  :label  (:finnish-vocational-attachment higher-base-education-module-texts)
                                                                                                                                  :params {:info-text {:label    (:submit-your-attachments higher-base-education-module-texts)
                                                                                                                                                       :enabled? true}}})]})
                                                                                                       (merge (info-element metadata)
                                                                                                              {:text (:click-to-add-more higher-base-education-module-texts)})]}]
                                                                             :validators ["required"]})]}
                                                        {:label     (:finnish-vocational-or-special higher-base-education-module-texts)
                                                         :value     "pohjakoulutus_amt"
                                                         :followups [(merge (single-choice-button metadata)
                                                                            {:id      "pohjakoulutus_amt"
                                                                             :label   (:finnish-special-before-2018 higher-base-education-module-texts)
                                                                             :options [{:label     (:yes general-texts)
                                                                                        :value     "Yes"
                                                                                        :followups [(merge (info-element metadata)
                                                                                                           {:text (:automatic-qualification-info higher-base-education-module-texts)})]}
                                                                                       {:label     (:no general-texts)
                                                                                        :value     "No"
                                                                                        :followups [(merge (question-group metadata)
                                                                                                           {:id       "pohjakoulutus_amt--followups"
                                                                                                            :children [(merge (text-field metadata)
                                                                                                                              {:id         "pohjakoulutus_amt--year-of-completion"
                                                                                                                               :label      (:year-of-completion higher-base-education-module-texts)
                                                                                                                               :params     {:size "S" :numeric true}
                                                                                                                               :validators ["required" "numeric"]})
                                                                                                                       (merge (text-field metadata)
                                                                                                                              {:id         "pohjakoulutus_amt--scope-of-qualification"
                                                                                                                               :label      (:scope-of-qualification higher-base-education-module-texts)
                                                                                                                               :params     {:size "S" :numeric true}
                                                                                                                               :validators ["numeric" "required"]})
                                                                                                                       (merge (dropdown metadata)
                                                                                                                              {:id         "pohjakoulutus_amt--scope-of-qualification-units"
                                                                                                                               :label      {:fi ""}
                                                                                                                               :options    [{:label (:courses higher-base-education-module-texts)
                                                                                                                                             :value "Courses"}
                                                                                                                                            {:label (:ects-credits higher-base-education-module-texts)
                                                                                                                                             :value "ECTS credits"}
                                                                                                                                            {:label (:study-weeks higher-base-education-module-texts)
                                                                                                                                             :value "Study weeks"}
                                                                                                                                            {:label (:competence-points higher-base-education-module-texts)
                                                                                                                                             :value "Competence points"}
                                                                                                                                            {:label (:hours higher-base-education-module-texts)
                                                                                                                                             :value "Hours"}
                                                                                                                                            {:label (:weekly-lessons higher-base-education-module-texts)
                                                                                                                                             :value "Weekly lessons per year"}
                                                                                                                                            {:label (:years higher-base-education-module-texts)
                                                                                                                                             :value "Years"}]
                                                                                                                               :validators ["required"]})
                                                                                                                       (merge (text-field metadata)
                                                                                                                              {:id         "pohjakoulutus_amt--qualification"
                                                                                                                               :label      (:qualification higher-base-education-module-texts)
                                                                                                                               :validators ["required"]})
                                                                                                                       (merge (text-field metadata)
                                                                                                                              {:id         "pohjakoulutus_amt--institution"
                                                                                                                               :label      (:educational-institution higher-base-education-module-texts)
                                                                                                                               :validators ["required"]})
                                                                                                                       (merge (attachment metadata)
                                                                                                                              {:id     "pohjakoulutus_amt--attachment"
                                                                                                                               :label  (:finnish-special-attachment higher-base-education-module-texts)
                                                                                                                               :params {:info-text {:label    (:submit-your-attachments higher-base-education-module-texts)
                                                                                                                                                    :enabled? true}}})]})
                                                                                                    (merge (info-element metadata)
                                                                                                           {:text (:click-to-add-more higher-base-education-module-texts)})]}]})]}
                                                        {:label     (:finnish-higher-education higher-base-education-module-texts)
                                                         :value     "pohjakoulutus_kk"
                                                         :followups [(merge (single-choice-button metadata)
                                                                            {:id         "pohjakoulutus_kk"
                                                                             :label      (:finnish-higher-education-1995-or-after higher-base-education-module-texts)
                                                                             :options    [{:label     (:yes general-texts)
                                                                                           :value     "Yes"
                                                                                           :followups [(merge (info-element metadata)
                                                                                                              {:text (:automatic-higher-qualification-info higher-base-education-module-texts)})]}
                                                                                          {:label     (:no general-texts)
                                                                                           :value     "No"
                                                                                           :followups [(merge (question-group metadata)
                                                                                                              {:id       "pohjakoulutus_kk--followups"
                                                                                                               :children [(merge (dropdown metadata)
                                                                                                                                 {:id              "pohjakoulutus_kk--degree-level"
                                                                                                                                  :koodisto-source {:uri     "kktutkinnot"
                                                                                                                                                    :title   "Kk-tutkinnot"
                                                                                                                                                    :version 1}
                                                                                                                                  :validators      ["required"]
                                                                                                                                  :label           (:finnish-higher-education-degree-level higher-base-education-module-texts)})
                                                                                                                          (merge (text-field metadata)
                                                                                                                                 {:id         "pohjakoulutus_kk--completion-date"
                                                                                                                                  :label      (:year-and-date-of-completion higher-base-education-module-texts)
                                                                                                                                  :params     {:size "S" :numeric true}
                                                                                                                                  :validators ["numeric" "required"]})
                                                                                                                          (merge (dropdown metadata)
                                                                                                                                 {:id              "pohjakoulutus_kk--degree"
                                                                                                                                  :koodisto-source {:uri     "tutkinto"
                                                                                                                                                    :title   "Tutkinto"
                                                                                                                                                    :version 1}
                                                                                                                                  :validators      ["required"]
                                                                                                                                  :label           (:degree higher-base-education-module-texts)})
                                                                                                                          (merge (text-field metadata)
                                                                                                                                 {:id         "pohjakoulutus_kk--institution"
                                                                                                                                  :label      (:higher-education-institution higher-base-education-module-texts)
                                                                                                                                  :validators ["required"]})
                                                                                                                          (merge (attachment metadata)
                                                                                                                                 {:id     "pohjakoulutus_kk--attachment"
                                                                                                                                  :label  (:higher-education-degree higher-base-education-module-texts)
                                                                                                                                  :params {:info-text {:label    (:submit-your-attachments higher-base-education-module-texts)
                                                                                                                                                       :enabled? true}}})]})]}]
                                                                             :validators ["required"]})]}
                                                        {:label     (:international-marticulation-outside-finland higher-base-education-module-texts)
                                                         :value     "pohjakoulutus_yo_ulkomainen"
                                                         :followups [(merge (text-field metadata)
                                                                            {:id         "pohjakoulutus_yo_ulkomainen--year-of-completion"
                                                                             :label      {:en "Year of completion" :fi "Suoritusvuosi" :sv "Avlagd år"}
                                                                             :params     {:size "S" :numeric true}
                                                                             :validators ["numeric" "required"]})
                                                                     (merge (dropdown metadata)
                                                                            {:id         "pohjakoulutus_yo_ulkomainen--name-of-examination"
                                                                             :label      (:internationa-marticulation-outside-finland-name higher-base-education-module-texts)
                                                                             :options    [{:label (:international-baccalaureate higher-base-education-module-texts)
                                                                                           :value "International Baccalaureate -diploma"}
                                                                                          {:label (:european-baccalaureate higher-base-education-module-texts)
                                                                                           :value "European Baccalaureate -diploma"}
                                                                                          {:label (:reifeprufung higher-base-education-module-texts)
                                                                                           :value "Reifeprüfung - diploma"}]
                                                                             :validators ["required"]})
                                                                     (merge (text-field metadata)
                                                                            {:id         "pohjakoulutus_yo_ulkomainen--institution"
                                                                             :label      (:educational-institution higher-base-education-module-texts)
                                                                             :validators ["required"]})
                                                                     (merge (dropdown metadata)
                                                                            {:id              "pohjakoulutus_yo_ulkomainen--country-of-completion"
                                                                             :koodisto-source {:uri "maatjavaltiot2" :title "Maat ja valtiot" :version 1}
                                                                             :validators      ["required"]
                                                                             :label           (:country-of-completion higher-base-education-module-texts)})]}
                                                        {:label     (:higher-education-outside-finland higher-base-education-module-texts)
                                                         :value     "pohjakoulutus_kk_ulk"
                                                         :followups [(merge (question-group metadata)
                                                                            {:id       "pohjakoulutus_kk_ulk"
                                                                             :children [(merge (dropdown metadata)
                                                                                               {:id              "pohjakoulutus_kk_ulk--level-of-degree"
                                                                                                :koodisto-source {:uri     "kktutkinnot"
                                                                                                                  :title   "Kk-tutkinnot"
                                                                                                                  :version 1}
                                                                                                :validators      ["required"]
                                                                                                :label           (:level-of-degree higher-base-education-module-texts)})
                                                                                        (merge (text-field metadata)
                                                                                               {:id         "pohjakoulutus_kk_ulk--year-of-completion"
                                                                                                :label      (:year-and-date-of-completion higher-base-education-module-texts)
                                                                                                :params     {:size "S" :numeric true}
                                                                                                :validators ["numeric" "required"]})
                                                                                        (merge (text-field metadata)
                                                                                               {:id         "pohjakoulutus_kk_ulk--degree"
                                                                                                :label      (:degree higher-base-education-module-texts)
                                                                                                :validators ["required"]})
                                                                                        (merge (text-field metadata)
                                                                                               {:id         "pohjakoulutus_kk_ulk--institution"
                                                                                                :label      (:educational-institution higher-base-education-module-texts)
                                                                                                :validators ["required"]})
                                                                                        (merge (dropdown metadata)
                                                                                               {:id              "pohjakoulutus_kk_ulk--country"
                                                                                                :koodisto-source {:uri     "maatjavaltiot2"
                                                                                                                  :title   "Maat ja valtiot"
                                                                                                                  :version 1}
                                                                                                :validators      ["required"]
                                                                                                :label           (:country-of-completion higher-base-education-module-texts)})
                                                                                        (merge (attachment metadata)
                                                                                               {:id     "pohjakoulutus_kk_ulk--attachement"
                                                                                                :label  (:higher-education-outside-finland higher-base-education-module-texts)
                                                                                                :params {:info-text {:label    (:submit-your-attachments higher-base-education-module-texts)
                                                                                                                     :enabled? true}}})]})
                                                                     (merge (info-element metadata)
                                                                            {:text (:click-to-add-more higher-base-education-module-texts)})]}
                                                        {:label     (:other-qualification-foreign higher-base-education-module-texts)
                                                         :value     "pohjakoulutus_ulk"
                                                         :followups [(merge (question-group metadata)
                                                                            {:id       "pohjakoulutus_ulk"
                                                                             :children [(merge (text-field metadata)
                                                                                               {:id         "pohjakoulutus_ulk--year-of-completion"
                                                                                                :label      (:year-of-completion higher-base-education-module-texts)
                                                                                                :params     {:size "S"}
                                                                                                :validators ["required"]})
                                                                                        (merge (text-field metadata)
                                                                                               {:id         "pohjakoulutus_ulk--degree"
                                                                                                :label      (:qualification higher-base-education-module-texts)
                                                                                                :validators ["required"]})
                                                                                        (merge (text-field metadata)
                                                                                               {:id         "pohjakoulutus_ulk--institution"
                                                                                                :label      (:educational-institution higher-base-education-module-texts)
                                                                                                :validators ["required"]})
                                                                                        (merge (dropdown metadata)
                                                                                               {:id              "pohjakoulutus_ulk--country-of-completion"
                                                                                                :koodisto-source {:uri     "maatjavaltiot2"
                                                                                                                  :title   "Maat ja valtiot"
                                                                                                                  :version 1}
                                                                                                :validators      ["required"]
                                                                                                :label           (:country-of-completion higher-base-education-module-texts)})
                                                                                        (merge (attachment metadata)
                                                                                               {:id     "pohjakoulutus_ulk--attachment"
                                                                                                :label  (:other-qualification-foreign-attachment higher-base-education-module-texts)
                                                                                                :params {:info-text {:label    (:submit-your-attachments higher-base-education-module-texts)
                                                                                                                     :enabled? true}}})]})
                                                                     (merge (info-element metadata)
                                                                            {:text (:click-to-add-more higher-base-education-module-texts)})]}
                                                        {:label     (:base-education-open higher-base-education-module-texts)
                                                         :value     "pohjakoulutus_avoin"
                                                         :followups [(merge (question-group metadata)
                                                                            {:id       "pohjakoulutus_avoin"
                                                                             :label    (:base-education-open-studies higher-base-education-module-texts)
                                                                             :children [(merge (text-field metadata)
                                                                                               {:id         "pohjakoulutus_avoin--field"
                                                                                                :label      (:field higher-base-education-module-texts)
                                                                                                :params     {:size "M"}
                                                                                                :validators ["required"]})
                                                                                        (merge (text-field metadata)
                                                                                               {:id         "pohjakoulutus_avoin--institution"
                                                                                                :label      (:higher-education-institution higher-base-education-module-texts)
                                                                                                :params     {:size "M"}
                                                                                                :validators ["required"]})
                                                                                        (merge (text-field metadata)
                                                                                               {:id         "pohjakoulutus_avoin--module"
                                                                                                :label      (:module higher-base-education-module-texts)
                                                                                                :params     {:size "M"}
                                                                                                :validators ["required"]})
                                                                                        (merge (text-field metadata)
                                                                                               {:id         "pohjakoulutus_avoin--scope"
                                                                                                :label      (:scope-of-qualification higher-base-education-module-texts)
                                                                                                :params     {:size "S" :numeric true}
                                                                                                :validators ["required" "numeric"]})
                                                                                        (merge (attachment metadata)
                                                                                               {:id     "pohjakoulutus_avoin--attachment"
                                                                                                :label  (:base-education-open-attachment higher-base-education-module-texts)
                                                                                                :params {:info-text {:label    (:submit-your-attachments higher-base-education-module-texts)
                                                                                                                     :enabled? true}}})]})
                                                                     (merge (info-element metadata)
                                                                            {:text (:click-to-add-more higher-base-education-module-texts)})]}
                                                        {:label     (:base-education-other higher-base-education-module-texts)
                                                         :value     "pohjakoulutus_muu"
                                                         :followups [(merge (text-field metadata)
                                                                            {:id         "pohjakoulutus_muu--year-of-completion"
                                                                             :label      (:year-of-completion higher-base-education-module-texts)
                                                                             :params     {:size "S" :numeric true}
                                                                             :validators ["numeric" "required"]})
                                                                     (merge (text-area metadata)
                                                                            {:id         "pohjakoulutus_muu--description"
                                                                             :label      (:base-education-other-description higher-base-education-module-texts)
                                                                             :params     {:max-length "500"}
                                                                             :validators ["required"]})
                                                                     (merge (dropdown metadata)
                                                                            {:id     "pohjakoulutus_muu--attachment"
                                                                             :label  (:base-education-other-attachment higher-base-education-module-texts)
                                                                             :params {:info-text {:label    (:submit-your-attachments higher-base-education-module-texts)
                                                                                                  :enabled? true}}})]}]})
                     (merge (single-choice-button metadata)
                            {:id         "secondary-completed-base-education"
                             :label      (:secondary-completed-base-education higher-base-education-module-texts)
                             :params     {:info-text {:label    (:required-for-statistics higher-base-education-module-texts)
                                                      :enabled? true}}
                             :options    [{:label     (:yes general-texts)
                                           :value     "Yes"
                                           :followups [(merge (dropdown metadata)
                                                              {:id              "secondary-completed-base-education--country"
                                                               :params          {:info-text {:label (:secondary-completed-country higher-base-education-module-texts)}}
                                                               :koodisto-source {:uri "maatjavaltiot2" :title "Maat ja valtiot" :version 1}
                                                               :validators      ["required"]
                                                               :label           (:choose-country higher-base-education-module-texts)})]}
                                          {:label (:no general-texts) :value "No"}]
                             :validators ["required"]})
                     (merge (single-choice-button metadata)
                            {:id         "finnish-vocational-before-1995"
                             :label      (:finnish-vocational-before-1995 higher-base-education-module-texts)
                             :params     {:info-text {:label (:finnish-vocational-before-1995-degree higher-base-education-module-texts)}}
                             :options    [{:label     (:yes general-texts)
                                           :value     "Yes"
                                           :followups [(merge (text-field metadata)
                                                              {:id         "finnish-vocational-before-1995--year-of-completion"
                                                               :label      (:year-of-completion higher-base-education-module-texts)
                                                               :params     {:size "S" :numeric true}
                                                               :validators ["numeric" "required"]})
                                                       (merge (dropdown metadata)
                                                              {:id              "finnish-vocational-before-1995--degree"
                                                               :koodisto-source {:uri "tutkinto" :title "Tutkinto" :version 1}
                                                               :validators      ["required"]
                                                               :label           (:name-of-degree higher-base-education-module-texts)})
                                                       (merge (text-field metadata)
                                                              {:id         "finnish-vocational-before-1995--other-institution"
                                                               :label      (:higher-education-institution higher-base-education-module-texts)
                                                               :validators ["required"]})]}
                                          {:label (:no general-texts) :value "No"}]
                             :validators ["required"]})]}))

