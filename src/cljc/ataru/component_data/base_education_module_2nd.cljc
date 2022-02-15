(ns ataru.component-data.base-education-module-2nd
  (:require [ataru.util :as util]
            [ataru.translations.texts :refer [base-education-2nd-module-texts general-texts]]
            [ataru.component-data.component :as component :refer [harkinnanvaraisuus-wrapper-id]]))

(defn- base-education-language-question
  [metadata]
  (merge (component/dropdown metadata)
    {:label (:study-language base-education-2nd-module-texts)
     :validators ["required"],
     :options
     [{:label (:language-finnish base-education-2nd-module-texts) :value "0"}
      {:label (:language-swedish base-education-2nd-module-texts) :value "1"}
      {:label (:language-saame base-education-2nd-module-texts) :value "2"}
      {:label (:language-english base-education-2nd-module-texts) :value "3"}
      {:label (:language-german base-education-2nd-module-texts) :value "4"}]}))

(defn- suorittanut-tutkinnon-question
  [metadata]
  (merge (component/single-choice-button metadata)
  {:label (:graduated-question base-education-2nd-module-texts)
   :validators ["required" "invalid-values"],
   :params {:invalid-values ["0"]},
   :options
   [{:label (:yes general-texts),
     :value "0",
     :followups
     [{:label {:fi ""},
       :text (:graduated-notification base-education-2nd-module-texts)
       :button-text (:close general-texts),
       :fieldClass "modalInfoElement",
       :id (util/component-id),
       :params {},
       :fieldType "p"
       :metadata metadata}]}
    {:label (:no general-texts) :value "1"}]}))

(defn- jos-olet-suorittanut-question
  [metadata]
  (merge (component/single-choice-button metadata)
  {:label (:graduated-question-conditional base-education-2nd-module-texts)
   :options
   [{:label (:tenth-grade base-education-2nd-module-texts)
     :value "0",
     :followups
     [{:label (:year-of-graduation base-education-2nd-module-texts)
       :validators ["required"],
       :fieldClass "formField",
       :id (util/component-id)
       :params {:size "S"}
       :metadata metadata
       :fieldType "textField"}]}
    {:label (:valma base-education-2nd-module-texts)
     :value "1",
     :followups
     [{:label (:year-of-graduation base-education-2nd-module-texts)
       :validators ["required"],
       :fieldClass "formField",
       :id (util/component-id)
       :params {:size "S"},
       :metadata metadata
       :fieldType "textField"}]}
    {:label (:luva base-education-2nd-module-texts)
     :value "2",
     :followups
     [{:label (:year-of-graduation base-education-2nd-module-texts)
       :validators ["required"],
       :fieldClass "formField",
       :id (util/component-id)
       :params {:size "S"},
       :metadata metadata
       :fieldType "textField"}]}
    {:label (:kansanopisto base-education-2nd-module-texts)
     :value "3",
     :followups
     [{:label (:year-of-graduation base-education-2nd-module-texts)
       :validators ["required"],
       :fieldClass "formField",
       :id (util/component-id)
       :params {:size "S"},
       :metadata metadata
       :fieldType "textField"}]}
    {:label (:free-civilized base-education-2nd-module-texts)
     :value "4",
     :followups
     [{:label (:year-of-graduation base-education-2nd-module-texts)
       :validators ["required"],
       :fieldClass "formField",
       :id (util/component-id)
       :params {:size "S"},
       :metadata metadata
       :fieldType "textField"}]}]}))

(defn- suoritusvuosi-question
  [metadata]
  (merge (component/text-field metadata)
  {:validators ["numeric" "required"],
   :label (:year-of-graduation-question base-education-2nd-module-texts)
   :params {:size "S",
            :max-value "2022",
            :numeric true,
            :min-value "1990"},
   :section-visibility-conditions
   [{:section-name "arvosanat-peruskoulu",
     :condition
     {:comparison-operator ">", :answer-compared-to 2017}}],
   :options
   [{:label {:fi "", :sv ""},
     :value "0",
     :followups
     [(suorittanut-tutkinnon-question metadata)
      (jos-olet-suorittanut-question metadata)]
     :condition {:comparison-operator "<", :answer-compared-to 2022}}]}))

(defn- yksilollistetty-question
  [metadata id]
  (merge (component/single-choice-button metadata)
  {
   :label (:individualized-question base-education-2nd-module-texts)
   :validators ["required"],
   :params {
            :info-text {
                        :label (:individualized-info base-education-2nd-module-texts)}},
   :id id,
   :section-visibility-conditions
   [{:section-name harkinnanvaraisuus-wrapper-id,
     :condition
     {:comparison-operator "=",
      :data-type "str",
      :answer-compared-to "1"}}],
   :options
   [{:label (:no general-texts) :value "0"}
    {:label (:yes general-texts)
     :value "1",
     :followups
     [(merge (component/info-element metadata)
             {:text (:individualized-harkinnanvaraisuus base-education-2nd-module-texts)
              })]}]}))

(defn- ulkomailla-harkinnanvarainen-info
  [metadata]
  (merge (component/info-element metadata)
         {:text (:foreign-harkinnanvaraisuus base-education-2nd-module-texts)}))

(defn- kopio-tutkintotodistuksesta-attachment
  [metadata]
  (merge (component/attachment metadata)
  {:label (:copy-of-proof-of-certificate base-education-2nd-module-texts)
   :validators [],
   :params {:deadline "29.3.2022 15:00",
            :mail-attachment? false,
            :info-text {:enabled? true,
                        :value (:copy-of-proof-of-certificate-info base-education-2nd-module-texts)}}}))

(defn- kopio-todistuksesta-attachment
  [metadata]
  (merge (component/attachment metadata)
         {:label (:copy-of-certificate base-education-2nd-module-texts)
          :params {:deadline "29.3.2022 15:00",
                   :info-text {:enabled? true,
                               :value (:copy-of-certificate-info base-education-2nd-module-texts)}}}))

(defn- ei-paattotodistusta-info
  [metadata]
  (assoc (component/info-element metadata)
         :text (:no-graduation-info base-education-2nd-module-texts)))

(defn- perusopetus-option
  [metadata]
  {:label (:base-education base-education-2nd-module-texts)
   :value "1",
   :followups
   [(base-education-language-question metadata)
    (suoritusvuosi-question metadata)]})

(defn- perusopetuksen-osittain-yksilollistetty-option
  [metadata]
  {:label (:base-education-partially-individualized base-education-2nd-module-texts)
   :value "2",
   :followups
   [(base-education-language-question metadata)
    (suoritusvuosi-question metadata)
    (yksilollistetty-question metadata "matematiikka-ja-aidinkieli-yksilollistetty_1")]})

(defn- perusopetuksen-yksilollistetty-option
  [metadata]
  {:label (:base-education-individualized base-education-2nd-module-texts)
   :value "6",
   :followups
   [(base-education-language-question metadata)
    (suoritusvuosi-question metadata)
    (yksilollistetty-question metadata "matematiikka-ja-aidinkieli-yksilollistetty_2")]})

(defn- opetus-jarjestetty-toiminta-alueittan-option
  [metadata]
  {:label (:base-education-organized-regionly base-education-2nd-module-texts)
   :value "3",
   :followups
   [(base-education-language-question metadata)
    (suoritusvuosi-question metadata)]})

(defn- ulkomailla-suoritettu-option
  [metadata]
  {:label (:base-education-foreign base-education-2nd-module-texts)
   :value "0",
   :followups
   [(ulkomailla-harkinnanvarainen-info metadata)
    (suorittanut-tutkinnon-question metadata)
    (kopio-tutkintotodistuksesta-attachment metadata)]})

(defn- ei-paattotodistusta-option
  [metadata]
  {:label (:base-education-no-graduation base-education-2nd-module-texts)
   :value "7",
   :followups
   [(ei-paattotodistusta-info metadata)
    (suorittanut-tutkinnon-question metadata)
    (kopio-todistuksesta-attachment metadata)]})

(defn- base-education-question
  [metadata]
  (assoc (component/single-choice-button metadata)
    :id "base-education-2nd"
    :label (:choose-base-education base-education-2nd-module-texts)
    :koodisto-source {
                      :uri "2asteenpohjakoulutus2021"
                      :version 1
                      :title "2. asteen pohjakoulutus (2021)"
                      :allow-invalid? false}
    :koodisto-ordered-by-user true
    :validators ["required"]
    :params
     {:info-text
      {:label (:choose-base-education-info base-education-2nd-module-texts)}}
    :section-visibility-conditions
      [{:section-name harkinnanvaraisuus-wrapper-id
        :condition
        {:comparison-operator "=",
         :data-type "str",
         :answer-compared-to "0"}}
       {:section-name harkinnanvaraisuus-wrapper-id
        :condition
        {:comparison-operator "=",
         :data-type "str",
         :answer-compared-to "7"}}]
      :options [(perusopetus-option metadata)
                (perusopetuksen-osittain-yksilollistetty-option metadata)
                (perusopetuksen-yksilollistetty-option metadata)
                (opetus-jarjestetty-toiminta-alueittan-option metadata)
                (ulkomailla-suoritettu-option metadata)
                (ei-paattotodistusta-option metadata)]
    ))

(defn base-education-2nd-module [metadata]
  (assoc (component/form-section metadata)
         :id "pohjakoulutus-2nd-wrapper"
         :label (:section-title base-education-2nd-module-texts)
         :children [(base-education-question metadata)]))

