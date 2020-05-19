(ns ataru.hakija.arvosanat.arvosanat-components
  (:require [ataru.schema.lang-schema :as lang-schema]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [ataru.translations.translation-util :as translations]
            [re-frame.core :as re-frame]
            [schema.core :as s]))

(s/defn oppiaineen-arvosana
  [{:keys [field-descriptor
           render-field]} :- render-field-schema/RenderFieldArgs]
  (let [lang                   @(re-frame/subscribe [:application/form-language])
        label                  (-> field-descriptor :label lang)
        children               (:children field-descriptor)
        children-count         (count children)
        arvosana-dropdown      (case children-count
                                 2 (second children)
                                 1 (first children)
                                 nil)
        arvosana-dropdown-idx  (dec children-count)
        oppimaara-dropdown     (when (= children-count 2)
                                 (first children))
        oppimaara-dropdown-idx 0]
    [:div.arvosanat-taulukko__rivi
     [:div.arvosanat-taulukko__solu.arvosana__oppiaine
      [:span (str label)]]
     [:div.arvosanat-taulukko__solu.arvosana__oppimaara
      (when oppimaara-dropdown
        [render-field oppimaara-dropdown oppimaara-dropdown-idx])]
     [:div.arvosanat-taulukko__solu.arvosana__arvosana
      (when arvosana-dropdown
        [render-field arvosana-dropdown arvosana-dropdown-idx])]
     [:div.arvosanat-taulukko__solu.arvosana__lisaa-valinnaisaine.arvosana__lisaa-valinnaisaine--solu
      [:i.zmdi.zmdi-plus-circle-o.arvosana__lisaa-valinnaisaine-ikoni]
      [:a.a-linkki.arvosana__lisaa-valinnaisaine-linkki
       "Lisää valinnaisaine"]]]))

(s/defn arvosanat-taulukko-otsikkorivi
  [{:keys [lang]} :- {:lang lang-schema/Lang}]
  [:div.arvosanat-taulukko__rivi
   [:div.arvosanat-taulukko__solu.arvosanat-taulukko__otsikko.arvosana__oppiaine
    [:span (translations/get-hakija-translation :oppiaine lang)]]
   [:div.arvosanat-taulukko__solu.arvosanat-taulukko__otsikko.arvosana__lisaa-valinnaisaine
    [:span (translations/get-hakija-translation :valinnaisaine lang)]]])

(s/defn arvosanat-taulukko
  [{:keys [field-descriptor
           render-field
           idx]} :- render-field-schema/RenderFieldArgs]
  (let [lang @(re-frame/subscribe [:application/form-language])]
    [:div.arvosanat-taulukko
     [arvosanat-taulukko-otsikkorivi
      {:lang lang}]
     (map (fn field-descriptor->oppiaineen-arvosana [arvosana-data]
            (let [arvosana-koodi (:id arvosana-data)
                  key            (str "arvosana-" arvosana-koodi)]
              ^{:key key}
              [render-field arvosana-data idx]))
          (:children field-descriptor))]))
