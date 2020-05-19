(ns ataru.hakija.components.link-component
  (:require [schema.core :as s]))

(s/defn link
  [{:keys [disabled?
           on-click]} :- {:disabled? s/Bool
                          :on-click  s/Any}
   label :- s/Any]
  [:a.a-linkki
   {:href     "#"
    :disabled disabled?
    :on-click (fn [event]
                (.preventDefault event)
                (when-not disabled?
                  (on-click)))}
   label])
