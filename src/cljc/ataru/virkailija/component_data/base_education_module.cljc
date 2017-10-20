(ns ataru.virkailija.component-data.base-education-module
  (:require [ataru.util :as u]
            [ataru.virkailija.component-data.component :as component]))

(defn eligibility []
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
                                                               :fieldClass "formField"}
                                                              {:id         (u/component-id)
                                                               :label      {:en "Attachment request for higher education completed in Finland"
                                                                            :fi "Liitepyyntö Suomessa suoritetusta kk-tutkinnosta"
                                                                            :sv "Begäran om bilagan för en universitetsexamen i Finland"}
                                                               :params     {}
                                                               :options    []
                                                               :fieldType  "attachment"
                                                               :fieldClass "formField"
                                                               :validators ["required"]}]
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
                      :validators ["required"]}]}))

