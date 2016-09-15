(ns ataru.virkailija.views.banner
  (:require-macros
            [reagent.ratom :refer [reaction]]
            [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.match :refer-macros [match]]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [cljs.core.async :as a :refer  [<! timeout]]
            [taoensso.timbre :refer-macros [spy debug]]
            [clojure.string :as string]))

(def logo
  [:div.logo
    [:img {:src "images/opintopolku_large-fi.png"
           :height "40px"}]])

(def panels
  {:editor      {:text "Lomake-editori" :href #(str "#/editor/" %)}
   :application {:text "Hakemukset" :href #(str "#/applications/" %)}})

(def active-section-arrow [:span.active-section-arrow {:dangerouslySetInnerHTML {:__html "&#x2304;"}}])

(defn section-link [panel-kw]
  (let [active-panel     (subscribe [:active-panel])
        active?          (reaction (= @active-panel panel-kw))
        selected-form-id (subscribe [:state-query [:editor :selected-form-id]])]
    (fn []
      [:div.section-link {:class (name panel-kw)}
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
     [section-link :application "#/applications/"]]))

(defn profile []
  (let [user-info         (subscribe [:state-query [:editor :user-info]])]
    (fn []
      (when @user-info
        [:div.profile
         [:div
          [:p (:username @user-info)]
          [:p (string/join ", " (map :fi (:organization-names @user-info)))]
          ]
         [:div.divider]
         [:div
          [:a {:href "/lomake-editori/auth/logout"} "Kirjaudu ulos"]]]))))

(defn status []
  (let [flash    (subscribe [:state-query [:flash]])
        loading? (subscribe [:state-query [:flash :loading?]])
        expired? (subscribe [:state-query [:flash :expired?]])]
    (fn []
      [:div
       (when @flash
         (match [@loading? @expired? @flash]
                [false _ {:error-type :concurrent-edit
                        :message message}]
                [:div.flasher.concurrent-edit-error.animated.flash
                 [:span message]]

                [false _ {:detail detailed-error
                        :message message}]
                [:div.flasher {:style {"color" "crimson"}}
                 [:span message]]

                [_ false {:message (message :guard some?)}]
                [:div.flasher
                 [:span message]]

                [_ true {:message (message :guard some?)}]
                [:div.flasher.animated.fadeOut
                 [:span message]]

                :else
                [:div]))])))

(defn top-banner []
  [:div.top-banner [:div.tabs logo [title]] [status] [profile]])
