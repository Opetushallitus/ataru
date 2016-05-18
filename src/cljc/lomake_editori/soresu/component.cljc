(ns lomake-editori.soresu.component)

(defn text-field
  []
  {:fieldClass "formField"
   :label      {:fi "", :sv ""}
   :id         (str (gensym))
   :required   true
   :fieldType  "textField"
   :helpText   {:fi "Aputeksti"
                :sv "Hjälptext"}})

(defn form-section
  []
  {:fieldClass "wrapperElement"
   :fieldType  "fieldset"
   :id         (str (gensym))
   :label      {:fi "Osion nimi" :sv "Avsnitt namn"}
   :children   []})
