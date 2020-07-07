(ns ataru.hakija.arvosanat.components.arvosanat-taulukko-otsikkorivi
  (:require [ataru.schema.lang-schema :as lang-schema]
            [ataru.translations.translation-util :as translations]
            [schema.core :as s]))

(s/defn arvosanat-taulukko-otsikkorivi
        [{:keys [lang
                 readonly?]} :- {:lang      lang-schema/Lang
                                 :readonly? s/Bool}]
        [:div.arvosanat-taulukko__rivi
         {:class (when readonly?
                   "arvosanat-taulukko__rivi--readonly")}
         [:div.arvosanat-taulukko__solu.arvosanat-taulukko__otsikko.arvosana__oppiaine
          [:span (translations/get-hakija-translation :oppiaine lang)]]
         (when readonly?
           [:<>
            [:div.arvosanat-taulukko__solu.arvosanat-taulukko__otsikko.arvosana__oppimaara
             [:span (translations/get-hakija-translation :oppimaara lang)]]
            [:div.arvosanat-taulukko__solu.arvosanat-taulukko__otsikko.arvosana__arvosana
             [:span (translations/get-hakija-translation :arvosana lang)]]])
         (when-not readonly?
           [:div.arvosanat-taulukko__solu.arvosanat-taulukko__otsikko.arvosana__lisaa-valinnaisaine
            [:span (translations/get-hakija-translation :valinnaisaine lang)]])])
