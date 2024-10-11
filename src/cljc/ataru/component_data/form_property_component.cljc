(ns ataru.component-data.form-property-component
  (:require [ataru.util :as util]))

(defn property-multiple-choice [metadata]
  {:fieldClass            "formPropertyField"
   :fieldType             "multipleOptions"
   :id                    (util/component-id)
   :label                 {:fi "" :sv "" :en ""}
   :metadata              metadata
   :exclude-from-answers  true
   :params                {:hidden true}})

