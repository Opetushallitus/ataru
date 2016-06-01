(ns ataru.hakija.banner
  (:require [re-frame.core :refer [subscribe]]))

(def logo
  [:div.logo
   [:img {:src "images/opintopolku_large-fi.png"
          :height "40px"}]])

(defn apply-button []
  (let [valid-status (subscribe [:application/valid-status])]
    (fn []
      [:button.application__send-application-button {:disabled (not (:valid @valid-status))} "LÄHETÄ HAKEMUS"])))

(defn banner [] [:div.top-banner.application-top-banner logo [apply-button]])
