(ns ataru.hakija.arvosanat.components.arvosanat-taulukko-readonly
  (:require [ataru.hakija.arvosanat.components.arvosanat-taulukko-otsikkorivi :as ato]
            [ataru.schema.lang-schema :as lang-schema]
            [clojure.string :as string]
            [re-frame.core :as re-frame]
            [schema.core :as s]))

(s/defn arvosanat-taulukko-readonly
        [{:keys [field-descriptor
                 render-field
                 lang
                 application
                 idx]} :- {:field-descriptor      s/Any
                                    :render-field s/Any
                                    :lang         lang-schema/Lang
                                    :application  s/Any
                                    :idx          (s/maybe s/Int)}]
        [:div.arvosanat-taulukko
         [ato/arvosanat-taulukko-otsikkorivi
          {:lang      lang
           :readonly? true}]
         (->> (:children field-descriptor)
              (filter (fn [arvosana-data]
                        (let [arvosana-koodi (:id arvosana-data)
                              value          @(re-frame/subscribe [:application/answer
                                                                   (keyword (str "arvosana-" arvosana-koodi))
                                                                   0])]
                          (or (= arvosana-koodi "oppiaineen-arvosanat-valinnaiset-kielet")
                              (-> value :value string/blank? not)))))
              (map (fn field-descriptor->oppiaineen-arvosana-readonly [arvosana-data]
                     (let [arvosana-koodi (:id arvosana-data)
                           key            (str "arvosana-" arvosana-koodi)]
                       ^{:key key}
                       [render-field arvosana-data application lang idx])))
              doall)])
