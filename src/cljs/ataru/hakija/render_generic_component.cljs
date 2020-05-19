(ns ataru.hakija.render-generic-component
  (:require [ataru.hakija.components.dropdown-component :as dropdown-component]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [schema.core :as s])
  (:require-macros [cljs.core.match :refer [match]]))

(s/defn render-generic-component
  [{:keys [field-descriptor]
    :as   render-field-args} :- render-field-schema/RenderFieldArgs]
  (match field-descriptor
         {:fieldClass "formField"
          :fieldType  "dropdown"}
         [dropdown-component/hakija-dropdown render-field-args]))
