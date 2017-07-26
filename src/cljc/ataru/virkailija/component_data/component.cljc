(ns ataru.virkailija.component-data.component
  (:require [ataru.util :as util]))

(defn text-field
  []
  {:fieldClass "formField"
   :fieldType  "textField"
   :label      {:fi "", :sv ""}
   :id         (util/component-id)
   :params     {}})

(defn text-area []
  (assoc (text-field)
         :fieldType "textArea"))

(defn form-section
  []
  {:fieldClass "wrapperElement"
   :fieldType  "fieldset"
   :id         (util/component-id)
   :label      {:fi "" :sv ""}
   :children   []
   :params     {}})

(defn dropdown-option
  []
  {:value ""
   :label {:fi "" :sv ""}})

(defn dropdown
  []
  {:fieldClass "formField"
   :fieldType  "dropdown"
   :id         (util/component-id)
   :label      {:fi "", :sv ""}
   :params     {}
   :options    [(dropdown-option)]})

(defn multiple-choice
  []
  {:fieldClass "formField"
   :fieldType  "multipleChoice"
   :id         (util/component-id)
   :label      {:fi "" :sv ""}
   :params     {}
   :options    []})

(defn row-section
  "Creates a data structure that represents a row that has multiple form
   components in it.

   This component currently doesn't have any render implementation
   in the editor side. If used WITHOUT a :module keyword associated to it,
   the editor UI will fail at ataru.virkailija.editor.core/soresu->reagent.

   Please see ataru.virkailija.component-data.person-info-module for example."
  [child-components]
  {:fieldClass "wrapperElement"
   :fieldType  "rowcontainer"
   :id         (util/component-id)
   :children   child-components
   :params     {}})

(defn info-element []
  {:fieldClass "infoElement"
   :fieldType  "p"
   :id         (util/component-id)
   :params     {}
   :label      {:fi ""} ; LocalizedString
   :text       {:fi ""} ; LocalizedString
   })

(defn adjacent-fieldset []
  {:id         (util/component-id)
   :fieldClass "wrapperElement"
   :label      {:fi ""}
   :fieldType  "adjacentfieldset"
   :children   []})

(defn single-choice-button []
  {:fieldClass "formField"
   :fieldType  "singleChoice"
   :id         (util/component-id)
   :label      {:fi "" :sv ""}
   :params     {}
   :options    []})

(defn attachment []
  {:fieldClass "formField"
   :fieldType  "attachment"
   :id         (util/component-id)
   :label      {:fi "" :sv ""}
   :params     {}
   :options    []})

(defn hakukohteet []
  {:fieldClass "formField"
   :fieldType "hakukohteet"
   :id :hakukohteet
   :label {:fi "Hakukohteet"
           :sv ""
           :en ""}
   :params {}
   :options []
   :exclude-from-answers-if-hidden true})
