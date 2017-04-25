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

(def panels
  {:editor      {:text "Lomakkeet" :href "/lomake-editori/editor/"}
   :application {:text "Hakemukset" :href "/lomake-editori/applications/"}})

(def right-labels {:form-edit "Lomakkeen muokkaus"
                   :view-applications "Hakemusten katselu"
                   :edit-applications "Hakemusten arviointi"})

(def active-section-arrow [:i.active-section-arrow.zmdi.zmdi-chevron-down.zmdi-hc-lg])

(defn section-link [panel-kw]
  (let [active-panel     (subscribe [:active-panel])
        active?          (reaction (= @active-panel panel-kw))]
    (fn []
      [:div.section-link {:class (name panel-kw)}
       (if @active?
         [:span.active-section
          active-section-arrow
          (-> panels panel-kw :text)]
         [:a {:on-click (partial routes/navigate-to-click-handler (str (-> panels panel-kw :href)))}
          (-> panels panel-kw :text)])])))

(defn title []
  (fn []
    [:div.title
     [section-link :editor]
     [:div.divider]
     [section-link :application]]))

(defn create-org-labels [organizations]
  (map
   (fn [org]
     (str (get-in org [:name :fi]) " (" (string/join ", " (map #(get right-labels (keyword %)) (:rights org))) ")"))
   organizations))

(defn profile []
  (let [user-info (subscribe [:state-query [:editor :user-info]])]
    (fn []
      (when @user-info
        (let [org-count      (count (:organizations @user-info))
              org-labels     (create-org-labels (:organizations @user-info))
              joint-orgs-str (string/join " \n" org-labels)
              tooltip-str    (str joint-orgs-str "\n\nKäyttäjätunnus: " (:username @user-info))
              org-str        (cond
                               (= 0 org-count) "Ei organisaatiota"
                               (< 1 org-count) "Useita organisaatioita"
                               :else           (get-in (first (:organizations @user-info)) [:name :fi]))]
          [:div.profile
           [:div
            [:p.tooltip-indicator {:title tooltip-str} org-str]]])))))

(defn status []
  (let [flash    (subscribe [:state-query [:flash]])
        loading? (subscribe [:state-query [:flash :loading?]])
        expired? (subscribe [:state-query [:flash :expired?]])]
    (fn []
      [:div.flasher-container
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
                nil))])))

(defn local-dev-logout-enabled? []
  (boolean (-> js/config
               js->clj
               (get "local-dev-logout"))))

(defn local-dev-logout []
  [:div.local-dev-logout
   [:a {:href "/lomake-editori/auth/logout"} "Kirjaudu ulos"]])

(defn top-banner []
  (let [banner-type (subscribe [:state-query [:banner :type]])]
    [:div
     [:div.top-banner {:class (case @banner-type
                                :fixed   "fixed-top-banner"
                                :in-flow "in-flow-top-banner")}
      [profile]
      [:div.tabs [title]]
      [status]]
     ;; When using static positioning, push the actual content of
     ;; lomake-editori tabs downwards as much as the height of the banner is
     ;; with this invisible placeholder
     (when (= @banner-type :fixed)
       [:div.fixed-top-banner-placeholder])

     (when (local-dev-logout-enabled?) [local-dev-logout])]))

(defn create-banner-position-handler []
  (let [raamit-visible (atom true)]
    (fn [_]
      (when-let [raami-element (aget (.getElementsByClassName js/document "virkailija-raamit") 0)]
        (if (<= (-> raami-element .getBoundingClientRect .-bottom) 0)
          (when @raamit-visible
            (dispatch [:state-update #(assoc-in % [:banner :type] :fixed)])
            (reset! raamit-visible false))
          (when-not @raamit-visible
            (dispatch [:state-update #(assoc-in % [:banner :type] :in-flow)])
            (reset! raamit-visible true)))))))

(set! (.-onscroll js/window) (create-banner-position-handler))
