(ns ataru.hakija.arvosanat.arvosanat-components
  (:require [ataru.schema.lang-schema :as lang-schema]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [ataru.translations.translation-util :as translations]
            [ataru.hakija.components.link-component :as link-component]
            [re-frame.core :as re-frame]
            [schema.core :as s]))

(s/defn oppiaineen-arvosana-rivi
  [{:keys [label
           oppimaara-dropdown
           arvosana-dropdown
           valinnaisaine-linkki]} :- {:label                                 s/Any
                                      (s/optional-key :oppimaara-dropdown)   s/Any
                                      (s/optional-key :arvosana-dropdown)    s/Any
                                      (s/optional-key :valinnaisaine-linkki) s/Any}]
  [:div.arvosanat-taulukko__rivi
   [:div.arvosanat-taulukko__solu.arvosana__oppiaine
    label]
   [:div.arvosanat-taulukko__solu.arvosana__oppimaara
    oppimaara-dropdown]
   [:div.arvosanat-taulukko__solu.arvosana__arvosana
    arvosana-dropdown]
   [:div.arvosanat-taulukko__solu.arvosana__lisaa-valinnaisaine.arvosana__lisaa-valinnaisaine--solu
    valinnaisaine-linkki]])

(s/defn oppiaineen-arvosana
  [{:keys [field-descriptor
           render-field]} :- render-field-schema/RenderFieldArgs]
  (let [lang               @(re-frame/subscribe [:application/form-language])
        row-count          @(re-frame/subscribe [:application/question-group-row-count (:id field-descriptor)])
        children           (:children field-descriptor)
        children-count     (count children)
        arvosana-dropdown  (case children-count
                             2 (second children)
                             1 (first children)
                             nil)
        oppimaara-dropdown (when (= children-count 2)
                             (first children))]
    [:<>
     (map (fn ->oppiaineen-arvosana-rivi [arvosana-idx]
            (let [key                 (str "oppiaineen-arvosana-rivi-" (:id field-descriptor) "-" arvosana-idx)
                  valinnaisaine-rivi? (> arvosana-idx 0)]
              ^{:key key}
              [oppiaineen-arvosana-rivi
               {:label                (let [label (cond->> (-> field-descriptor :label lang)
                                                           valinnaisaine-rivi?
                                                           (translations/get-hakija-translation :oppiaine-valinnainen lang))]
                                        [:span
                                         {:class (when valinnaisaine-rivi?
                                                   "oppiaineen-arvosana-rivi__oppiaine--valinnaisaine")}
                                         label])
                :oppimaara-dropdown   (when oppimaara-dropdown
                                        [render-field oppimaara-dropdown arvosana-idx])
                :arvosana-dropdown    (when arvosana-dropdown
                                        [render-field arvosana-dropdown arvosana-idx])
                :valinnaisaine-linkki (let [label (if valinnaisaine-rivi?
                                                    (translations/get-hakija-translation :poista lang)
                                                    (translations/get-hakija-translation :lisaa-valinnaisaine lang))]
                                        [link-component/link
                                         {:on-click (fn add-or-remove-oppiaineen-valinnaisaine-row []
                                                      (if valinnaisaine-rivi?
                                                        (re-frame/dispatch [:application/remove-question-group-row field-descriptor arvosana-idx])
                                                        (re-frame/dispatch [:application/add-question-group-row field-descriptor])))}
                                         (if valinnaisaine-rivi?
                                           [:span label]
                                           [:<>
                                            [:i.zmdi.zmdi-plus-circle-o.arvosana__lisaa-valinnaisaine-ikoni]
                                            [:span.arvosana__valinnaisaine-linkki
                                             label]])])}]))
          (range row-count))]))

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
