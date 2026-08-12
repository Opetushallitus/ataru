(ns ataru.application-common.components.dropdown-component
  (:require [ataru.application-common.components.button-component :as button-component]
            [clojure.string :as string]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [schema.core :as s]
            [schema-tools.core :as st]
            [ataru.translations.translation-util :as translations]
            [ataru.util :as util]))

(s/defschema SelectOptionProps
  {:value s/Str
   :label s/Str})

(s/defn dropdown-chevron
  [{:keys [expanded?]} :- {:expanded? s/Bool}]
  [:i.zmdi.a-dropdown-button--chevron
   {:aria-hidden true
    :class       (if expanded?
                   "zmdi-chevron-up"
                   "zmdi-chevron-down")}])

(s/defn dropdown-select-option
  [{:keys [value
           label
           selected-value]} :- (st/assoc
                                 SelectOptionProps
                                 :selected-value
                                 (s/maybe s/Str))]
  [:option
   {:value    value
    :selected (when (= value selected-value)
                true)}
   label])

(s/defn dropdown-select
  [{:keys [expanded?
           options
           unselected-label
           selected-value
           on-click
           dropdown-id
           on-change]} :- {:expanded?        s/Bool
                           :options          [SelectOptionProps]
                           :unselected-label s/Str
                           :selected-value   (s/maybe s/Str)
                           :on-click         s/Any
                           :dropdown-id      s/Str
                           :on-change        s/Any}]
  [:div.a-native-component.a-dropdown-select-container
   [:select.a-dropdown-select
    {:aria-hidden true
     :on-click    on-click
     :on-change   (fn dropdown-select-on-change [event]
                    (let [value (.. event -target -value)]
                      (on-change value)))
     :value       (or selected-value "")}
    [dropdown-select-option
     {:value          ""
      :label          unselected-label
      :selected-value nil}]
    (map-indexed (fn [option-idx option-props]
                   (let [key (str "dropdown-select-" dropdown-id "-option-" option-idx)]
                     ^{:key key}
                     [dropdown-select-option (assoc
                                               option-props
                                               :selected-value
                                               selected-value)]))
                 options)]
   [dropdown-chevron
    {:expanded? expanded?}]])

(s/defn dropdown-list-option
  [{:keys [value
           label
           on-click
           option-id
           selected-value
           data-test-id]} :- (st/assoc
                               SelectOptionProps
                               :on-click s/Any
                               :option-id s/Str
                               :selected-value (s/maybe s/Str)
                               :data-test-id (s/maybe s/Str))]
  (let [selected? (= selected-value value)]
    [:li.a-dropdown-list__option
     {:id            option-id
      :on-click      (fn dropdown-list-option-on-click []
                       (on-click value))
      :on-key-down   (fn [e]
                       (when (or (= " " (.-key e))
                                 (= "Enter" (.-key e)))
                         (.preventDefault e)
                         (on-click value)))
      :role          "option"
      :aria-selected (when selected?
                       true)
      :data-test-id  data-test-id
      :tab-index     "0"}
     (when selected?
       [:i.zmdi.zmdi-check.a-dropdown-list-option__checked])
     [:span label]]))

(s/defn dropdown-search-input
  [{:keys [query
           on-change
           on-key-down
           listbox-id
           lang
           input-ref]} :- {:query       (s/maybe s/Str)
                           :on-change   s/Any
                           :on-key-down s/Any
                           :listbox-id  s/Str
                           :lang        s/Keyword
                           :input-ref   s/Any}]
  [:div.a-dropdown-search
   [:i.zmdi.zmdi-search.a-dropdown-search__icon
    {:aria-hidden true}]
   [:input.a-dropdown-search__input
    {:ref                input-ref
     :type               "text"
     :value              (or query "")
     :placeholder        (translations/get-hakija-translation :search-dropdown-options lang)
     :aria-label         (translations/get-hakija-translation :search-dropdown-options lang)
     :role               "combobox"
     :aria-expanded      true
     :aria-haspopup      "listbox"
     :aria-controls      listbox-id
     :aria-autocomplete  "list"
     :on-change          (fn dropdown-search-input-on-change [event]
                           (on-change (.. event -target -value)))
     :on-key-down        on-key-down}]])

(s/defn dropdown-list
  [{:keys [expanded?
           options
           on-click
           label-id
           dropdown-id
           selected-value
           query
           on-query-change
           on-search-key-down
           input-ref
           lang
           data-test-id]} :- {:expanded?          s/Bool
                              :options            [SelectOptionProps]
                              :on-click           s/Any
                              :label-id           s/Str
                              :dropdown-id        s/Str
                              :selected-value     (s/maybe s/Str)
                              :query              (s/maybe s/Str)
                              :on-query-change    s/Any
                              :on-search-key-down s/Any
                              :input-ref          s/Any
                              :lang               s/Keyword
                              :data-test-id       (s/maybe s/Str)}]
  (let [filtered-options   (if (string/blank? query)
                             options
                             (let [query-lower (string/lower-case query)]
                               (filter (fn [{:keys [label]}]
                                        (string/includes? (string/lower-case label) query-lower))
                                      options)))
        options-with-id    (map-indexed (fn [option-idx option-props]
                                          (assoc
                                            option-props
                                            :option-id
                                            (str dropdown-id "-option-" option-idx)))
                                        filtered-options)
        selected-option-id (->> options-with-id
                                (filter (fn [{:keys [value]}]
                                          (= value selected-value)))
                                (map :option-id)
                                first)
        listbox-id         (str dropdown-id "-listbox")]
    [:div.a-component.a-dropdown-list
     {:data-test-id (str data-test-id "-list")
      :class        (when-not expanded?
                      "a-dropdown-list--collapsed")}
     [dropdown-search-input
      {:query       query
       :on-change   on-query-change
       :on-key-down on-search-key-down
       :listbox-id  listbox-id
       :lang        lang
       :input-ref   input-ref}]
     (if (empty? options-with-id)
       [:p.a-dropdown-list__no-results
        (translations/get-hakija-translation :no-dropdown-search-hits lang)]
       [:ul.a-dropdown-list-container
        (cond-> {:id              listbox-id
                 :aria-labelledby label-id
                 :role            "listbox"
                 :tab-index       "-1"}
                (not (string/blank? selected-value))
                (assoc :aria-activedescendant selected-option-id))
        (map-indexed (fn [option-idx option-props]
                       (let [key (str "dropdown-list-" dropdown-id "-option-" option-idx)]
                         ^{:key key}
                         [dropdown-list-option (merge option-props
                                                      (cond-> {:on-click       on-click
                                                               :selected-value selected-value
                                                               :data-test-id   (str data-test-id "-option-" (:value option-props))}))]))
                     options-with-id)])]))

(s/defn collapse-dropdown
  [{:keys [dropdown-id]} :- {:dropdown-id s/Str}]
  (re-frame/dispatch [:application-components/collapse-dropdown {:dropdown-id dropdown-id}]))

(s/defn expand-dropdown
  [{:keys [dropdown-id]} :- {:dropdown-id s/Str}]
  (re-frame/dispatch [:application-components/expand-dropdown {:dropdown-id dropdown-id}]))

(defn dropdown []
  (let [dropdown-id (util/component-id)
        button-id   (str dropdown-id "-button")
        input-ref   (atom nil)
        focus-input (fn []
                      (reagent/after-render
                        (fn []
                          (when-let [el @input-ref]
                            (.focus el)))))
        focus-button (fn []
                       (reagent/after-render
                         (fn []
                           (when-let [el (.getElementById js/document button-id)]
                             (.focus el)))))]
    (s/fn render-dropdown
      [{:keys [options
               unselected-label
               unselected-label-icon
               selected-value
               on-change
               data-test-id]} :- {:options                                [SelectOptionProps]
                                  :unselected-label                       s/Str
                                  (s/optional-key :unselected-label-icon) [(s/one s/Str "icon component")]
                                  :selected-value                         (s/maybe s/Str)
                                  :on-change                              s/Any
                                  (s/optional-key :data-test-id)          (s/maybe s/Str)}]
      (let [lang                     @(re-frame/subscribe [:application/form-language])
            expanded?                @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :expanded?] false])
            query                    @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :query] nil])
            on-dropdown-value-change (fn on-dropdown-value-change [event]
                                       (collapse-dropdown {:dropdown-id dropdown-id})
                                       (on-change event))
            on-dropdown-button-click (fn on-dropdown-button-click []
                                       (if expanded?
                                         (collapse-dropdown {:dropdown-id dropdown-id})
                                         (do (expand-dropdown {:dropdown-id dropdown-id})
                                             (focus-input))))
            on-query-change          (fn on-query-change [value]
                                       (re-frame/dispatch [:application-components/set-dropdown-query
                                                            {:dropdown-id dropdown-id
                                                             :query       value}]))
            on-search-key-down       (fn on-search-key-down [e]
                                       (when (= "Escape" (.-key e))
                                         (.preventDefault e)
                                         (collapse-dropdown {:dropdown-id dropdown-id})
                                         (focus-button)))
            label-id                 (str dropdown-id "-label")
            button-label             (if-not (string/blank? selected-value)
                                       (->> options
                                            (filter (fn filter-dropdown-select-option [{option-value :value}]
                                                      (= option-value selected-value)))
                                            (map :label)
                                            (first))
                                       unselected-label)]
        [:div.a-dropdown
         [:div.a-dropdown-button-container.a-component
          {:class (when expanded?
                    "a-component")}
          [button-component/button
           {:label        (cond->> [:span.a-dropdown-button__label
                                    {:aria-labelledby label-id
                                     :aria-expanded   expanded?}
                                    button-label]
                            (seq unselected-label-icon)
                            (conj [:<> unselected-label-icon]))
            :on-click     on-dropdown-button-click
            :id           button-id
            :data-test-id (str data-test-id "-button")
            :aria-attrs   {:aria-haspopup "listbox"}
            :tab-index    "0"}]
          (when-not (seq unselected-label-icon)
            [dropdown-chevron
             {:expanded? expanded?}])]
         [dropdown-select
          {:expanded?        expanded?
           :options          options
           :unselected-label unselected-label
           :selected-value   selected-value
           :dropdown-id      dropdown-id
           :on-click         (fn []
                               (expand-dropdown {:dropdown-id dropdown-id}))
           :on-change        on-dropdown-value-change}]
         [dropdown-list
          {:expanded?           expanded?
           :options             options
           :on-click            on-dropdown-value-change
           :label-id            label-id
           :dropdown-id         dropdown-id
           :selected-value      selected-value
           :query               query
           :on-query-change     on-query-change
           :on-search-key-down  on-search-key-down
           :input-ref           #(reset! input-ref %)
           :lang                lang
           :data-test-id        data-test-id}]]))))
