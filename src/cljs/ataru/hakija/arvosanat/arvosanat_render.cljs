(ns ataru.hakija.arvosanat.arvosanat-render
  (:require [ataru.hakija.arvosanat.arvosanat-components :as arvosanat]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [ataru.schema.lang-schema :as lang-schema]
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
          :fieldType  "fieldset"
          :id         "oppiaineen-arvosanat-valinnaiset-kielet"}
         [arvosanat/valinnaiset-kielet render-field-args]

         ;{:fieldClass "formField"
         ; :fieldType  "dropdown"
         ; :id         "oppiaine-valinnainen-kieli"}
         ;[arvosanat/valinnainen-kieli-dropdown render-field-args]

         {:fieldClass "questionGroup"
          :fieldType  "fieldset"}
         [arvosanat/oppiaineen-arvosana render-field-args]))

(s/defn render-arvosanat-component-readonly
  [{:keys [field-descriptor
           render-field
           application
           lang
           question-group-index]} :- {:field-descriptor     s/Any
                                      :application          s/Any
                                      :lang                 (s/maybe lang-schema/Lang)
                                      :render-field         s/Any
                                      :question-group-index (s/maybe s/Int)}]
  (match field-descriptor
         {:fieldClass "wrapperElement"
          :fieldType  "fieldset"}
         [arvosanat/arvosanat-taulukko-readonly
          {:lang             lang
           :application      application
           :render-field     render-field
           :field-descriptor field-descriptor
           :idx              question-group-index}]

         {:fieldClass "questionGroup"
          :fieldType  "fieldset"}
         [arvosanat/oppiaineen-arvosana-readonly
          {:field-descriptor field-descriptor
           :application      application
           :render-field     render-field
           :lang             lang
           :idx              question-group-index}]))
