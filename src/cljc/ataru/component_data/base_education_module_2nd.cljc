(ns ataru.component-data.base-education-module-2nd
  (:require [ataru.util :as util]
            [ataru.translations.texts :refer [base-education-2nd-module-texts general-texts]]
            [ataru.component-data.component :as component :refer [harkinnanvaraisuus-wrapper-id]]
            [clojure.core.match :refer [match]]))

(def base-education-option-values-affecting-harkinnanvaraisuus
  {:ei-paattotodistusta-value "7"
   :ulkomailla-suoritettu-value "0"})

(def base-education-option-where-harkinnanvaraisuus-do-not-need-to-be-checked
  {:opetusjarjestetty-toiminta-alueittain "3"
   :ulkomailla-suoritettu-value "0"})

(def yksilollistetty-key-values-affecting-harkinnanvaraisuus
  {:matematiikka-ja-aidinkieli-yksilollistetty_1 "1"
   :matematiikka-ja-aidinkieli-yksilollistetty_2 "1"})

(def base-education-choice-key "base-education-2nd")

(def base-education-wrapper-key "pohjakoulutus-2nd-wrapper")

(def tutkintokieli-perusopetus-key "tutkintokieli-perusopetus")
(def suoritusvuosi-perusopetus-key "suoritusvuosi-perusopetus")

(def tutkintokieli-osittain-yks-key "tutkintokieli-osittain-yks")
(def suoritusvuosi-osittain-yks-key "suoritusvuosi-osittain-yks")
(def tutkintokieli-yks-key "tutkintokieli-yks")
(def suoritusvuosi-yks-key "suoritusvuosi-yks")

(def tutkintokieli-toiminta-alueittain "tutkintokieli-toiminta-alueittain")
(def suoritusvuosi-toiminta-alueittain-key "suoritusvuosi-toiminta-alueittain")

(def tutkintokieli-osittain-rajattu "tutkintokieli-osittain-rajattu")
(def suoritusvuosi-osittain-rajattu "suoritusvuosi-osittain-rajattu")

(def tutkintokieli-kokonaan-rajattu "tutkintokieli-kokonaan-rajattu")
(def suoritusvuosi-kokonaan-rajattu "suoritusvuosi-kokonaan-rajattu")

(def tutkintokieli-keys [tutkintokieli-perusopetus-key
                         tutkintokieli-osittain-yks-key
                         tutkintokieli-yks-key
                         tutkintokieli-toiminta-alueittain
                         tutkintokieli-osittain-rajattu
                         tutkintokieli-kokonaan-rajattu

                         "56e05f17-f289-495b-9f83-5dff310cb35b"
                         "daf29a8e-19b2-4922-b8cb-ac80ea10593b"
                         "c1d72123-173f-492e-9edc-f0141714c609"
                         "b4f96c06-7872-4c9b-b871-315dc1cc0395"
                         
                         ;; 2026 lomake
                         "ced8dc18-668c-4024-afe9-2b857a3c80ee"
                         "66974add-8ed7-465a-a1fc-029af1f8dc26"
                         "e6488859-aba3-4199-9598-6378fd5becbe"
                         "4b71d515-d6e3-4782-8716-4735bfcf1363"
                         "9ed1bd1e-2db8-4529-9354-8767300d7ad8"
                         "33b07a21-fd38-4236-a264-3fd26b9958b4"])

(def suoritusvuosi-keys [suoritusvuosi-perusopetus-key
                         suoritusvuosi-osittain-yks-key
                         suoritusvuosi-yks-key
                         suoritusvuosi-toiminta-alueittain-key
                         suoritusvuosi-osittain-rajattu
                         suoritusvuosi-kokonaan-rajattu

                         "b5a683d9-21aa-419f-a6d9-a65c42ff1b29"
                         "ebb7fd12-e762-40e3-ad40-a1f9136728d5"
                         "bc159ab3-2f23-41ca-8b05-4b8573d408e7"
                         "42725ecd-95c4-4ec8-bdd0-a7ad881ee5f1"

                         ;; 2026 lomake
                         "ca6623c9-6eec-4dc7-b7cd-08ae260bab8a"
                         "3156cb2e-7d83-4dd9-a96b-944193e13a75"
                         "7cf6e34f-5bce-4b3c-81a2-49f0e1b80806"
                         "429056c7-ffa6-4d73-a027-8b930556adc1"
                         "d6acad27-a2fa-4371-a551-904d5b601139"
                         "34f4c066-1551-4b0b-99c6-82751c4188e2"])

(def matematiikka-ja-aidinkieli-yksilollistetty-keys ["matematiikka-ja-aidinkieli-yksilollistetty_1"
                                                      "matematiikka-ja-aidinkieli-yksilollistetty_2"
                                                      "ed68f9be-81e7-4d36-b995-4edb220bd52a"
                                                      "22cf9706-21d8-4a6f-ba66-3481591fa43f"])

(defn base-education-2nd-language-value-to-lang
  [value]
  (match [value]
         ["0"] "fi"
         ["1"] "sv"
         ["2"] "sa"
         ["3"] "en"
         ["4"] "de"
         :else nil))

(defn- base-education-language-question
  [metadata id]
  (merge (component/dropdown metadata)
    {:id id
     :label (:study-language base-education-2nd-module-texts)
     :validators ["required"]
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
   :validators ["required" "invalid-values"]
   :params {:invalid-values ["0"]}
   :options
   [{:label (:yes general-texts)
     :value "0"
     :followups
     [{:label {:fi ""}
       :text (:graduated-notification base-education-2nd-module-texts)
       :button-text (:close general-texts)
       :fieldClass "modalInfoElement"
       :id (util/component-id)
       :params {}
       :fieldType "p"
       :metadata metadata}]}
    {:label (:no general-texts) :value "1"}]}))

(defn- jos-olet-suorittanut-question
  [metadata]
  (merge (component/single-choice-button metadata)
  {:label (:graduated-question-conditional base-education-2nd-module-texts)
   :options
   [{:label (:tenth-grade base-education-2nd-module-texts)
     :value "0"
     :followups
     [{:label (:year-of-graduation base-education-2nd-module-texts)
       :validators ["required"]
       :fieldClass "formField"
       :id (util/component-id)
       :params {:size "S"}
       :metadata metadata
       :fieldType "textField"}]}
    {:label (:valma base-education-2nd-module-texts)
     :value "1"
     :followups
     [{:label (:year-of-graduation base-education-2nd-module-texts)
       :validators ["required"]
       :fieldClass "formField"
       :id (util/component-id)
       :params {:size "S"}
       :metadata metadata
       :fieldType "textField"}]}
    {:label (:luva base-education-2nd-module-texts)
     :value "2"
     :followups
     [{:label (:year-of-graduation base-education-2nd-module-texts)
       :validators ["required"]
       :fieldClass "formField"
       :id (util/component-id)
       :params {:size "S"}
       :metadata metadata
       :fieldType "textField"}]}
    {:label (:kansanopisto base-education-2nd-module-texts)
     :value "3"
     :followups
     [{:label (:year-of-graduation base-education-2nd-module-texts)
       :validators ["required"]
       :fieldClass "formField"
       :id (util/component-id)
       :params {:size "S"}
       :metadata metadata
       :fieldType "textField"}]}
    {:label (:free-civilized base-education-2nd-module-texts)
     :value "4"
     :followups
     [{:label (:year-of-graduation base-education-2nd-module-texts)
       :validators ["required"]
       :fieldClass "formField"
       :id (util/component-id)
       :params {:size "S"}
       :metadata metadata
       :fieldType "textField"}]}]}))

(defn- suoritusvuosi-question
  [metadata id]
  (merge (component/text-field metadata)
  {:id id
   :validators ["numeric" "required"]
   :label (:year-of-graduation-question base-education-2nd-module-texts)
   :params {:size "S"
            :max-value "2022"
            :numeric true
            :min-value "1990"}
   :section-visibility-conditions
   [{:section-name "arvosanat-peruskoulu"
     :condition
     {:comparison-operator ">" :answer-compared-to 2017}}]
   :options
   [{:label {:fi "" :sv ""}
     :value "0"
     :followups
     [(suorittanut-tutkinnon-question metadata)
      (jos-olet-suorittanut-question metadata)]
     :condition {:comparison-operator "<" :answer-compared-to 2022}}]}))

(defn- yksilollistetty-question
  [metadata id]
  (merge (component/single-choice-button metadata)
  {
   :label (:individualized-question base-education-2nd-module-texts)
   :validators ["required"]
   :params {:info-text {:label (:individualized-info base-education-2nd-module-texts)}}
   :id id
   :section-visibility-conditions
   [{:section-name harkinnanvaraisuus-wrapper-id
     :condition
     {:comparison-operator "="
      :data-type "str"
      :answer-compared-to "1"}}]
   :options
   [{:label (:no general-texts) :value "0"}
    {:label (:yes general-texts)
     :value "1"
     :followups
     [(merge (component/info-element metadata)
             {:text (:individualized-harkinnanvaraisuus base-education-2nd-module-texts)})]}]}))

(defn- ulkomailla-harkinnanvarainen-info
  [metadata]
  (merge (component/info-element metadata)
         {:text (:foreign-harkinnanvaraisuus base-education-2nd-module-texts)}))

(defn- kopio-tutkintotodistuksesta-attachment
  [metadata]
  (merge (component/attachment metadata)
  {:label (:copy-of-proof-of-certificate base-education-2nd-module-texts)
   :validators []
   :params {:deadline "29.3.2022 15:00"
            :mail-attachment? false
            :info-text {:enabled? true
                        :value (:copy-of-proof-of-certificate-info base-education-2nd-module-texts)}}}))

(defn- kopio-todistuksesta-attachment
  [metadata]
  (merge (component/attachment metadata)
         {:label (:copy-of-certificate base-education-2nd-module-texts)
          :params {:deadline "29.3.2022 15:00"
                   :info-text {:enabled? true
                               :value (:copy-of-certificate-info base-education-2nd-module-texts)}}}))

(defn- ei-paattotodistusta-info
  [metadata]
  (assoc (component/info-element metadata)
         :text (:no-graduation-info base-education-2nd-module-texts)))

(defn- perusopetus-option
  [metadata]
  {:label (:base-education base-education-2nd-module-texts)
   :value "1"
   :followups
   [(base-education-language-question metadata tutkintokieli-perusopetus-key)
    (suoritusvuosi-question metadata suoritusvuosi-perusopetus-key)]})

(defn- perusopetuksen-osittain-yksilollistetty-option
  [metadata]
  {:label (:base-education-partially-individualized base-education-2nd-module-texts)
   :value "2"
   :followups
   [(base-education-language-question metadata tutkintokieli-osittain-yks-key)
    (suoritusvuosi-question metadata suoritusvuosi-osittain-yks-key)
    (yksilollistetty-question metadata "matematiikka-ja-aidinkieli-yksilollistetty_1")]})

(defn- perusopetuksen-yksilollistetty-option
  [metadata]
  {:label (:base-education-individualized base-education-2nd-module-texts)
   :value "6"
   :followups
   [(base-education-language-question metadata tutkintokieli-yks-key)
    (suoritusvuosi-question metadata suoritusvuosi-yks-key)
    (yksilollistetty-question metadata "matematiikka-ja-aidinkieli-yksilollistetty_2")]})

(defn- opetus-jarjestetty-toiminta-alueittan-option
  [metadata]
  {:label (:base-education-organized-regionly base-education-2nd-module-texts)
   :value "3"
   :followups
   [(base-education-language-question metadata tutkintokieli-toiminta-alueittain)
    (suoritusvuosi-question metadata suoritusvuosi-toiminta-alueittain-key)]})

(defn- ulkomailla-suoritettu-option
  [metadata]
  {:label (:base-education-foreign base-education-2nd-module-texts)
   :value (:ulkomailla-suoritettu-value base-education-option-values-affecting-harkinnanvaraisuus)
   :followups
   [(ulkomailla-harkinnanvarainen-info metadata)
    (suorittanut-tutkinnon-question metadata)
    (kopio-tutkintotodistuksesta-attachment metadata)]})

(defn- ei-paattotodistusta-option
  [metadata]
  {:label (:base-education-no-graduation base-education-2nd-module-texts)
   :value (:ei-paattotodistusta-value base-education-option-values-affecting-harkinnanvaraisuus)
   :followups
   [(ei-paattotodistusta-info metadata)
    (suorittanut-tutkinnon-question metadata)
    (kopio-todistuksesta-attachment metadata)]})

(defn- base-education-question
  [metadata]
  (assoc (component/single-choice-button metadata)
    :id base-education-choice-key
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
        {:comparison-operator "="
         :data-type "str"
         :answer-compared-to (:ulkomailla-suoritettu-value base-education-option-values-affecting-harkinnanvaraisuus)}}
       {:section-name harkinnanvaraisuus-wrapper-id
        :condition
        {:comparison-operator "="
         :data-type "str"
         :answer-compared-to (:ei-paattotodistusta-value base-education-option-values-affecting-harkinnanvaraisuus)}}]
      :options [(perusopetus-option metadata)
                (perusopetuksen-osittain-yksilollistetty-option metadata)
                (perusopetuksen-yksilollistetty-option metadata)
                (opetus-jarjestetty-toiminta-alueittan-option metadata)
                (ulkomailla-suoritettu-option metadata)
                (ei-paattotodistusta-option metadata)]))

(defn base-education-2nd-module [metadata]
  (assoc (component/form-section metadata)
         :id base-education-wrapper-key
         :label (:section-title base-education-2nd-module-texts)
         :children [(base-education-question metadata)]))

