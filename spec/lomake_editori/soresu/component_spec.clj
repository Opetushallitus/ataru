(ns lomake-editori.soresu.component-spec
  (:require [oph.soresu.form.schema :as soresu]
            [lomake-editori.soresu.component :as component]
            [schema.core :as s]
            [speclj.core :refer :all]))

(soresu/create-form-schema [] [] [])

(s/defschema TemporaryWrapperElementSchema
  {:fieldClass                (s/eq "wrapperElement")
   :id                        s/Str
   :fieldType                 (s/eq "fieldset")
   :children                  [s/Any]
   (s/optional-key :params)   s/Any
   (s/optional-key :label)    soresu/LocalizedString
   (s/optional-key :helpText) soresu/LocalizedString})

(describe "text-field"
  (it "should be validated with soresu-form schema"
    (should (s/validate soresu/FormField (component/text-field)))))

(describe "form-section"
  (it "should be validated with soresu-form schema"
    (should (s/validate TemporaryWrapperElementSchema (component/form-section)))))

(run-specs)
