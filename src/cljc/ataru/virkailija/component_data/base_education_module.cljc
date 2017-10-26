(ns ataru.virkailija.component-data.base-education-module
  (:require [ataru.util :as u]
            [ataru.virkailija.component-data.component :as component]))

(defn module []
  (merge (component/form-section)
         {:label    {:fi "Koulutustausta"
                     :sv "Utbildningsbakgrund"
                     :en "Eligibility"}
          :children [{:id         "completed-base-education"
                      :label      {:en "Fill in the education that you have completed  or will complete during the application term."
                                   :fi "Merkitse suorittamasi pohjakoulutukset, myös ne jotka suoritat hakukautena. "
                                   :sv "Ange avlagda grundutbildningar, samt de som du avlägger under ansökningsperioden"}
                      :params     {}
                      :options    [{:label     {:en "Higher education qualification completed in Finland"
                                                :fi "Suomessa suoritettu korkeakoulututkinto "
                                                :sv "Högskoleexamen som avlagts i Finland"}
                                    :value     "Högskoleexamen som avlagts i Finland"
                                    :followups [{:id         "higher-education-qualification-in-finland"
                                                 :label      {:fi "", :sv ""}
                                                 :params     {}
                                                 :children   [{:id              "higher-education-qualification-in-finland-level"
                                                               :label           {:en "Qualification level", :fi "Tutkintotaso", :sv "Examensnivå"}
                                                               :params          {}
                                                               :options         [{:label {:fi "", :sv ""}, :value ""}]
                                                               :fieldType       "dropdown"
                                                               :fieldClass      "formField"
                                                               :validators      ["required"]
                                                               :koodisto-source {:uri "kktutkinnot", :title "Kk-tutkinnot", :version 1}}
                                                              {:id         "higher-education-qualification-in-finland-year-and-date"
                                                               :label      {:en "Year and date of completion"
                                                                            :fi "Suoritusvuosi ja päivämäärä"
                                                                            :sv "År och datum då examen avlagts"}
                                                               :params     {}
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id              "higher-education-qualification-in-finland-qualification"
                                                               :label           {:en "Qualification/degree", :fi "Tutkinto", :sv "Examen"}
                                                               :params          {}
                                                               :options         [{:label {:fi "", :sv ""}, :value ""}]
                                                               :fieldType       "dropdown"
                                                               :validators      ["required"]
                                                               :fieldClass      "formField"
                                                               :koodisto-source {:uri "tutkinto", :title "Tutkinto", :version 1}}
                                                              {:id         "higher-education-qualification-in-finland-institution"
                                                               :label      {:en "Higher education institution", :fi "Korkeakoulu", :sv "Högskola"}
                                                               :params     {}
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}]
                                                 :fieldType  "fieldset"
                                                 :fieldClass "questionGroup"}]}
                                   {:label     {:en "Studies required by the higher education institution completed at open university or open university of applied sciences (UAS)"
                                                :fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot "
                                                :sv "Studier som högskolan kräver vid en öppen högskola"}
                                    :value     "Studier som högskolan kräver vid en öppen högskola"
                                    :followups [{:id         "studies-required-by-higher-education"
                                                 :label      {:fi "", :sv ""}
                                                 :params     {}
                                                 :children   [{:id         "studies-required-by-higher-education-field"
                                                               :label      {:en "Field", :fi "Ala", :sv "Bransch"}
                                                               :params     {}
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         "studies-required-by-higher-education-study-module"
                                                               :label      {:en "Study module", :fi "Opintokokonaisuus ", :sv "Studiehelhet"}
                                                               :params     {}
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         "studies-required-by-higher-education-scope"
                                                               :label      {:en "Scope", :fi "Laajuus ", :sv "Omfattning"}
                                                               :params     {}
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         "studies-required-by-higher-education-institution"
                                                               :label      {:en "Higher education institution", :fi "Korkeakoulu", :sv "Högskola"}
                                                               :params     {}
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}]
                                                 :fieldType  "fieldset"
                                                 :fieldClass "questionGroup"}]}
                                   {:label     {:en "Higher education qualification completed outside Finland"
                                                :fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto "
                                                :sv "Högskoleexamen som avlagts annanstans än i Finland"}
                                    :value     "Högskoleexamen som avlagts annanstans än i Finland"
                                    :followups [{:id         "higher-education-qualification-outside-finland"
                                                 :label      {:fi "", :sv ""}
                                                 :params     {}
                                                 :children   [{:id              "higher-education-qualification-outside-finland-level"
                                                               :label           {:en "Qualification level", :fi "Tutkintotaso", :sv "Examensnivå"}
                                                               :params          {}
                                                               :options         [{:label {:fi "", :sv ""}, :value ""}]
                                                               :fieldType       "dropdown"
                                                               :fieldClass      "formField"
                                                               :validators      ["required"]
                                                               :koodisto-source {:uri "kktutkinnot", :title "Kk-tutkinnot", :version 1}}
                                                              {:id         "higher-education-qualification-outside-finland-year-and-date"
                                                               :label      {:en "Year and date of completion"
                                                                            :fi "Suoritusvuosi ja päivämäärä"
                                                                            :sv "År och datum då examen avlagts"}
                                                               :params     {}
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         "higher-education-qualification-outside-finland-qualification"
                                                               :label      {:en "Qualification/degree", :fi "Tutkinto", :sv "Examen"}
                                                               :params     {}
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         "higher-education-qualification-outside-finland-institution"
                                                               :label      {:en "Higher education institution", :fi "Korkeakoulu ", :sv "Högskola"}
                                                               :params     {}
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id              "higher-education-qualification-outside-finland-country"
                                                               :label           {:en "Country where the qualification has been awarded"
                                                                                 :fi "Suoritusmaa"
                                                                                 :sv "Land där examen har avlagts"}
                                                               :params          {}
                                                               :validators      ["required"]
                                                               :options         [{:label {:fi "", :sv ""}, :value ""}]
                                                               :fieldType       "dropdown"
                                                               :fieldClass      "formField"
                                                               :koodisto-source {:uri "maatjavaltiot2", :title "Maat ja valtiot", :version 1}}]
                                                 :fieldType  "fieldset"
                                                 :fieldClass "questionGroup"}]}
                                   {:label     {:en "Other eligibility for higher education"
                                                :fi "Muu korkeakoulukelpoisuus"
                                                :sv "Övrig högskolebehörighet"}
                                    :value     "Övrig högskolebehörighet"
                                    :followups [{:id         "other-eligibility-question-group"
                                                 :label      {:fi "", :sv ""}
                                                 :params     {}
                                                 :children   [{:id         "other-eligibility-year-of-completion"
                                                               :label      {:en "Year of completion", :fi "Suoritusvuosi", :sv "Avlagd år"}
                                                               :params     {:size "S"}
                                                               :validators ["required"]
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         "other-eligibility-description"
                                                               :label      {:en "Describe eligibility"
                                                                            :fi "Kelpoisuuden kuvaus"
                                                                            :sv "Beskrivning av behörigheten"}
                                                               :params     {:size "M"}
                                                               :validators ["required"]
                                                               :fieldType  "textArea"
                                                               :fieldClass "formField"}]
                                                 :fieldType  "fieldset"
                                                 :fieldClass "questionGroup"}]}]
                      :fieldType  "multipleChoice"
                      :fieldClass "formField"
                      :validators ["required"]}
                     (merge (component/single-choice-button)
                            {:label      {:en "Have you completed general upper secondary education or vocational qualification?",
                                          :fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon?",
                                          :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"},
                             :id         "upper-secondary-school-completed"
                             :params     {},
                             :options    [{:label     {:en "Yes", :fi "Kyllä", :sv "Ja"},
                                           :value     "Ja",
                                           :followups [{:id              "upper-secondary-school-completed-country",
                                                        :label           {:en "Choose country", :fi "Valitse suoritusmaa", :sv " Välj land"},
                                                        :params          {:info-text {:label {:en "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification, but are in the process of doing so, please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification, school-level qualification, post-secondary level qualification, higher vocational level qualification, further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification.",
                                                                                              :fi "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa mutta olet suorittamassa sellaista, valitse se maa, jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto, ammatti-tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata.",
                                                                                              :sv "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det, välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå, yrkesinriktad examen på högre nivå, yrkesexamen eller specialyrkesexamen. Ange inte land där du avlagt högskoleexamen."}}},
                                                        :options         [{:label {:fi "", :sv ""}, :value ""}],
                                                        :fieldType       "dropdown",
                                                        :fieldClass      "formField",
                                                        :validators      ["required"]
                                                        :koodisto-source {:uri "maatjavaltiot2", :title "Maat ja valtiot", :version 1}}]}
                                          {:label {:en "No", :fi "En", :sv "Nej"}, :value "Nej"}],
                             :validators ["required"]})]}))
