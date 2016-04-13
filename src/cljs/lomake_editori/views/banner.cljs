(ns lomake-editori.views.banner
  (:require-macros
            [reagent.ratom :refer [reaction]])
  (:require [cljs.core.match :refer-macros [match]]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [re-com.core :as c]))

(def logo
  [:div.logo
    [:img {:src "/images/opintopolku_large-fi.png"
           :height "40px"}]])

(defn applications []
  (let [applications (subscribe [:state-query [:application :applications]])
        count-applications (reaction (count @applications))]
    (fn []
      [:div.applications-count.display-flex
       [:span @count-applications]])))

(def panels
  {:editor "Lomake-editori"
   :application "Hakemukset"})

(def invert-panels (clojureset/map-invert panels))

(defn section-link [name]
  (let [active-panel         (subscribe [:active-panel])
        name-of-active-panel (reaction (get invert-panels @active-panel))
        active? (reaction (= @name-of-active-panel name))]
    (fn []
      [c/box
       :child (if @active?
                [:span.active-section name]
                [:a {:on-click #(dispatch [:set-active-panel (get invert-panels name)])} name])])))

(defn title []
  (let [panels (subscribe [:panels])]
    (fn []
      [c/h-box
       :class "title"
       :width "100%"
       :align :center
       :gap "1em"
       :children
       [[section-link "Lomake-editori"]
        [c/box
         :child ""
         :class "divider"
         :size "1px"]
        [section-link "Hakemukset"]
        [c/box
         :child [applications]]]])))

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
               :child [title]
               :size "1"]
              [c/box
               :child profile
               :size "300px"]]])

