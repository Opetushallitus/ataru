(ns ataru.fixtures.form)

(def form-with-content
  {:name        "Test fixture!"
   :modified-by "DEVELOPER"
   :content
   [{:fieldClass "formField"
     :label      {:fi "tekstiä" :sv ""}
     :id         "G__19"
     :required   false
     :fieldType  "textField"}
    {:fieldClass "wrapperElement"
     :fieldType  "fieldset"
     :id         "G__31"
     :label      {:fi "Osion nimi" :sv "Avsnitt namn"}
     :children
     [{:fieldClass "formField"
       :label      {:fi "" :sv ""}
       :id         "G__32"
       :required   false
       :fieldType  "textField"}]}]})
