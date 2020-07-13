(ns ataru.hakija.arvosanat.components.oppiaineen-arvosana-rivi
  (:require [schema.core :as s]))

(s/defn oppiaineen-arvosana-rivi
        [{:keys [label
                 oppimaara-column
                 arvosana-column
                 valinnaisaine-column
                 data-test-id
                 pakollinen-oppiaine?
                 readonly?]} :- {:label                                 s/Any
                                 (s/optional-key :oppimaara-column)     s/Any
                                 (s/optional-key :arvosana-column)      s/Any
                                 (s/optional-key :valinnaisaine-column) s/Any
                                 :pakollinen-oppiaine?                  s/Bool
                                 (s/optional-key :data-test-id)         s/Str
                                 (s/optional-key :readonly?)            s/Bool}]
        [:div.arvosanat-taulukko__rivi
         {:data-test-id data-test-id
          :class        (cond-> ""

                                readonly?
                                (str " arvosanat-taulukko__rivi--readonly")

                                pakollinen-oppiaine?
                                (str " arvosanat-taulukko__rivi--pakollinen-oppiaine"))}
         [:div.arvosanat-taulukko__solu.arvosana__oppiaine
          {:class (when-not oppimaara-column
                    "arvosanat-taulukko__solu--span-2")}
          label]
         (when oppimaara-column
           [:div.arvosanat-taulukko__solu.arvosana__oppimaara
            oppimaara-column])
         (when arvosana-column
           [:div.arvosanat-taulukko__solu.arvosana__arvosana
            arvosana-column])
         (when valinnaisaine-column
           [:div.arvosanat-taulukko__solu.arvosana__lisaa-valinnaisaine.arvosana__lisaa-valinnaisaine--solu
            valinnaisaine-column])])
