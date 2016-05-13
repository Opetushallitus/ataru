(ns lomake-editori.soresu.component-spec
  (:require [oph.soresu.form.schema :as soresu]
            [lomake-editori.soresu.component :as component]
            [schema.core :as s]
            [speclj.core :refer :all]))

(soresu/create-form-schema [] [] [])

(describe "text-field"
  (it "should be validated with soresu-form schema"
    (should (s/validate soresu/FormField (component/text-field)))))

(run-specs)
