(ns ataru.application-common.components.button-component
  (:require [schema.core :as s]))

(s/defn button
  [{:keys [label]} :- {:label s/Str}]
  [:button.a-button
   {:type "button"}
   [:span label]
   [:i.zmdi.zmdi-chevron-down.a-button__chevron
    {:aria-hidden true}]])
