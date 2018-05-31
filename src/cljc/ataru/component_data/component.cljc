(ns ataru.component-data.component
  (:require [ataru.translations.texts :as texts]
            [ataru.util :as util]))

(defn text-field [metadata]
  {:fieldClass "formField"
   :fieldType  "textField"
   :label      {:fi "", :sv ""}
   :id         (util/component-id)
   :metadata   metadata
   :params     {}})

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

(defn dropdown-option []
  {:value ""
   :label {:fi "" :sv ""}})

(defn dropdown [metadata]
  {:fieldClass "formField"
   :fieldType  "dropdown"
   :id         (util/component-id)
   :label      {:fi "", :sv ""}
   :params     {}
   :metadata   metadata
   :options    [(dropdown-option)]})

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

(defn adjacent-fieldset [metadata]
  {:id         (util/component-id)
   :fieldClass "wrapperElement"
   :label      {:fi ""}
   :fieldType  "adjacentfieldset"
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
   :label                {:fi "Ilmoitus riittämättömästä pohjakoulutuksesta"
                          :sv "Ilmoitus riittämättömästä pohjakoulutuksesta"
                          :en "Ilmoitus riittämättömästä pohjakoulutuksesta"}
   :text                 {:fi "Ilmoittamasi pohjakoulutuksen perusteella et voi tulla valituksi seuraaviin hakukohteisiin"
                          :sv "Ilmoittamasi pohjakoulutuksen perusteella et voi tulla valituksi seuraaviin hakukohteisiin"
                          :en "Ilmoittamasi pohjakoulutuksen perusteella et voi tulla valituksi seuraaviin hakukohteisiin"}})

(defn koulutusmarkkinointilupa [metadata]
  (assoc (single-choice-button metadata)
         :id "koulutusmarkkinointilupa"
         :label {:fi "Yhteystietojani saa käyttää koulutusmarkkinoinnissa?"
                 :sv "Yhteystietojani saa käyttää koulutusmarkkinoinnissa?"
                 :en "Yhteystietojani saa käyttää koulutusmarkkinoinnissa?"}
         :validators ["required"]
         :options [{:value "Kyllä"
                    :label (:yes texts/general-texts)}
                   {:value "Ei"
                    :label (:no texts/general-texts)}]))

(defn valintatuloksen-julkaisulupa [metadata]
  (assoc (single-choice-button metadata)
         :id "valintatuloksen-julkaisulupa"
         :label {:fi "Opiskelijavalinnan tulokseni saa julkaista internetissä?"
                 :sv "Opiskelijavalinnan tulokseni saa julkaista internetissä?"
                 :en "Opiskelijavalinnan tulokseni saa julkaista internetissä?"}
         :validators ["required"]
         :options [{:value "Kyllä"
                    :label (:yes texts/general-texts)}
                   {:value "Ei"
                    :label (:no texts/general-texts)}]))
