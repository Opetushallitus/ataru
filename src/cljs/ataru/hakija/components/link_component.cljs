(ns ataru.hakija.components.link-component
  (:require [schema.core :as s]))

(s/defn link
  [{:keys [on-click]} :- {:on-click s/Any}
   label :- s/Any]
  [:a.a-linkki
   {:href     "#"
    :on-click (fn [event]
                (.preventDefault event)
                (on-click))}
   label])
