(ns ataru.hakija.arvosanat.components.oppiaineen-arvosana-readonly
  (:require [ataru.schema.lang-schema :as lang-schema]
            [ataru.hakija.arvosanat.components.oppiaineen-arvosana-rivi :as oar]
            [ataru.translations.translation-util :as translations]
            [re-frame.core :as re-frame]
            [schema.core :as s]
            [schema-tools.core :as st]))

(s/defn oppiaineen-arvosana-readonly
        [{:keys [field-descriptor
                 application
                 render-field
                 lang]} :- (st/open-schema
                             {:field-descriptor s/Any
                              :application      s/Any
                              :render-field     s/Any
                              :lang             lang-schema/Lang})]
        (let [row-count @(re-frame/subscribe [:application/question-group-row-count (:id field-descriptor)])]
          [:<>
           (map (fn ->oppiaineen-arvosana-rivi-readonly [arvosana-idx]
                  (let [key                 (str "oppiaineen-arvosana-rivi-" (:id field-descriptor) "-" arvosana-idx)
                        valinnaisaine-rivi? (> arvosana-idx 0)
                        children            (:children field-descriptor)
                        arvosana-dropdown   (some-> children
                                                    last
                                                    (assoc
                                                      :readonly-render-options
                                                      {:arvosanat-taulukko? true}))
                        oppimaara-dropdown  (when (= (count children) 2)
                                              (-> children
                                                  first
                                                  (assoc
                                                    :readonly-render-options
                                                    {:arvosanat-taulukko? true})))
                        data-test-id        (str "oppiaineen-arvosana-readonly-" (:id field-descriptor))]
                    ^{:key key}
                    [oar/oppiaineen-arvosana-rivi
                     {:pakollinen-oppiaine?
                      (not valinnaisaine-rivi?)

                      :readonly?
                      true

                      :label
                      (cond->> (-> field-descriptor :label lang)
                               valinnaisaine-rivi?
                               (translations/get-hakija-translation :oppiaine-valinnainen lang))

                      :oppimaara-column
                      (when oppimaara-dropdown
                        [render-field
                         (assoc oppimaara-dropdown :data-test-id (str data-test-id "-oppimaara-" arvosana-idx))
                         application
                         lang arvosana-idx])

                      :arvosana-column
                      (when arvosana-dropdown
                        [render-field
                         (assoc arvosana-dropdown :data-test-id (str data-test-id "-arvosana-" arvosana-idx))
                         application
                         lang
                         arvosana-idx])}]))
                (range row-count))]))
