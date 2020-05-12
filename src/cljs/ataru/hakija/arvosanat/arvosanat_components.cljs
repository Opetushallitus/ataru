(ns ataru.hakija.arvosanat.arvosanat-components
  (:require [ataru.translations.translation-util :as translations]
            [re-frame.core :as re-frame]))

(defn oppiaineen-arvosana [{:keys [field-descriptor]}]
  (let [lang  @(re-frame/subscribe [:application/form-language])
        label (-> field-descriptor :label lang)]
    [:<>
     [:span.arvosana__oppiaine (str label)]
     [:span.arvosana__oppimaara (translations/get-hakija-translation :oppimäärä lang)]
     [:span.arvosana__arvosana (translations/get-hakija-translation :arvosana lang)]
     [:span.arvosana__lisaa-valinnaisaine "+"]]))

(defn arvosanat-taulukko [{:keys [field-descriptor
                                  render-field]}]
  (let [lang @(re-frame/subscribe [:application/form-language])]
    [:div.arvosanat-taulukko
     [:span.arvosana__oppiaine (translations/get-hakija-translation :oppiaine lang)]
     [:span.arvosana__lisaa-valinnaisaine (translations/get-hakija-translation :valinnaisaine lang)]
     (map (fn [arvosana-data]
            (let [arvosana-koodi (:id arvosana-data)
                  key            (str "arvosana-" arvosana-koodi)]
              ^{:key key}
              [render-field arvosana-data]))
          (:children field-descriptor))]))
