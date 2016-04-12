(ns lomake-editori.views.banner
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [re-com.core :as c]))

(def logo
  [:div.logo
    [:img {:src "/images/opintopolku_large-fi.png"
           :height "40px"}]])

(def title
  [c/h-box
   :class "title"
   :width "100%"
   :align :center
   :gap "1em"
   :children
   [[c/box
     :child [:span "Virkailijan Työpöytä"]
     :class "section-name"]
    [c/box
     :child [:span ""]
     :class "divider"
     :size "1px"]
    [c/box
     :child [:span "Hakemukset"]]]
   ])

(def profile
  [:div.profile
   [:div
    [:p "Testi Käyttäjä"]
    [:p "Stadin Aikuisopisto"]]
   [:div
    [:a {:href "#"} "Kirjaudu ulos"]]])

(def top-banner
  [c/h-box
   :class "top-banner"
   :width  "100%"
   :height "100px"
   :align :center
   :justify :center
   :children [[c/box
               :child logo
               :size "300px"]
              [c/box
               :child title
               :size "1"]
              [c/box
               :child profile
               :size "300px"]]])

