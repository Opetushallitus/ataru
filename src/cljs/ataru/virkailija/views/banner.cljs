(ns ataru.virkailija.views.banner
  (:require-macros
            [reagent.ratom :refer [reaction]]
            [cljs.core.async.macros :refer [go]])
  (:require [ataru.virkailija.routes :as routes]
            [cljs.core.match :refer-macros [match]]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [cljs.core.async :as a :refer  [<! timeout]]
            [taoensso.timbre :refer-macros [spy debug]]
            [clojure.string :as string]))

(def logo
  [:div.logo
    [:img {:src "/lomake-editori/images/opintopolku_large-fi.png"
           :height "40px"}]])

(def panels
  {:editor      {:text "Lomakkeet" :href #(str "/lomake-editori/editor/" %)}
   :application {:text "Hakemukset" :href #(str "/lomake-editori/applications/" %)}})

(def active-section-arrow [:span.active-section-arrow {:dangerouslySetInnerHTML {:__html "&#x2304;"}}])

(defn section-link [panel-kw]
  (let [active-panel     (subscribe [:active-panel])
        active?          (reaction (= @active-panel panel-kw))
        selected-form-key (subscribe [:state-query [:editor :selected-form-key]])]
    (fn []
      [:div.section-link {:class (name panel-kw)}
       (if @active?
         [:span.active-section
          active-section-arrow
          (-> panels panel-kw :text)]
         [:a {:href (str ((-> panels panel-kw :href ) @selected-form-key))
              :on-click routes/anchor-click-handler}
          (-> panels panel-kw :text)])])))

(defn title []
  (fn []
    [:div.title
     [section-link :editor]
     [:div.divider]
     [section-link :application]]))

(defn profile []
  (let [user-info (subscribe [:state-query [:editor :user-info]])]
    (fn []
      (when @user-info
        (let [org-count      (count (:organizations @user-info))
              org-names      (map #(get-in % [:name :fi]) (:organizations @user-info))
              joint-orgs-str (string/join "; " org-names)
              org-str        (cond
                               (= 0 org-count) "Ei organisaatiota"
                               (< 1 org-count) "Useita organisaatioita"
                               :else           (first org-names))
              tooltip        (if (< 1 org-count) joint-orgs-str "")
              tooltip-class  (if (< 1 org-count) "tooltip-indicator" "")]
          [:div.profile
           [:div
            [:p (:username @user-info)]
            [:p {:title tooltip :class tooltip-class} org-str]]
           [:div.divider]
           [:div
            [:a {:href "/lomake-editori/auth/logout"} "Kirjaudu ulos"]]])))))

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
                [:div.flasher
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
