(ns ataru.virkailija.component-data.component
  (:require [ataru.util :as util]))

(defn text-field
  []
  {:fieldClass "formField"
   :fieldType  "textField"
   :label      {:fi "", :sv ""}
   :id         (util/component-id)
   :params     {}
   :focus?     true})

(defn text-area []
  (assoc (text-field)
         :fieldType "textArea"))

(defn form-section
  []
  {:fieldClass "wrapperElement"
   :fieldType  "fieldset"
   :id         (util/component-id)
   :label      {:fi "Osion nimi" :sv "Avsnitt namn"}
   :children   []
   :params     {}
   :focus?     true})

(defn dropdown-option
  []
  {:value ""
   :label {:fi "" :sv ""}
   :focus? true})

(defn dropdown
  []
  {:fieldClass "formField"
   :fieldType "dropdown"
   :id (util/component-id)
   :label {:fi "", :sv ""}
   :params {}
   :options [(dropdown-option)]
   :focus? true})

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
