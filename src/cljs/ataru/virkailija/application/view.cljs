(ns ataru.virkailija.application.view
  (:require [re-frame.core :as re-frame]
            [ataru.virkailija.application.handlers]
            [reagent.core :as r]))

(defn applications []
  (let [applications (subscribe [:state-query [:application :applications]])]
    (into [:div.application__list]
          (for [application @applications]
            (:modified-time)))))


(defn application-list []
  (let [form (subscribe [:state-query [:application :form]])]
    [:div.application__name
     (:name @form)
     [applications]]))

(defn application []
  [application-list])
