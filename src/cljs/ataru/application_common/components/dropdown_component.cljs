(ns ataru.application-common.components.dropdown-component
  (:require [ataru.application-common.components.button-component :as button-component]
            [clojure.string :as string]
            [re-frame.core :as re-frame]
            [schema.core :as s]
            [schema-tools.core :as st]
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
      :role          "option"
      :aria-selected (when selected?
                       true)
      :data-test-id  data-test-id}
     (when selected?
       [:i.zmdi.zmdi-check.a-dropdown-list-option__checked])
     [:span label]]))

(s/defn dropdown-list
  [{:keys [expanded?
           options
           on-click
           label-id
           dropdown-id
           selected-value
           data-test-id]} :- {:expanded?      s/Bool
                              :options        [SelectOptionProps]
                              :on-click       s/Any
                              :label-id       s/Str
                              :dropdown-id    s/Str
                              :selected-value (s/maybe s/Str)
                              :data-test-id   (s/maybe s/Str)}]
  (let [options-with-id    (map-indexed (fn [option-idx option-props]
                                          (assoc
                                            option-props
                                            :option-id
                                            (str dropdown-id "-option-" option-idx)))
                                        options)
        selected-option-id (->> options-with-id
                                (filter (fn [{:keys [value]}]
                                          (= value selected-value)))
                                (map :option-id)
                                first)]
    [:div.a-component.a-dropdown-list
     {:data-test-id (str data-test-id "-list")
      :class        (when-not expanded?
                      "a-dropdown-list--collapsed")}
     [:ul.a-dropdown-list-container
      (cond-> {:aria-labelledby label-id
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
                   options-with-id)]]))

(s/defn collapse-dropdown
  [{:keys [dropdown-id]} :- {:dropdown-id s/Str}]
  (re-frame/dispatch [:application-components/collapse-dropdown {:dropdown-id dropdown-id}]))

(s/defn expand-dropdown
  [{:keys [dropdown-id]} :- {:dropdown-id s/Str}]
  (re-frame/dispatch [:application-components/expand-dropdown {:dropdown-id dropdown-id}]))

(defn dropdown []
  (let [dropdown-id (util/component-id)]
    (s/fn render-dropdown
      [{:keys [options
               unselected-label
               selected-value
               on-change
               data-test-id]} :- {:options                       [SelectOptionProps]
                                  :unselected-label              s/Str
                                  :selected-value                (s/maybe s/Str)
                                  :on-change                     s/Any
                                  (s/optional-key :data-test-id) (s/maybe s/Str)}]
      (let [expanded?                @(re-frame/subscribe [:state-query [:components :dropdown dropdown-id :expanded?] false])
            on-dropdown-value-change (fn on-dropdown-value-change [event]
                                       (collapse-dropdown {:dropdown-id dropdown-id})
                                       (on-change event))
            on-dropdown-button-click (fn on-dropdown-button-click []
                                       (if expanded?
                                         (collapse-dropdown {:dropdown-id dropdown-id})
                                         (expand-dropdown {:dropdown-id dropdown-id})))
            button-label             (if-not (string/blank? selected-value)
                                       (->> options
                                            (filter (fn filter-dropdown-select-option [{option-value :value}]
                                                      (= option-value selected-value)))
                                            (map :label)
                                            (first))
                                       unselected-label)
            label-id                 (str dropdown-id "-label")]
        [:div.a-dropdown
         [:div.a-dropdown-button-container.a-component
          {:class (when expanded?
                    "a-component")}
          [button-component/button
           (cond-> {:label        button-label
                    :on-click     on-dropdown-button-click
                    :data-test-id (str data-test-id "-button")
                    :aria-attrs   {:aria-haspopup   "listbox"
                                   :aria-labelledby label-id}}
                   expanded?
                   (assoc-in [:aria-attrs :aria-expanded] true))]
          [dropdown-chevron
           {:expanded? expanded?}]]
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
          {:expanded?      expanded?
           :options        options
           :on-click       on-dropdown-value-change
           :label-id       label-id
           :dropdown-id    dropdown-id
           :selected-value selected-value
           :data-test-id   data-test-id}]]))))
