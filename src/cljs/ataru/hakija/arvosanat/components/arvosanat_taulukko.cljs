(ns ataru.hakija.arvosanat.components.arvosanat-taulukko
  (:require [ataru.hakija.arvosanat.components.arvosanat-taulukko-otsikkorivi :as ato]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [re-frame.core :as re-frame]
            [schema.core :as s]))

(s/defn arvosanat-taulukko
        [{:keys [field-descriptor
                 render-field
                 idx]} :- render-field-schema/RenderFieldArgs]
        (let [lang @(re-frame/subscribe [:application/form-language])]
          [:div.arvosanat-taulukko
           [ato/arvosanat-taulukko-otsikkorivi
            {:lang      lang
             :readonly? false}]
           (map (fn field-descriptor->oppiaineen-arvosana [arvosana-data]
                  (let [arvosana-koodi (:id arvosana-data)
                        key            (str "arvosana-" arvosana-koodi)]
                    ^{:key key}
                    [render-field arvosana-data idx]))
                (:children field-descriptor))]))
