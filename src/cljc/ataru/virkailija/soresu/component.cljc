(ns ataru.virkailija.soresu.component)

(defn text-field
  []
  {:fieldClass "formField"
   :label      {:fi "", :sv ""}
   :id         (str (gensym))
   :required   false
   :fieldType  "textField"})

(defn form-section
  []
  {:fieldClass "wrapperElement"
   :fieldType  "fieldset"
   :id         (str (gensym))
   :label      {:fi "Osion nimi" :sv "Avsnitt namn"}
   :children   []})
