(ns ataru.virkailija.views.banner
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
  (let [application-count (subscribe [:state-query [:application :count]])]
    [:div.applications-count.display-flex
     [:span @application-count]]))

(def panels
  {:editor      {:text "Lomake-editori" :href #(str "#/editor/" %)}
   :application {:text "Hakemukset" :href #(str "#/application/" %)}})

(def active-section-arrow [:span.active-section-arrow {:dangerouslySetInnerHTML {:__html "&#x2304;"}}])

(defn section-link [panel-kw]
  (let [active-panel     (subscribe [:active-panel])
        active?          (reaction (= @active-panel panel-kw))
        selected-form-id (subscribe [:state-query [:editor :selected-form-id]])]
    (fn []
      [:div.section-link
       (if @active?
         [:span.active-section
          active-section-arrow
          (-> panels panel-kw :text)]
         [:a {:href (str ((-> panels panel-kw :href ) @selected-form-id))}
          (-> panels panel-kw :text)])])))

(defn title []
  (fn []
    [:div.title
     [section-link :editor "#/editor/"]
     [:div.divider]
     [section-link :application "#/application/"]
     [:div [applications]]
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
          [:a {:href "/lomake-editori/auth/logout"} "Kirjaudu ulos"]]]))))

(defn status []
  (let [flasher  (subscribe [:state-query [:flasher]])
        loading? (subscribe [:state-query [:flasher :loading?]])]
    (fn []
      [:div
       (when @flasher
         (match [@loading? @flasher]
                [false {:error-type :concurrent-edit
                        :message message}]
                [:div.flasher {:class "concurrent-edit-error animated flash"}
                 [:span message]]
                [false {:detail detailed-error
                        :message message}]
                [:div.flasher {:style {"color" "crimson"}}
                 [:span message]]

                [true {:message nil}]
                [:div]

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
