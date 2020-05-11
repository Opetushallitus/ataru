(ns ataru.hakija.arvosanat.arvosanat-components
  (:require [re-frame.core :as re-frame]))

(defn- arvosana [{:keys [label]}]
  [:<>
   [:span.arvosana__oppiaine (str label)]
   [:span.arvosana__oppimaara "Oppimäärä"]
   [:span.arvosana__arvosana "Arvosana"]
   [:span.arvosana__lisaa-valinnaisaine "+"]])

(defn arvosanat-taulukko [field-descriptor]
  (let [lang @(re-frame/subscribe [:application/form-language])]
    [:div.arvosanat-taulukko
     [:span.arvosana__oppiaine "Oppiaine"]
     [:span.arvosana__lisaa-valinnaisaine "Valinnaisaine"]
     (map (fn [arvosana-data]
            (let [label          (-> arvosana-data :label lang)
                  arvosana-koodi (:id arvosana-data)
                  key            (str "arvosana-" arvosana-koodi)]
              ^{:key key}
              [arvosana {:label label}]))
          (:children field-descriptor))]))
