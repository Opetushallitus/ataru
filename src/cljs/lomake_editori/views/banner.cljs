(ns lomake-editori.views.banner
  (:require-macros
            [reagent.ratom :refer [reaction]]
            [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.match :refer-macros [match]]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [cljs.core.async :as a :refer  [<! timeout]]
            [taoensso.timbre :refer-macros [spy debug]]))

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

(defn- icon [icon & classes]
  (fn [icon]
    [:i.zmdi.zmdi-hc-2x {:class (str "zmdi-" icon " "
                                     (apply str (interpose " " classes)))}]))

(defn status []
  (let [flasher          (subscribe [:state-query [:flasher]])
        loading?         (subscribe [:state-query [:flasher :loading?]])]
    (fn []
      [:div.flasher
       (when @flasher
         (match [@loading? @flasher]
                [false {:detail detailed-error
                        :message message}]
                [:div {:style {"color" "crimson"}}
                 [icon "alert-triangle" "animated pulse infinite"]
                 [:span message]]

                [true {:message message}]
                [:div
                 [icon "hc-spin" "zmdi-rotate-right"]
                 [:span.animated.fadeOut message]]

                [false {:message message}]
                [:div
                 [icon "flower-alt" "animated" "fadeIn"]
                 [:span.animated.fadeIn message]]))])))

(defn top-banner []
  [:div.top-banner [:div.tabs logo [title] [status]] profile])
