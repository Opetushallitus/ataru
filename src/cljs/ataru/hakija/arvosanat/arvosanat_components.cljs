(ns ataru.hakija.arvosanat.arvosanat-components
  (:require [ataru.schema.lang-schema :as lang-schema]
            [ataru.hakija.schema.render-field-schema :as render-field-schema]
            [ataru.translations.translation-util :as translations]
            [ataru.hakija.components.link-component :as link-component]
            [clojure.string :as string]
            [re-frame.core :as re-frame]
            [schema.core :as s]
            [schema-tools.core :as st]))

(s/defn oppiaineen-arvosana-rivi
  [{:keys [label
           oppimaara-dropdown
           arvosana-dropdown
           lisaa-valinnaisaine-linkki
           pakollinen-oppiaine?
           data-test-id]} :- {:label                                       s/Any
                              (s/optional-key :oppimaara-dropdown)         s/Any
                              (s/optional-key :arvosana-dropdown)          s/Any
                              (s/optional-key :lisaa-valinnaisaine-linkki) s/Any
                              :pakollinen-oppiaine?                        s/Bool
                              (s/optional-key :data-test-id)               s/Str}]
  [:div.arvosanat-taulukko__rivi
   {:class        (when pakollinen-oppiaine?
                    "arvosanat-taulukko__rivi--pakollinen-oppiaine")
    :data-test-id data-test-id}
   [:div.arvosanat-taulukko__solu.arvosana__oppiaine
    {:class (when-not oppimaara-dropdown
              "arvosanat-taulukko__solu--span-2")}
    label]
   (when oppimaara-dropdown
     [:div.arvosanat-taulukko__solu.arvosana__oppimaara
      oppimaara-dropdown])
   (when arvosana-dropdown
     [:div.arvosanat-taulukko__solu.arvosana__arvosana
      arvosana-dropdown])
   [:div.arvosanat-taulukko__solu.arvosana__lisaa-valinnaisaine.arvosana__lisaa-valinnaisaine--solu
    lisaa-valinnaisaine-linkki]])

(def ^:private max-valinnaisaine-amount 3)

(defn- answered? [{:keys [value valid]}]
  (and valid
       (-> value string/blank? not)))

(s/defn lisaa-valinnaisaine-linkki
  [{:keys [valinnaisaine-rivi?
           arvosana-dropdown
           oppimaara-dropdown
           lang
           arvosana-idx
           field-descriptor
           row-count
           data-test-id]} :- {:valinnaisaine-rivi? s/Bool
                              :arvosana-dropdown   s/Any
                              :oppimaara-dropdown  (s/maybe s/Any)
                              :lang                lang-schema/Lang
                              :arvosana-idx        s/Int
                              :field-descriptor    s/Any
                              :row-count           s/Int
                              :data-test-id        s/Str}]
  (let [label               (if valinnaisaine-rivi?
                              (translations/get-hakija-translation :poista lang)
                              (translations/get-hakija-translation :lisaa-valinnaisaine lang))
        arvosana-answered?  (some-> (when arvosana-dropdown
                                      @(re-frame/subscribe [:application/answer (:id arvosana-dropdown) arvosana-idx]))
                                    answered?)
        oppimaara-answered? (or (nil? oppimaara-dropdown)
                                (-> @(re-frame/subscribe [:application/answer (:id oppimaara-dropdown) arvosana-idx])
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

                     :oppimaara-dropdown
                     (when oppimaara-dropdown
                       [render-field
                        (assoc oppimaara-dropdown :data-test-id (str data-test-id "-oppimaara-" arvosana-idx))
                        arvosana-idx])

                     :arvosana-dropdown
                     (when arvosana-dropdown
                       [render-field
                        (assoc arvosana-dropdown :data-test-id (str data-test-id "-arvosana-" arvosana-idx))
                        arvosana-idx])

                     :lisaa-valinnaisaine-linkki
                     [lisaa-valinnaisaine-linkki
                      {:valinnaisaine-rivi? valinnaisaine-rivi?
                       :arvosana-dropdown   arvosana-dropdown
                       :oppimaara-dropdown  oppimaara-dropdown
                       :lang                lang
                       :arvosana-idx        arvosana-idx
                       :field-descriptor    field-descriptor
                       :row-count           row-count
                       :data-test-id        (str data-test-id "-lisaa-valinnaisaine-linkki-" arvosana-idx "-" (if valinnaisaine-rivi? "poista" "lisaa"))}]}])))
         (into [:<>]))))

(s/defn arvosanat-taulukko-otsikkorivi
  [{:keys [lang
           readonly?]} :- {:lang      lang-schema/Lang
                           :readonly? s/Bool}]
  [:div.arvosanat-taulukko__rivi
   [:div.arvosanat-taulukko__solu.arvosanat-taulukko__otsikko.arvosana__oppiaine
    [:span (translations/get-hakija-translation :oppiaine lang)]]
   (when readonly?
     [:<>
      [:div.arvosanat-taulukko__solu.arvosanat-taulukko__otsikko.arvosana__oppimaara
       [:span (translations/get-hakija-translation :oppimaara lang)]]
      [:div.arvosanat-taulukko__solu.arvosanat-taulukko__otsikko.arvosana__arvosana
       [:span (translations/get-hakija-translation :arvosana lang)]]])
   [:div.arvosanat-taulukko__solu.arvosanat-taulukko__otsikko.arvosana__lisaa-valinnaisaine
    [:span (translations/get-hakija-translation :valinnaisaine lang)]]])

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
                                              {:arvosanat-taulukko? true})))]
              ^{:key key}
              [oppiaineen-arvosana-rivi
               {:pakollinen-oppiaine?
                (not valinnaisaine-rivi?)
                :label
                (cond->> (-> field-descriptor :label lang)
                         valinnaisaine-rivi?
                         (translations/get-hakija-translation :oppiaine-valinnainen lang))

                :oppimaara-dropdown
                (when oppimaara-dropdown
                  [render-field oppimaara-dropdown application lang arvosana-idx])

                :arvosana-dropdown
                (when arvosana-dropdown
                  [render-field arvosana-dropdown application lang arvosana-idx])

                :lisaa-valinnaisaine-linkki
                nil}]))
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
   (map (fn field-descriptor->oppiaineen-arvosana-readonly [arvosana-data]
          (let [arvosana-koodi (:id arvosana-data)
                key            (str "arvosana-" arvosana-koodi)]
            ^{:key key}
            [render-field arvosana-data application lang idx]))
        (:children field-descriptor))])
