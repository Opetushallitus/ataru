(ns ataru.virkailija.views.banner
  (:require-macros
            [reagent.ratom :refer [reaction]]
            [cljs.core.async.macros :refer [go]])
  (:require [ataru.virkailija.routes :as routes]
            [cljs.core.match :refer-macros [match]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [cljs.core.async :refer  [<! timeout]]
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
       [:a {:href (-> panels panel-kw :href)}
        (when @active?
          active-section-arrow)
        (-> panels panel-kw :text)]])))

(defn title []
  (fn []
    [:div.title
     [section-link :editor]
     [:div.divider]
     [section-link :application]]))

(defn- get-label
  [label]
  (some #(-> label %) [:fi :sv :en]))

(defn create-org-labels [organizations]
  (map
    (fn [org]
      (str (get-label (:name org))
           (when (not-empty (:rights org))
             (str " (" (string/join ", " (map #(get right-labels (keyword %)) (:rights org))) ")"))))
   organizations))

(defn- org-label
  [organizations selected-organization]
  (let [org-count (count organizations)]
    (cond
      (some? selected-organization) (get-label (:name selected-organization))
      (zero? org-count) "Ei organisaatiota"
      (< 1 org-count) "Useita organisaatioita"
      :else (-> organizations (first) :name (get-label)))))

(defn- organization-rights-select []
  (let [rights (subscribe [:state-query [:editor :organizations :rights]])]
    [:div.profile__organization-rights-selector
     "Valitse käyttäjän oikeudet"
     (doall
            (for [[right label] [[:view-applications "Hakemusten katselu"]
                                 [:edit-applications "Hakemusten muokkaus"]
                                 [:form-edit "Lomakkeiden muokkaus"]]]
              ^{:key (str "org-right-selector-for-" (name right))}
              [:label.profile__organization-select-right
               [:input
                {:type      "checkbox"
                 :checked   (contains? (set @rights) right)
                 :on-change #(dispatch [:editor/update-selected-organization-rights right (.. % -target -checked)])}]
               label]))]))

(defn profile []
  (let [user-info             (subscribe [:state-query [:editor :user-info]])
        org-select-visible?   (reagent/atom false)]
    (fn []
      (when @user-info
        (let [organizations             (:organizations @user-info)
              search-results            (subscribe [:state-query [:editor :organizations :matches]])
              selected-organization-sub (subscribe [:state-query [:editor :user-info :selected-organization]])
              selected-organization     (when @selected-organization-sub [@selected-organization-sub])
              org-str                   (org-label organizations (first selected-organization))]
          [:div.profile
           [:div.profile__organization
            [:a.profile__organization-link
             {:on-click #(swap! org-select-visible? not)}
             [:i.profile__organization-link-icon.zmdi.zmdi-accounts.zmdi-hc-2x]
             [:div.profile__organization-link-name org-str]]
            (when @org-select-visible?
              [:div.profile__organization-select
               [:div.profile__organization-select-username-container
                [:i.profile__organization-select-username-icon.zmdi.zmdi-account.zmdi-hc-lg]
                [:span.profile__organization-select-username-name (str (:name @user-info) " (" (:username @user-info) ")")]]
               (into
                 [:ul.profile__organization-select-user-orgs.zmdi-hc-ul]
                 (map
                   (fn [org] [:li [:i.zmdi.zmdi-hc-li.zmdi-accounts] org])
                   (create-org-labels (or selected-organization organizations))))
               (when selected-organization
                 [:div
                  (when (:superuser? @user-info)
                    [organization-rights-select])
                  [:a.profile__reset-to-default-organization
                   {:on-click #(dispatch [:editor/remove-selected-organization])}
                   (str "Palauta oletusorganisaatio (" (org-label organizations nil) ")")]])
               [:h4.profile__organization-select-title "Vaihda organisaatio"]
               [:input.editor-form__text-field.profile__organization-select-input
                {:type        "text"
                 :placeholder "Etsi aliorganisaatioita"
                 :value       @(subscribe [:state-query [:editor :organizations :query]])
                 :on-change   #(dispatch [:editor/update-organization-select-query (.-value (.-target %))])}]
               (into
                 [:ul.profile__organization-select-results.zmdi-hc-ul
                  (map
                    (fn [{:keys [oid name]}]
                      [:li.profile__organization-select-result
                       {:key (str "organization-match-" oid)}
                       [:a
                        {:on-click #(dispatch [:editor/select-organization oid])}
                        [:i.zmdi.zmdi-hc-li.zmdi-accounts]
                        (get-label name)]])
                    @search-results)])
               (when (< 10 (count @search-results))
                 [:div.profile__organization-more-results "Lisää tuloksia, tarkenna hakua"])])]])))))

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

(defn snackbar []
  (if-let [snackbar-messages @(subscribe [:snackbar-message])]
    [:div.snackbar
      (let [status (first snackbar-messages)
            message (second snackbar-messages)]
        [:span.snackbar__content
         [:div.snackbar__content-status status]
         [:div.snackbar__content-paragraph message]])]))

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
