(ns ataru.component-data.form-property-component
  (:require [ataru.util :as util]))

(defn property-multiple-choice [metadata]
  {:fieldClass            "formPropertyField"
   :fieldType             "multipleChoice"
   :id                    (util/component-id)
   :label                 {:fi "" :sv ""}
   :metadata              metadata
   :exclude-from-answers  true})

