(ns ataru.hakija.arvosanat.arvosanat-components
  (:require [ataru.translations.translation-util :as translations]
            [re-frame.core :as re-frame]))

(defn oppiaineen-arvosana [{:keys [field-descriptor
                                   render-field]}]
  (let [lang               @(re-frame/subscribe [:application/form-language])
        label              (-> field-descriptor :label lang)
        children           (:children field-descriptor)
        oppimaara-dropdown (when (-> children seq)
                             (first children))]
    [:<>
     [:div.arvosana-taulukko__grid-item.arvosana__oppiaine
      [:span (str label)]]
     (when oppimaara-dropdown
       [:div.arvosana-taulukko__grid-item.arvosana__oppimaara
        [render-field oppimaara-dropdown]])
     [:div.arvosana-taulukko__grid-item.arvosana__arvosana
      [:span (translations/get-hakija-translation :arvosana lang)]]
     [:div.arvosana-taulukko__grid-item.arvosana__lisaa-valinnaisaine
      [:span "+"]]]))

(defn arvosanat-taulukko [{:keys [field-descriptor
                                  render-field]}]
  (let [lang @(re-frame/subscribe [:application/form-language])]
    [:div.arvosanat-taulukko
     [:span.arvosana-taulukko__grid-item.arvosana__oppiaine (translations/get-hakija-translation :oppiaine lang)]
     [:span.arvosana-taulukko__grid-item.arvosana__lisaa-valinnaisaine (translations/get-hakija-translation :valinnaisaine lang)]
     (map (fn [arvosana-data]
            (let [arvosana-koodi (:id arvosana-data)
                  key            (str "arvosana-" arvosana-koodi)]
              ^{:key key}
              [render-field arvosana-data]))
          (:children field-descriptor))]))
