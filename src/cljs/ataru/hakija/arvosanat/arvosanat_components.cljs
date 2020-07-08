(ns ataru.hakija.arvosanat.arvosanat-components
  (:require [ataru.schema.lang-schema :as lang-schema]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [ataru.translations.translation-util :as translations]
            [ataru.hakija.components.link-component :as link-component]
            [ataru.hakija.components.dropdown-component :as dropdown-component]
            [clojure.string :as string]
            [re-frame.core :as re-frame]
            [schema.core :as s]
            [schema-tools.core :as st]
            [ataru.translations.texts :as texts]))

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

(def ^:private max-valinnaisaine-amount 3)

(defn- answered? [{:keys [value valid]}]
  (and valid
       (-> value string/blank? not)))

(s/defn lisaa-valinnaisaine-linkki
  [{:keys [valinnaisaine-rivi?
           arvosana-column
           oppimaara-column
           lang
           arvosana-idx
           field-descriptor
           row-count
           data-test-id]} :- {:valinnaisaine-rivi? s/Bool
                              :arvosana-column     s/Any
                              :oppimaara-column    (s/maybe s/Any)
                              :lang                lang-schema/Lang
                              :arvosana-idx        s/Int
                              :field-descriptor    s/Any
                              :row-count           s/Int
                              :data-test-id        s/Str}]
  (let [label               (if valinnaisaine-rivi?
                              (translations/get-hakija-translation :poista lang)
                              (translations/get-hakija-translation :lisaa-valinnaisaine lang))
        arvosana-answered?  (some-> (when arvosana-column
                                      @(re-frame/subscribe [:application/answer (:id arvosana-column) arvosana-idx]))
                                    answered?)
        oppimaara-answered? (or (nil? oppimaara-column)
                                (-> @(re-frame/subscribe [:application/answer (:id oppimaara-column) arvosana-idx])
                                    answered?))
        disabled?           (and (not valinnaisaine-rivi?)
                                 (or (not (< (dec row-count) max-valinnaisaine-amount))
                                     (not arvosana-answered?)
                                     (not oppimaara-answered?)))]
    [link-component/link
     (cond-> {:on-click  (fn add-or-remove-oppiaineen-valinnaisaine-row []
                           (if valinnaisaine-rivi?
                             (re-frame/dispatch [:application/remove-question-group-row field-descriptor arvosana-idx])
                             (re-frame/dispatch [:application/add-question-group-row field-descriptor])))
              :disabled? disabled?}
             data-test-id
             (assoc :data-test-id data-test-id))
     (if valinnaisaine-rivi?
       [:span label]
       [:<>
        [:i.zmdi.zmdi-plus-circle-o.arvosana__lisaa-valinnaisaine--ikoni]
        [:span.arvosana__lisaa-valinnaisaine--linkki label]])]))

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
                   [oppiaineen-arvosana-rivi
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
                     [lisaa-valinnaisaine-linkki
                      {:valinnaisaine-rivi? valinnaisaine-rivi?
                       :arvosana-column     arvosana-dropdown
                       :oppimaara-column    oppimaara-dropdown
                       :lang                lang
                       :arvosana-idx        arvosana-idx
                       :field-descriptor    field-descriptor
                       :row-count           row-count
                       :data-test-id        (str data-test-id "-lisaa-valinnaisaine-linkki-" arvosana-idx "-" (if valinnaisaine-rivi? "poista" "lisaa"))}]}])))
         (into [:<>]))))

(s/defn valinnainen-kieli-dropdown
  [{:keys [valinnainen-kieli-field-descriptor
           valinnaiset-kielet-field-descriptor
           render-field
           idx]} :- (-> render-field-schema/RenderFieldArgs
                        (st/select-keys [:render-field :idx])
                        (st/merge {:valinnainen-kieli-field-descriptor  s/Any
                                   :valinnaiset-kielet-field-descriptor s/Any}))]
  [dropdown-component/hakija-dropdown
   {:field-descriptor valinnainen-kieli-field-descriptor
    :render-field     render-field
    :idx              idx
    :on-change        (fn []
                        (re-frame/dispatch [:application/add-question-group-row valinnaiset-kielet-field-descriptor]))}])

(def ^:private valinnainen-kieli-id-oppiaine-koodi-idx
  (count "oppiaine-valinnainen-kieli-"))

(s/defn valinnainen-kieli-label
  [{:keys [field-descriptor
           idx
           lang]} :- {:field-descriptor s/Any
                      :idx              s/Int
                      :lang             lang-schema/Lang}]
  (let [answer @(re-frame/subscribe [:application/answer
                                     (:id field-descriptor)
                                     idx])
        label  (as-> (:value answer) key
                     (subs key valinnainen-kieli-id-oppiaine-koodi-idx)
                     (str "oppiaine-" key)
                     (keyword key)
                     (-> texts/oppiaine-translations key lang))]
    [:span label]))

(s/defn valinnainen-kieli-oppimaara
  [{:keys [field-descriptor
           render-field
           idx]} :- render-field-schema/RenderFieldArgs]
  (let [oppiaine (some-> @(re-frame/subscribe [:application/answer
                                               :oppiaine-valinnainen-kieli
                                               idx])
                         :value
                         (subs valinnainen-kieli-id-oppiaine-koodi-idx))
        label    (if (= oppiaine "a")
                   (:oppimaara texts/translation-mapping)
                   (:oppiaine texts/translation-mapping))]
    [render-field
     (assoc field-descriptor :unselected-label label)
     idx]))

(s/defn poista-valinnainen-kieli
  [{:keys [field-descriptor
           idx]} :- {:field-descriptor s/Any
                     :idx              s/Int}]
  (let [lang @(re-frame/subscribe [:application/form-language])]
    [link-component/link
     {:on-click  (fn remove-valinnainen-kieli-row []
                   (re-frame/dispatch [:application/remove-question-group-row field-descriptor idx]))
      :disabled? false}
     (translations/get-hakija-translation :remove lang)]))

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
                                                                                (subs (count "oppiaine-valinnainen-kieli-")))]
                                                   ^{:key key}
                                                   [oppiaineen-arvosana-rivi
                                                    {:pakollinen-oppiaine?
                                                     false

                                                     :label
                                                     [valinnainen-kieli-label {:field-descriptor oppiaine-dropdown
                                                                               :idx              valinnainen-kieli-rivi-idx
                                                                               :lang             lang}]

                                                     :oppimaara-column
                                                     [valinnainen-kieli-oppimaara
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
                                                     [poista-valinnainen-kieli
                                                      {:field-descriptor field-descriptor
                                                       :idx              valinnainen-kieli-rivi-idx}]}]))))
        lisaa-valinnainen-kieli-row [oppiaineen-arvosana-rivi
                                     {:pakollinen-oppiaine?
                                      false

                                      :label
                                      [valinnainen-kieli-dropdown
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
                                                    (subs (count "oppiaine-valinnainen-kieli-")))
                         data-test-id       (str "valinnaiset-kielet-readonly-" (:id field-descriptor))
                         oppimaara-dropdown (if (= oppiaineen-koodi "a")
                                              oppimaara
                                              oppiaine-kieli)]
                     ^{:key key}
                     [oppiaineen-arvosana-rivi
                      {:pakollinen-oppiaine?
                       false

                       :readonly?
                       true

                       :label
                       [valinnainen-kieli-label
                        {:field-descriptor oppiaine
                         :idx              valinnainen-kieli-idx
                         :lang             lang}]

                       :oppimaara-column
                       [valinnainen-kieli-oppimaara
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

(s/defn arvosanat-taulukko
  [{:keys [field-descriptor
           render-field
           idx]} :- render-field-schema/RenderFieldArgs]
  (let [lang @(re-frame/subscribe [:application/form-language])]
    [:div.arvosanat-taulukko
     [arvosanat-taulukko-otsikkorivi
      {:lang      lang
       :readonly? false}]
     (map (fn field-descriptor->oppiaineen-arvosana [arvosana-data]
            (let [arvosana-koodi (:id arvosana-data)
                  key            (str "arvosana-" arvosana-koodi)]
              ^{:key key}
              [render-field arvosana-data idx]))
          (:children field-descriptor))]))

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
              [oppiaineen-arvosana-rivi
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

(s/defn arvosanat-taulukko-readonly
  [{:keys [field-descriptor
           render-field
           lang
           application
           idx]} :- {:field-descriptor s/Any
                     :render-field     s/Any
                     :lang             lang-schema/Lang
                     :application      s/Any
                     :idx              (s/maybe s/Int)}]
  [:div.arvosanat-taulukko
   [arvosanat-taulukko-otsikkorivi
    {:lang      lang
     :readonly? true}]
   (->> (:children field-descriptor)
        (filter (fn [arvosana-data]
                  (let [arvosana-koodi (:id arvosana-data)
                        value          @(re-frame/subscribe [:application/answer
                                                             (keyword (str "arvosana-" arvosana-koodi))
                                                             0])]
                    (or (= arvosana-koodi "oppiaineen-arvosanat-valinnaiset-kielet")
                        (-> value :value string/blank? not)))))
        (map (fn field-descriptor->oppiaineen-arvosana-readonly [arvosana-data]
               (let [arvosana-koodi (:id arvosana-data)
                     key            (str "arvosana-" arvosana-koodi)]
                 ^{:key key}
                 [render-field arvosana-data application lang idx])))
        doall)])
