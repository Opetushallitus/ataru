(ns ataru.hakija.arvosanat.components.oppiaineen-arvosana
  (:require [ataru.hakija.arvosanat.components.oppiaineen-arvosana-rivi :as oar]
            [ataru.hakija.arvosanat.components.lisaa-valinnaisaine-linkki :as lvl]
            [ataru.translations.translation-util :as translations]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [re-frame.core :as re-frame]
            [schema.core :as s]))

(s/defn oppiaineen-arvosana
        [{:keys [field-descriptor
                 render-field]} :- render-field-schema/RenderFieldArgs]
        (let [lang               @(re-frame/subscribe [:application/form-language])
              row-count          @(re-frame/subscribe [:application/question-group-row-count (:id field-descriptor)])
              children           (:children field-descriptor)
              arvosana-dropdown  (last children)
              oppimaara-dropdown (when (= (count children) 2)
                                   (first children))
              data-test-id       (str "oppiaineen-arvosana-" (:id field-descriptor))]
          (->> (range row-count)
               (mapv (fn ->oppiaineen-arvosana-rivi [arvosana-idx]
                       (let [key                 (str "oppiaineen-arvosana-rivi-" (:id field-descriptor) "-" arvosana-idx)
                             valinnaisaine-rivi? (> arvosana-idx 0)]
                         ^{:key key}
                         [oar/oppiaineen-arvosana-rivi
                          {:data-test-id
                           data-test-id
                           :pakollinen-oppiaine?
                           (not valinnaisaine-rivi?)
                           :label
                           (let [label (cond->> (-> field-descriptor :label lang)
                                                valinnaisaine-rivi?
                                                (translations/get-hakija-translation :oppiaine-valinnainen lang))]
                             [:span
                              {:class (when valinnaisaine-rivi?
                                        "oppiaineen-arvosana-rivi__oppiaine--valinnaisaine")}
                              label])

                           :oppimaara-column
                           (when oppimaara-dropdown
                             [render-field
                              (assoc oppimaara-dropdown :data-test-id (str data-test-id "-oppimaara-" arvosana-idx))
                              arvosana-idx])

                           :arvosana-column
                           (when arvosana-dropdown
                             [render-field
                              (assoc arvosana-dropdown :data-test-id (str data-test-id "-arvosana-" arvosana-idx))
                              arvosana-idx])

                           :valinnaisaine-column
                           [lvl/lisaa-valinnaisaine-linkki
                            {:valinnaisaine-rivi? valinnaisaine-rivi?
                             :arvosana-column     arvosana-dropdown
                             :oppimaara-column    oppimaara-dropdown
                             :lang                lang
                             :arvosana-idx        arvosana-idx
                             :field-descriptor    field-descriptor
                             :row-count           row-count
                             :data-test-id        (str data-test-id "-lisaa-valinnaisaine-linkki-" arvosana-idx "-" (if valinnaisaine-rivi? "poista" "lisaa"))}]}])))
               (into [:<>]))))
