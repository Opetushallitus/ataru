(ns ataru.soresu.component-spec
  (:require [oph.soresu.form.schema :as soresu]
            [ataru.schema.clj-schema]
            [ataru.virkailija.soresu.component :as component]
            [schema.core :as s]
            [speclj.core :refer :all]))

(describe "text-field"
  (it "should be validated with soresu-form schema"
      (should-be-nil (s/check soresu/FormField (component/text-field)))))

(describe "form-section"
  (it "should be validated with ataru-form schema"
    (should-be-nil (s/check soresu/WrapperElement (component/form-section)))))

(run-specs)
