(ns lomake-editori.views.banner
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [re-com.core :as c]))

(def logo [:div.logo
           [:img {:src "/images/opintopolku_large-fi.png"
                  :height "40px"}]])

(def title [:div.title "LOMAKE-EDITORI | HAKEMUKSET"])

(def profile [:div.profile "Testi Käyttäjä"
              [:a {:href "#"} "Kirjaudu ulos"]])

(def top-banner
  [c/h-box
   :class "top-banner"
   :width  "100%"
   :height "100px"
   :align :center
   :children [[c/box
               :child logo
               :size "300px"]
              [c/box
               :child title
               :size "1"]
              [c/box
               :child profile
               :size "1"]]])

