(ns ataru.virkailija.soresu.component
  (:require [ataru.cljs-util :as util]))

(defn text-field
  []
  {:fieldClass "formField"
   :fieldType  "textField"
   :label      {:fi "", :sv ""}
   :id         (util/new-uuid)
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
   :id         (util/new-uuid)
   :label      {:fi "Osion nimi" :sv "Avsnitt namn"}
   :children   []
   :params     {}
   :focus?     true})

(defn dropdown-option
  []
  {:value "" :label {:fi "" :sv ""}})

(defn dropdown
  []
  {:fieldClass "formField"
   :fieldType "dropdown"
   :id (util/new-uuid)
   :label {:fi "", :sv ""}
   :params {}
   :options [(dropdown-option)]
   :required false
   :focus? true})
