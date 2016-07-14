(ns ataru.virkailija.soresu.component
  #?(:cljs (:require [ataru.cljs-util :as util])
     :clj  (:import (java.util UUID))))

(defn text-field
  []
  {:fieldClass "formField"
   :fieldType  "textField"
   :label      {:fi "", :sv ""}
   :id         #?(:cljs (util/new-uuid)
                  :clj  (str (UUID/randomUUID)))
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
   :id         #?(:cljs (util/new-uuid)
                  :clj  (str (UUID/randomUUID)))
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
   :id #?(:cljs (util/new-uuid)
          :clj  (str (UUID/randomUUID)))
   :label {:fi "", :sv ""}
   :params {}
   :options [(dropdown-option)]
   :required false
   :focus? true})
