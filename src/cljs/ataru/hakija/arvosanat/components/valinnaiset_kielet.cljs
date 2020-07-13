(ns ataru.hakija.arvosanat.components.valinnaiset-kielet
  (:require [schema.core :as s]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [schema-tools.core :as st]
            [re-frame.core :as re-frame]
            [clojure.string :as string]
            [ataru.hakija.arvosanat.components.oppiaineen-arvosana-rivi :as oar]
            [ataru.hakija.arvosanat.components.valinnainen-kieli-label :as vkl]
            [ataru.hakija.arvosanat.components.valinnainen-kieli-oppimaara :as vko]
            [ataru.hakija.arvosanat.components.poista-valinnainen-kieli :as pvk]
            [ataru.hakija.arvosanat.components.valinnainen-kieli-dropdown :as vkd]
            [ataru.hakija.arvosanat.valinnainen-oppiaine-koodi :as vok]))

(s/defn valinnaiset-kielet
        [{:keys [field-descriptor
                 render-field]} :- (-> render-field-schema/RenderFieldArgs
                                       (st/dissoc :idx)
                                       st/open-schema)]
        (let [row-count                   @(re-frame/subscribe [:application/question-group-row-count (:id field-descriptor)])
              [oppiaine-dropdown
               oppimaara-dropdown
               oppiaine-kieli-dropdown
               arvosana-dropdown] (:children field-descriptor)
              last-oppiaine-answer        @(re-frame/subscribe [:application/answer
                                                                :oppiaine-valinnainen-kieli
                                                                (dec row-count)])
              lang                        @(re-frame/subscribe [:application/form-language])
              data-test-id                "valinnaiset-kielet-oppiaine"
              valinnaiset-kielet-rows     (->> (cond-> row-count
                                                       (string/blank? (:value last-oppiaine-answer))
                                                       dec)
                                               range
                                               (mapv (fn ->valinnainen-kieli-rivi [valinnainen-kieli-rivi-idx]
                                                       (let [key              (str "valinnainen-kieli-rivi-" (:id field-descriptor) "-" valinnainen-kieli-rivi-idx)
                                                             oppiaineen-koodi (some-> @(re-frame/subscribe [:application/answer
                                                                                                            :oppiaine-valinnainen-kieli
                                                                                                            valinnainen-kieli-rivi-idx])
                                                                                      :value
                                                                                      (subs vok/valinnainen-kieli-id-oppiaine-koodi-idx))]
                                                         ^{:key key}
                                                         [oar/oppiaineen-arvosana-rivi
                                                          {:pakollinen-oppiaine?
                                                           false

                                                           :label
                                                           [vkl/valinnainen-kieli-label {:field-descriptor oppiaine-dropdown
                                                                                         :idx              valinnainen-kieli-rivi-idx
                                                                                         :lang             lang}]

                                                           :oppimaara-column
                                                           [vko/valinnainen-kieli-oppimaara
                                                            {:field-descriptor (assoc (if (= oppiaineen-koodi "a")
                                                                                        oppimaara-dropdown
                                                                                        oppiaine-kieli-dropdown)
                                                                                      :data-test-id (str data-test-id "-oppimaara-" valinnainen-kieli-rivi-idx))
                                                             :render-field     render-field
                                                             :idx              valinnainen-kieli-rivi-idx}]

                                                           :arvosana-column
                                                           [render-field
                                                            (assoc arvosana-dropdown
                                                                   :data-test-id
                                                                   (str data-test-id "-arvosana-" valinnainen-kieli-rivi-idx))
                                                            valinnainen-kieli-rivi-idx]

                                                           :valinnaisaine-column
                                                           [pvk/poista-valinnainen-kieli
                                                            {:field-descriptor field-descriptor
                                                             :idx              valinnainen-kieli-rivi-idx}]}]))))
              lisaa-valinnainen-kieli-row [oar/oppiaineen-arvosana-rivi
                                           {:pakollinen-oppiaine?
                                            false

                                            :label
                                            [vkd/valinnainen-kieli-dropdown
                                             {:valinnainen-kieli-field-descriptor  (assoc
                                                                                     oppiaine-dropdown
                                                                                     :data-test-id
                                                                                     (str data-test-id "-dropdown"))
                                              :valinnaiset-kielet-field-descriptor field-descriptor
                                              :render-field                        render-field
                                              :idx                                 (dec row-count)}]}]]
          (as-> [:<>] valinnaiset-kielet-component

                (cond-> valinnaiset-kielet-component
                        (> (count valinnaiset-kielet-rows) 0)
                        (into valinnaiset-kielet-rows))

                (conj valinnaiset-kielet-component
                      lisaa-valinnainen-kieli-row))))
