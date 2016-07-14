(ns ataru.virkailija.component-data.component
  (:require [ataru.util :as util]))

(defn text-field
  []
  {:fieldClass "formField"
   :fieldType  "textField"
   :label      {:fi "", :sv ""}
   :id         (util/component-id)
   :params     {}
   :required   false
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
   :required false
   :focus? true})
