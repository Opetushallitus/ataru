(ns ataru.soresu.component-spec
  (:require [ataru.fixtures.form :as fixtures]
            [ataru.schema.clj-schema :as ataru-schema]
            [ataru.virkailija.soresu.component :as component]
            [schema.core :as s]
            [speclj.core :refer :all]
            [oph.soresu.form.schema :as soresu]))

(describe "text-field"
  (tags :unit)

  (it "should be validated with soresu-form schema"
      (should-be-nil (s/check soresu/FormField (component/text-field)))))

(describe "form-section"
  (tags :unit)

  (it "should be validated with ataru-form schema"
    (should-be-nil (s/check soresu/WrapperElement (component/form-section)))))

(describe "fixture"
  (tags :unit)

  (it "must validate"
      (should-be-nil
        (s/check ataru-schema/FormWithContent fixtures/form-with-content))))

(run-specs)
