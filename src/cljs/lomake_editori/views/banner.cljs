(ns lomake-editori.views.banner
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [re-com.core :as c]))

(def logo [:div "logo"])
(def title [:div "LOMAKE-EDITORI | HAKEMUKSET"])
(def profile [:div "Testi Käyttäjä"
              [:a {:href "#"} "Kirjaudu ulos"]])

(def top-banner
  [c/h-box
   :width  "100%"
   :height "100px"
   :align :center
   :children [[c/box
               :child logo
               :size "1"]
              [c/box
               :child title
               :size "1"]
              [c/box
               :child profile
               :size "1"]]])

