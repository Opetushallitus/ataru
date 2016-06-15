(ns ataru.virkailija.soresu.component)

(defn text-field
  []
  {:fieldClass "formField"
   :fieldType  "textField"
   :label      {:fi "", :sv ""}
   :id         (str (gensym))
   :params     {}
   :required   false})

(defn text-area []
  (assoc (text-field)
         :fieldType "textArea"))

(defn form-section
  []
  {:fieldClass "wrapperElement"
   :fieldType  "fieldset"
   :id         (str (gensym))
   :label      {:fi "Osion nimi" :sv "Avsnitt namn"}
   :children   []
   :params     {}})

(defn dropdown-option
  []
  {:value "" :label {:fi "" :sv ""}})

(defn dropdown
  []
  {:fieldClass "formField"
   :fieldType "dropdown"
   :id (str (gensym))
   :label {:fi "", :sv ""}
   :params {}
   :options [(dropdown-option)]
   :required false})
