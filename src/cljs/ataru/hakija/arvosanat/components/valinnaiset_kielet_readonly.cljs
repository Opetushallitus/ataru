(ns ataru.hakija.arvosanat.components.valinnaiset-kielet-readonly
  (:require [ataru.hakija.arvosanat.components.oppiaineen-arvosana-rivi :as oar]
            [ataru.hakija.arvosanat.components.valinnainen-kieli-label :as vkl]
            [ataru.hakija.arvosanat.components.valinnainen-kieli-oppimaara :as vko]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [ataru.hakija.arvosanat.valinnainen-oppiaine-koodi :as vok]
            [ataru.schema.lang-schema :as lang-schema]
            [re-frame.core :as re-frame]
            [schema.core :as s]
            [schema-tools.core :as st]))

(s/defn valinnaiset-kielet-readonly
        [{:keys [field-descriptor
                 render-field
                 application
                 lang]} :- (-> render-field-schema/RenderFieldArgs
                               (st/dissoc :idx)
                               (st/merge {:lang        lang-schema/Lang
                                          :application s/Any}))]
        (let [row-count @(re-frame/subscribe [:application/question-group-row-count (:id field-descriptor)])]
          (when (> row-count 1)
            (->> row-count
                 dec
                 range
                 (mapv (fn ->valinnainen-kieli-readonly [valinnainen-kieli-idx]
                         (let [key                (str "valinnainen-kieli-rivi-" (:id field-descriptor) "-" valinnainen-kieli-idx)
                               [oppiaine
                                oppimaara
                                oppiaine-kieli
                                arvosana] (:children field-descriptor)
                               oppiaineen-koodi   (some-> @(re-frame/subscribe [:application/answer
                                                                                :oppiaine-valinnainen-kieli
                                                                                valinnainen-kieli-idx])
                                                          :value
                                                          (subs vok/valinnainen-kieli-id-oppiaine-koodi-idx))
                               data-test-id       (str "valinnaiset-kielet-readonly-" (:id field-descriptor))
                               oppimaara-dropdown (if (= oppiaineen-koodi "a")
                                                    oppimaara
                                                    oppiaine-kieli)]
                           ^{:key key}
                           [oar/oppiaineen-arvosana-rivi
                            {:pakollinen-oppiaine?
                             false

                             :readonly?
                             true

                             :label
                             [vkl/valinnainen-kieli-label
                              {:field-descriptor oppiaine
                               :idx              valinnainen-kieli-idx
                               :lang             lang}]

                             :oppimaara-column
                             [vko/valinnainen-kieli-oppimaara
                              {:field-descriptor (merge
                                                   oppimaara-dropdown
                                                   {:readonly-render-options
                                                    {:arvosanat-taulukko? true}
                                                    :data-test-id
                                                    (str data-test-id "-oppimaara-" valinnainen-kieli-idx)})
                               :render-field     render-field
                               :idx              valinnainen-kieli-idx}]

                             :arvosana-column
                             [render-field
                              (merge
                                arvosana
                                {:readonly-render-options
                                 {:arvosanat-taulukko? true}
                                 :data-test-id
                                 (str data-test-id "-arvosana-" valinnainen-kieli-idx)})
                              application
                              lang
                              valinnainen-kieli-idx]}])))
                 (into [:<>])))))
