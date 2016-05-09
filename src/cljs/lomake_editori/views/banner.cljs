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

(def active-section-arrow [:span.active-section-arrow {:dangerouslySetInnerHTML {:__html "&#x2304;"}}])

(defn section-link [panel-kw]
  (let [active-panel         (subscribe [:active-panel])
        active?              (reaction (= @active-panel
                                          panel-kw))]
    (fn []
      [:div.section-link
       (if @active?
                [:span.active-section
                 active-section-arrow
                 (panel-kw panels)]
                [:a {:href (str "#/" (name panel-kw))}
                 (panel-kw panels)])])))

(defn title []
  (fn []
    [:div.title
     [section-link :editor]
     ;;
     ;; Hidden until the tab contains something real
     ;;
     ;;      [:div.divider]
     ;;      [section-link :application]
     ;;      [:div [applications]]
     ]))

(defn profile []
  (let [username         (subscribe [:state-query [:editor :user-info :username]])]
    (fn []
      (when @username
        [:div.profile
         [:div
          [:p @username]
          ;; Hidden until we get the relevant organization
          ;; [:p "Stadin Aikuisopisto"]
          ]
         [:div.divider]
         [:div
          [:a {:href "#"} "Kirjaudu ulos"]]]))))

(defn status []
  (let [flasher          (subscribe [:state-query [:flasher]])
        loading?         (subscribe [:state-query [:flasher :loading?]])]
    (fn []
      [:div
       (when @flasher
         (match [@loading? @flasher]
                [false {:detail detailed-error
                        :message message}]
                [:div.flasher {:style {"color" "crimson"}}
                 [:span message]]

                [true {:message message}]
                [:div.flasher
                 [:span.animated.fadeOut message]]

                [false {:message nil}]
                [:div]

                [false {:message message}]
                [:div.flasher
                 [:span.animated.fadeIn message]]))])))

(defn top-banner []
  [:div.top-banner [:div.tabs logo [title]] [status] [profile]])
