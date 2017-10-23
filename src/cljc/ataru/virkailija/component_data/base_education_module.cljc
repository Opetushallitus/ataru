(ns ataru.virkailija.component-data.base-education-module
  (:require [ataru.util :as u]
            [ataru.virkailija.component-data.component :as component]))

(defn module []
  (merge (component/form-section)
         {:label    {:fi "Koulutustausta"
                     :sv "Utbildningsbakgrund"
                     :en "Eligibility"}
          :children [{:id         (u/component-id)
                      :label      {:en "Fill in the education that you have completed  or will complete during the application term."
                                   :fi "Merkitse suorittamasi pohjakoulutukset, myös ne jotka suoritat hakukautena. "
                                   :sv "Ange avlagda grundutbildningar, samt de som du avlägger under ansökningsperioden"}
                      :params     {}
                      :options    [{:label     {:en "Higher education qualification completed in Finland"
                                                :fi "Suomessa suoritettu korkeakoulututkinto "
                                                :sv "Högskoleexamen som avlagts i Finland"}
                                    :value     "Högskoleexamen som avlagts i Finland"
                                    :followups [{:id         (u/component-id)
                                                 :label      {:fi "", :sv ""}
                                                 :params     {}
                                                 :children   [{:id              (u/component-id)
                                                               :label           {:en "Qualification level", :fi "Tutkintotaso", :sv "Examensnivå"}
                                                               :params          {}
                                                               :options         [{:label {:fi "", :sv ""}, :value ""}]
                                                               :fieldType       "dropdown"
                                                               :fieldClass      "formField"
                                                               :koodisto-source {:uri "kktutkinnot", :title "Kk-tutkinnot", :version 1}}
                                                              {:id         (u/component-id)
                                                               :label      {:en "Year and date of completion"
                                                                            :fi "Suoritusvuosi ja päivämäärä"
                                                                            :sv "År och datum då examen avlagts"}
                                                               :params     {}
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id              (u/component-id)
                                                               :label           {:en "Qualification", :fi "Tutkinto", :sv "Examen"}
                                                               :params          {}
                                                               :options         [{:label {:fi "", :sv ""}, :value ""}]
                                                               :fieldType       "dropdown"
                                                               :fieldClass      "formField"
                                                               :koodisto-source {:uri "tutkinto", :title "Tutkinto", :version 1}}
                                                              {:id         (u/component-id)
                                                               :label      {:en "Higher education institution", :fi "Korkeakoulu", :sv "Högskola"}
                                                               :params     {}
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}]
                                                 :fieldType  "fieldset"
                                                 :fieldClass "questionGroup"}]}
                                   {:label     {:en "Studies required by the higher education institution completed at open university or open university of applied sciences (UAS)"
                                                :fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot "
                                                :sv "Studier som högskolan kräver vid en öppen högskola"}
                                    :value     "Studier som högskolan kräver vid en öppen högskola"
                                    :followups [{:id         (u/component-id)
                                                 :label      {:fi "", :sv ""}
                                                 :params     {}
                                                 :children   [{:id         (u/component-id)
                                                               :label      {:en "Field", :fi "Ala", :sv "Bransch"}
                                                               :params     {}
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         (u/component-id)
                                                               :label      {:en "Study module", :fi "Opintokokonaisuus ", :sv "Studiehelhet"}
                                                               :params     {}
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         (u/component-id)
                                                               :label      {:en "Scope", :fi "Laajuus ", :sv "Omfattning"}
                                                               :params     {}
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         (u/component-id)
                                                               :label      {:en "Higher education institution", :fi "Korkeakoulu", :sv "Högskola"}
                                                               :params     {}
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}]
                                                 :fieldType  "fieldset"
                                                 :fieldClass "questionGroup"}]}
                                   {:label     {:en "Higher education qualification completed outside Finland"
                                                :fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto "
                                                :sv "Högskoleexamen som avlagts annanstans än i Finland"}
                                    :value     "Högskoleexamen som avlagts annanstans än i Finland"
                                    :followups [{:id         (u/component-id)
                                                 :label      {:fi "", :sv ""}
                                                 :params     {}
                                                 :children   [{:id              (u/component-id)
                                                               :label           {:en "Qualification level", :fi "Tutkintotaso", :sv "Examensnivå"}
                                                               :params          {}
                                                               :options         [{:label {:fi "", :sv ""}, :value ""}]
                                                               :fieldType       "dropdown"
                                                               :fieldClass      "formField"
                                                               :koodisto-source {:uri "kktutkinnot", :title "Kk-tutkinnot", :version 1}}
                                                              {:id         (u/component-id)
                                                               :label      {:en "Year and date of completion"
                                                                            :fi "Suoritusvuosi ja päivämäärä"
                                                                            :sv "År och datum då examen avlagts"}
                                                               :params     {}
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         (u/component-id)
                                                               :label      {:en "Qualification", :fi "Tutkinto", :sv "Examen"}
                                                               :params     {}
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         (u/component-id)
                                                               :label      {:en "Higher education institution", :fi "Korkeakoulu ", :sv "Högskola"}
                                                               :params     {}
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id              (u/component-id)
                                                               :label           {:en "Country where the qualification has been awarded"
                                                                                 :fi "Suoritusmaa"
                                                                                 :sv "Land där examen har avlagts"}
                                                               :params          {}
                                                               :options         [{:label {:fi "", :sv ""}, :value ""}]
                                                               :fieldType       "dropdown"
                                                               :fieldClass      "formField"
                                                               :validators      ["required"]
                                                               :koodisto-source {:uri "maatjavaltiot2", :title "Maat ja valtiot", :version 1}}]
                                                 :fieldType  "fieldset"
                                                 :fieldClass "questionGroup"}]}
                                   {:label     {:en "Other eligibility for higher education"
                                                :fi "Muu korkeakoulukelpoisuus"
                                                :sv "Övrig högskolebehörighet"}
                                    :value     "Övrig högskolebehörighet"
                                    :followups [{:id         (u/component-id)
                                                 :label      {:fi "", :sv ""}
                                                 :params     {}
                                                 :children   [{:id         (u/component-id)
                                                               :label      {:en "Year of completion", :fi "Suoritusvuosi", :sv "Avlagd år"}
                                                               :params     {:size "S"}
                                                               :fieldType  "textField"
                                                               :fieldClass "formField"}
                                                              {:id         (u/component-id)
                                                               :label      {:en "Describe eligibility"
                                                                            :fi "Kelpoisuuden kuvaus"
                                                                            :sv "Beskrivning av behörigheten"}
                                                               :params     {:size "M"}
                                                               :fieldType  "textArea"
                                                               :fieldClass "formField"}]
                                                 :fieldType  "fieldset"
                                                 :fieldClass "questionGroup"}]}]
                      :fieldType  "multipleChoice"
                      :fieldClass "formField"
                      :validators ["required"]}
                     (merge (component/single-choice-button)
                            {:label      {:en "Have you completed general upper secondary school syllabus/matriculation examination or vocational qualification?",
                                          :fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon? ",
                                          :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"},
                             :params     {},
                             :options    [{:label     {:en "Yes", :fi "Kyllä", :sv "Ja"},
                                           :value     "Ja",
                                           :followups [{:id              "aa1329db-5ec7-4106-8d17-105135e3abbc",
                                                        :label           {:en "Choose country", :fi "Valitse suoritusmaa", :sv " Välj land"},
                                                        :params          {:info-text {:label {:en "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification, but are in the process of doing so, please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification, school-level qualification, post-secondary level qualification, higher vocational level qualification, further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification.",
                                                                                              :fi "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa mutta olet suorittamassa sellaista, valitse se maa, jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto, ammatti-tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata.",
                                                                                              :sv "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det, välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå, yrkesinriktad examen på högre nivå, yrkesexamen eller specialyrkesexamen. Ange inte land där du avlagt högskoleexamen."}}},
                                                        :options         [{:label {:fi "", :sv ""}, :value ""}],
                                                        :fieldType       "dropdown",
                                                        :fieldClass      "formField",
                                                        :koodisto-source {:uri "maatjavaltiot2", :title "Maat ja valtiot", :version 1}}]}
                                          {:label {:en "No", :fi "En", :sv "Nej"}, :value "Nej"}],
                             :validators ["required"]})]}))
