(ns lomake-editori.views.banner
  (:require-macros
            [reagent.ratom :refer [reaction]])
  (:require [cljs.core.match :refer-macros [match]]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [re-com.core :as c]
            [taoensso.timbre :refer-macros [spy]]))

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

(defn section-link [panel-kw]
  (let [active-panel         (subscribe [:active-panel])
        active?              (reaction (= @active-panel
                                          panel-kw))]
    (fn []
      [c/box
       :child (if @active?
                [:span.active-section (panel-kw panels)]
                [:a {:on-click #(dispatch [:set-active-panel panel-kw])}
                 (panel-kw panels)])])))

(defn title []
  (fn []
    [c/h-box
     :class "title"
     :width "100%"
     :align :center
     :gap "1em"
     :children
     [[section-link :editor]
      [c/box
       :child ""
       :class "divider"
       :size "1px"]
      [section-link :application]
      [c/box
       :child [applications]]]]))

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

