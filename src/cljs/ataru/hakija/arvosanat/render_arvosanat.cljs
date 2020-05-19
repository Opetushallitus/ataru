(ns ataru.hakija.arvosanat.render-arvosanat
  (:require [ataru.hakija.arvosanat.arvosanat-components :as arvosanat]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [schema.core :as s])
  (:require-macros [cljs.core.match :refer [match]]))

(s/defn render-arvosanat-component
  [{:keys [field-descriptor]
    :as   render-field-args} :- render-field-schema/RenderFieldArgs]
  (match field-descriptor
         {:fieldClass "wrapperElement"
          :fieldType  "fieldset"}
         [arvosanat/arvosanat-taulukko render-field-args]

         {:fieldClass "questionGroup"
          :fieldType  "fieldset"}
         [arvosanat/oppiaineen-arvosana render-field-args]))
