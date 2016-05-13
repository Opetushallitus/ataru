(ns lomake-editori.soresu.component)

(defn text-field
  []
  {:fieldClass "formField"
   :label      {:fi "Tekstikenttä", :sv "Textfält"}
   :id         "applicant-surname"
   :required   true
   :fieldType  "textField"
   :helpText   {:fi "Aputeksti"
                :sv "Hjälptext"}})
