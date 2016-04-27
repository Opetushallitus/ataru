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
    [:img {:src "images/opintopolku_large-fi.png"
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
      [:div.section-link
       (if @active?
                [:span.active-section (panel-kw panels)]
                [:a {:href (str "#/" (name panel-kw))}
                 (panel-kw panels)])])))

(defn title []
  (fn []
    [:div.title
     [section-link :editor]
      [:div.divider]
      [section-link :application]
      [:div [applications]]]))

(def profile
  [:div.profile
   [:div
    [:p "Testi Käyttäjä"]
    [:p "Stadin Aikuisopisto"]]
   [:div
    [:a {:href "#"} "Kirjaudu ulos"]]])

(defn top-banner []
  [:div.top-banner logo [title] profile])
