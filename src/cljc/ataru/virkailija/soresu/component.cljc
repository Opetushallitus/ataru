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
