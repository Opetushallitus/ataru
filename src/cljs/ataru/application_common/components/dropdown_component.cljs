(ns ataru.application-common.components.dropdown-component
  (:require [ataru.application-common.components.button-component :as button-component]
            [schema.core :as s]))

(s/defschema SelectOptionProps
  {:value s/Str
   :label s/Str})

(s/defn dropdown-select-option
  [{:keys [value
           label]} :- SelectOptionProps]
  [:option
   {:value value}
   label])

(s/defn dropdown-select
  [{:keys [options
           unselected-label
           on-change]} :- {:options          [SelectOptionProps]
                           :unselected-label s/Str
                           :on-change        s/Any}]
  [:select.a-native-component
   {:aria-hidden true
    :on-change   on-change}
   [dropdown-select-option
    {:value ""
     :label unselected-label}]
   (map (fn [{:keys [value] :as option-props}]
          (let [key (str "dropdown-select-option-" value)]
            ^{:key key}
            [dropdown-select-option option-props]))
        options)])

(s/defn dropdown
  [{:keys [options
           unselected-label
           on-change]} :- {:options          [SelectOptionProps]
                           :unselected-label s/Str
                           :on-change        s/Any}]
  [:div
   [button-component/button
    {:label unselected-label}]
   ;[dropdown-select
   ; {:options          options
   ;  :unselected-label unselected-label
   ;  :on-change        on-change}]
   ])
