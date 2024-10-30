(ns ataru.component-data.component
  (:require [ataru.translations.texts :as texts]
            [ataru.util :as util]))

(def harkinnanvaraisuus-wrapper-id "harkinnanvaraisuus-wrapper")

(defn text-field [metadata]
  {:fieldClass "formField"
   :fieldType  "textField"
   :label      {:fi "", :sv ""}
   :id         (util/component-id)
   :metadata   metadata
   :params     {}})

(defn text-field-option
  ([] (text-field-option nil))
  ([value]
   {:value value
    :label {:fi "" :sv ""}}))

(defn text-field-conditional-option
  ([] (text-field-conditional-option nil))
  ([value]
   {:value     value
    :label     {:fi "" :sv ""}
    :condition {:comparison-operator "<"}}))

(defn text-area [metadata]
  (assoc (text-field metadata)
         :fieldType "textArea"))

(defn form-section [metadata]
  {:fieldClass "wrapperElement"
   :fieldType  "fieldset"
   :id         (util/component-id)
   :label      {:fi "" :sv ""}
   :children   []
   :metadata   metadata
   :params     {}})

(defn question-group [metadata]
  {:fieldClass "questionGroup"
   :fieldType  "fieldset"
   :id         (util/component-id)
   :label      {:fi "" :sv ""}
   :children   []
   :metadata   metadata
   :params     {}})

(defn question-group-tutkinto [metadata]
  {:fieldClass "questionGroup"
   :fieldType  "tutkintofieldset"
   :id         (util/component-id)
   :label      {:fi "" :sv "" :en ""}
   :children   []
   :metadata   metadata
   :params     {}})

(defn externalDataElement [metadata]
  {:fieldClass "externalDataElement"
   :fieldType  "selectabletutkintolist"
   :id         (util/component-id)
   :label      {:fi "" :sv "" :en ""}
   :children   []
   :metadata   metadata
   :params     {}})

(defn dropdown-option
  ([] (dropdown-option nil))
  ([value]
   {:value value
    :label {:fi "" :sv ""}}))

(defn dropdown [metadata]
  {:fieldClass "formField"
   :fieldType  "dropdown"
   :id         (util/component-id)
   :label      {:fi "", :sv ""}
   :params     {}
   :metadata   metadata
   :options    [(dropdown-option "0")
                (dropdown-option "1")]})

(defn multiple-choice [metadata]
  {:fieldClass "formField"
   :fieldType  "multipleChoice"
   :id         (util/component-id)
   :label      {:fi "" :sv ""}
   :params     {}
   :metadata   metadata
   :options    []})

(defn row-section
  "Creates a data structure that represents a row that has multiple form
   components in it.

   This component currently doesn't have any render implementation
   in the editor side. If used WITHOUT a :module keyword associated to it,
   the editor UI will fail at ataru.virkailija.editor.core/soresu->reagent.

   Please see ataru.component-data.person-info-module for example."
  [child-components metadata]
  {:fieldClass "wrapperElement"
   :fieldType  "rowcontainer"
   :id         (util/component-id)
   :children   child-components
   :metadata   metadata
   :params     {}})

(defn info-element [metadata]
  {:fieldClass "infoElement"
   :fieldType  "p"
   :id         (util/component-id)
   :params     {}
   :metadata   metadata
   :label      {:fi ""}   ; LocalizedString
   :text       {:fi ""}}) ; LocalizedString

(defn modal-info-element [metadata]
  {:fieldClass "modalInfoElement"
   :fieldType  "p"
   :id         (util/component-id)
   :params     {}
   :metadata   metadata
   :label      {:fi ""}   ; LocalizedString
   :text       {:fi ""}}) ; LocalizedString

(defn adjacent-fieldset [metadata]
  {:id         (util/component-id)
   :fieldClass "wrapperElement"
   :label      {:fi ""}
   :fieldType  "adjacentfieldset"
   :params     {}
   :metadata   metadata
   :children   []})

(defn single-choice-button [metadata]
  {:fieldClass "formField"
   :fieldType  "singleChoice"
   :id         (util/component-id)
   :label      {:fi "" :sv ""}
   :params     {}
   :metadata   metadata
   :options    []})

(defn attachment [metadata]
  {:fieldClass "formField"
   :fieldType  "attachment"
   :id         (util/component-id)
   :label      {:fi "" :sv ""}
   :params     {}
   :metadata   metadata
   :options    []})

; NB: when altering this, take into account that the hakukohteet component is
;     dynamically injected to legacy forms without one already present:
(defn hakukohteet []
  (let [metadata {:created-by  {:name "system"
                                :oid  "system"
                                :date "1970-01-01T00:00:00Z"}
                  :modified-by {:name "system"
                                :oid  "system"
                                :date "1970-01-01T00:00:00Z"}}]
    {:fieldClass                     "formField"
     :fieldType                      "hakukohteet"
     :id                             "hakukohteet"
     :label                          {:fi "Hakukohteet"
                                      :sv "Ansökningsmål"
                                      :en "Application options"}
     :params                         {}
     :options                        []
     :metadata                       metadata
     :validators                     ["hakukohteet"]
     :exclude-from-answers-if-hidden true}))

(defn pohjakoulutusristiriita [metadata]
  {:id                   "pohjakoulutusristiriita"
   :fieldClass           "pohjakoulutusristiriita"
   :fieldType            "pohjakoulutusristiriita"
   :exclude-from-answers true
   :params               {:deny-submit false}
   :rules                {:pohjakoulutusristiriita nil}
   :metadata             metadata
   :label                (:insufficient-base-education texts/translation-mapping)
   :text                 (:not-applicable-for-hakukohteet texts/translation-mapping)})

(defn koulutusmarkkinointilupa [metadata]
  (assoc (single-choice-button metadata)
         :id "koulutusmarkkinointilupa"
         :label (:allow-use-of-contact-information texts/translation-mapping)
         :params {:info-text {:label (:allow-use-of-contact-information-info texts/translation-mapping)}}
         :validators ["required"]
         :options [{:value "Kyllä"
                    :label (:yes texts/general-texts)}
                   {:value "Ei"
                    :label (:no texts/general-texts)}]))

(defn valintatuloksen-julkaisulupa [metadata]
  (assoc (single-choice-button metadata)
         :id "valintatuloksen-julkaisulupa"
         :label (:allow-publishing-of-results-online texts/translation-mapping)
         :validators ["required"]
         :options [{:value "Kyllä"
                    :label (:yes texts/general-texts)}
                   {:value "Ei"
                    :label (:no texts/general-texts)}]))

(defn paatos-opiskelijavalinnasta-sahkopostiin [metadata]
  (assoc (single-choice-button metadata)
    :id "paatos-opiskelijavalinnasta-sahkopostiin"
    :label (:paatos-opiskelijavalinnasta-sahkopostiin texts/translation-mapping)
    :validators ["required"]
    :options [{:value "Kyllä"
               :label (:yes texts/general-texts)}
              {:value "Ei"
               :label (:no texts/general-texts)}]))

(defn lupa-sahkoiseen-asiointiin [metadata]
  (assoc (single-choice-button metadata)
         :id "sahkoisen-asioinnin-lupa"
         :label (:permission-for-electronic-transactions texts/translation-mapping)
         :params {:info-text {:label (:permission-for-electronic-transactions-info texts/translation-mapping)}}
         :validators ["required-hakija"]
         :options [{:value "Kyllä"
                    :label (:permission-for-electronic-transactions-kylla texts/translation-mapping)}]))

(defn asiointikieli [metadata]
  (assoc (dissoc (dropdown metadata) :options)
         :id "asiointikieli"
         :label (:contact-language texts/translation-mapping)
         :params {:info-text {:label (:contact-language-info texts/translation-mapping)}}
         :validators ["required"]
         :options [{:value "1"
                    :label (:finnish texts/translation-mapping)}
                   {:value "2"
                    :label (:swedish texts/translation-mapping)}
                   {:value "3"
                    :label (:english texts/translation-mapping)}]))

(defn lupatiedot-kk [metadata]
  (assoc (form-section metadata)
         :id "lupatiedot-kk"
         :label (:lupatiedot-kk texts/translation-mapping)
         :children [(assoc (info-element metadata)
                           :text (:lupatiedot-kk-info texts/translation-mapping))
                    (lupa-sahkoiseen-asiointiin metadata)
                    (koulutusmarkkinointilupa metadata)
                    (asiointikieli metadata)]))

(defn lupatiedot-toinen-aste [metadata]
  (assoc (form-section metadata)
    :id "lupatiedot-toinen-aste"
    :label (:lupatiedot-toinen-aste texts/translation-mapping)
    :children [(assoc (info-element metadata)
                 :text (:lupatiedot-toinen-aste-info texts/translation-mapping))
               (paatos-opiskelijavalinnasta-sahkopostiin metadata)
               (koulutusmarkkinointilupa metadata)
               (valintatuloksen-julkaisulupa metadata)
               (asiointikieli metadata)]))

(defn huoltajan-etunimi [metadata idx]
  (assoc (text-field metadata)
    :id (str "guardian-firstname" idx)
    :label (:guardian-firstname texts/translation-mapping)
    :validators []))

(defn huoltajan-sukunimi [metadata idx]
  (assoc (text-field metadata)
    :id (str "guardian-lastname" idx)
    :label (:guardian-lastname texts/translation-mapping)
    :validators []))

(defn huoltajan-puhelin [metadata suffix]
  (assoc (text-field metadata)
    :id (str "guardian-phone" suffix)
    :label (:guardian-phone texts/translation-mapping)
    :validators []))

(defn huoltajan-email [metadata suffix]
  (assoc (text-field metadata)
    :id (str "guardian-email" suffix)
    :label (:guardian-email texts/translation-mapping)
    :validators [:email-simple]))

(defn huoltajan-nimet-rivi [metadata secondary]
  (let [suffix                      (when secondary "-secondary")
        metadata-with-added-fields  (assoc (adjacent-fieldset metadata)
                                      :children [(huoltajan-etunimi metadata suffix)
                                                 (huoltajan-sukunimi metadata suffix)])]
    (if secondary
      (assoc metadata-with-added-fields
        :label (get texts/translation-mapping :guardian-contact-minor-secondary))
      metadata-with-added-fields)))

(defn huoltajan-yhteystiedot-rivi [metadata secondary]
  (let [suffix (when secondary "-secondary")]
    (assoc (adjacent-fieldset metadata)
      :children [(huoltajan-puhelin metadata suffix)
                 (huoltajan-email metadata suffix)])))

(defn huoltajan-yhteystiedot [metadata]
  (assoc (form-section metadata)
         :id "guardian-contact-information"
         :label (:guardian-contact-information texts/translation-mapping)
         :children [(huoltajan-nimet-rivi metadata false)
                    (huoltajan-yhteystiedot-rivi metadata false)
                    (huoltajan-nimet-rivi metadata true)
                    (huoltajan-yhteystiedot-rivi metadata true)]))

(def lupatiedot-kk-questions
  (->> (lupatiedot-kk {})
       :children
       util/flatten-form-fields
       (map (comp name :id))
       set))

(defn- harkinnanvaraisuus-info [metadata]
  (assoc (info-element metadata)
    :text (:harkinnanvaraisuus-info texts/translation-mapping)))

(defn harkinnanvaraisuus-question [metadata]
  (assoc (single-choice-button metadata)
    :id                "harkinnanvaraisuus"
    :label             (:harkinnanvaraisuus-question texts/translation-mapping)
    :validators        ["required"]
    :sensitive-answer  true
    :options [{:value  "1"
               :label  (:yes texts/general-texts)
               :followups [(assoc (single-choice-button metadata)
                             :id                "harkinnanvaraisuus-reason"
                             :label             (:harkinnanvaraisuus-reason texts/translation-mapping)
                             :validators        ["required"]
                             :sensitive-answer  true
                             :options [{:label  (:harkinnanvaraisuus-reason-0 texts/translation-mapping)
                                        :value  "0"}
                                       {:label  (:harkinnanvaraisuus-reason-1 texts/translation-mapping)
                                        :value  "1"}
                                       {:label  (:harkinnanvaraisuus-reason-2 texts/translation-mapping)
                                        :value  "2"}
                                       {:label  (:harkinnanvaraisuus-reason-3 texts/translation-mapping)
                                        :value  "3"}])]}
              {:value "0"
               :label (:no texts/general-texts)}]))

(defn harkinnanvaraisuus [metadata]
  (assoc (form-section metadata)
    :id       harkinnanvaraisuus-wrapper-id
    :label    (:harkinnanvaraisuus-topic texts/translation-mapping)
    :children [(harkinnanvaraisuus-info metadata)
               (harkinnanvaraisuus-question metadata)]))
