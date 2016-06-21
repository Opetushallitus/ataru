(ns ataru.virkailija.application.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [ataru.virkailija.application.handlers]))

(defn applications []
  (let [applications (subscribe [:state-query [:application :applications]])]
    (into [:div.application__list]
          (for [application @applications]
            (:modified-time application)))))


(defn application-list []
  (let [form (subscribe [:state-query [:application :form]])]
    [:div.application__name
     (:name @form)
     [applications]]))

(defn application []
  [application-list])
