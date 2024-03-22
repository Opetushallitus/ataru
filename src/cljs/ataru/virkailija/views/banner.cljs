(ns ataru.virkailija.views.banner
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [cljs.core.match :refer-macros [match]]
            [clojure.string :as string]
            [goog.string :as s]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]))

(def panels
  {:editor      {:text :forms-panel :href "/lomake-editori/editor/"}
   :application {:text :applications-panel :href "/lomake-editori/applications/"}})

(def right-labels {:form-edit         :form-edit-rights-panel
                   :view-applications :view-applications-rights-panel
                   :edit-applications :edit-applications-rights-panel
                   :view-valinta      :view-valinta-rights-panel
                   :edit-valinta      :edit-valinta-rights-panel
                   :opinto-ohjaaja    :opinto-ohjaaja
                   :valinnat-valilehti :valinnat-valilehti })

(def active-section-arrow [:i.active-section-arrow.zmdi.zmdi-chevron-down.zmdi-hc-lg])

(defn section-link [panel-kw]
  (let [active-panel (subscribe [:active-panel])
        active?      (reaction (= @active-panel panel-kw))]
    (fn []
      [:div.section-link {:class (name panel-kw)}
       [:a {:href (-> panels panel-kw :href)}
        (when @active?
          active-section-arrow)
        @(subscribe [:editor/virkailija-translation (get-in panels [panel-kw :text])])]])))

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
             (str " ("
                  (string/join ", " (map (fn [r] @(subscribe [:editor/virkailija-translation ((keyword r) right-labels)]))
                                         (:rights org)))
                  ")"))))
   organizations))

(defn- org-label
  [organizations selected-organization]
  (let [org-count (count organizations)]
    (cond
      (some? selected-organization) (get-label (:name selected-organization))
      (zero? org-count) @(subscribe [:editor/virkailija-translation :no-organization])
      (< 1 org-count) @(subscribe [:editor/virkailija-translation :multiple-organizations])
      :else (-> organizations (first) :name (get-label)))))

(defn- organization-rights-select []
  (let [rights (set @(subscribe [:state-query [:editor :user-info :selected-organization :rights]]))]
    [:div.profile__organization-rights-selector
     @(subscribe [:editor/virkailija-translation :choose-user-rights])
     (doall
      (for [[right label] right-labels]
        ^{:key (str "org-right-selector-for-" (name right))}
        [:label.profile__organization-select-right
         [:input
          {:type      "checkbox"
           :checked   (contains? rights (name right))
           :on-change #(dispatch [:editor/update-selected-organization-rights right (.. % -target -checked)])}]
         @(subscribe [:editor/virkailija-translation label])]))]))

(defn- organization-results-filter-checkbox
  [id label]
  [[:input.profile__organization-select-filters-checkbox
    {:type      "checkbox"
     :id        (name id)
     :checked   @(subscribe [:state-query [:editor :organizations id]])
     :on-change #(dispatch [:editor/toggle-organization-select-filter id])}]
   [:label.profile__organization-select-filters-checkbox-label
    {:for (name id)}
    label]])

(defn profile []
  (let [user-info                 (subscribe [:state-query [:editor :user-info]])
        org-select-visible?       (reagent/atom false)
        results-page              (subscribe [:state-query [:editor :organizations :results-page]])
        search-results            (subscribe [:state-query [:editor :organizations :matches]])
        selected-organization-sub (subscribe [:state-query [:editor :user-info :selected-organization]])
        page-size                 20]
    (fn []
      (when @user-info
        (let [organizations         (:organizations @user-info)
              selected-organization (when @selected-organization-sub [@selected-organization-sub])
              org-str               (org-label organizations (first selected-organization))
              num-results-to-show   (* page-size (inc @results-page))]
          [:div.profile
           [:div.profile__organization
            [:a.profile__organization-link
             {:on-click #(swap! org-select-visible? not)}
             [:i.profile__organization-link-icon.zmdi.zmdi-accounts.zmdi-hc-2x]
             [:div.profile__organization-link-name org-str]]
            (when @org-select-visible?
              [:div.profile__organization-select
               [:div.profile__organization-select-header
                [:div.profile__organization-select-username-container
                 [:i.profile__organization-select-username-icon.zmdi.zmdi-account.zmdi-hc-lg]
                 [:span.profile__organization-select-username-name (str (:name @user-info) " (" (:oid @user-info) ")")]]
                [:button.virkailija-close-button
                 {:on-click #(swap! org-select-visible? not)}
                 [:i.zmdi.zmdi-close]]]
               [:div.profile__organization-select-content
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
                    (s/format "%s (%s)"
                              @(subscribe [:editor/virkailija-translation :reset-organization])
                              (org-label organizations nil))]])
                [:h4.profile__organization-select-title @(subscribe [:editor/virkailija-translation :change-organization])]
                [:input.editor-form__text-field.profile__organization-select-input
                 {:type        "text"
                  :placeholder @(subscribe [:editor/virkailija-translation :search-sub-organizations])
                  :value       @(subscribe [:state-query [:editor :organizations :query]])
                  :on-change   #(dispatch [:editor/update-organization-select-query (.-value (.-target %))])}]
                (into [:div.profile__organization-select-filters]
                      (apply concat [(organization-results-filter-checkbox :org-select-organizations "Organisaatiot")
                                     (organization-results-filter-checkbox :org-select-hakukohde-groups "Hakukohderyhmät")]))
                [:div.profile__organization-select-results-container
                 (into
                   [:ul.profile__organization-select-results.zmdi-hc-ul
                    (map
                      (fn [{:keys [oid name hakukohderyhma? active?]}]
                        [:li.profile__organization-select-result
                         {:key (str "organization-match-" oid)}
                         [:a
                          {:on-click #(dispatch [:editor/select-organization oid])
                           :title oid}
                          (if hakukohderyhma?
                            [:i.zmdi.zmdi-hc-li.zmdi-collection-text]
                            [:i.zmdi.zmdi-hc-li.zmdi-accounts])
                          (let [label (get-label name)]
                            (if (clojure.string/blank? label)
                              "—"
                              label))]
                         [:a.profile__organization-select-results__link
                          {:href (str "/organisaatio-service/lomake/" oid)
                           :target "blank"}
                          [:i.zmdi.zmdi-open-in-new]]
                         (when (and (not active?) (not (nil? active?)))
                           [:i.material-icons-outlined.arkistoitu
                             "archive"])])
                      (take num-results-to-show @search-results))])
                 (when (= (inc num-results-to-show) (count @search-results))
                   [:a.profile__organization-more-results
                    {:on-click #(dispatch [:editor/increase-organization-result-page])}
                    @(subscribe [:editor/virkailija-translation :more-results-refine-search])])]]])]])))))

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

                [false _ {:message message}]
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
   [:a {:href "/lomake-editori/auth/logout"} @(subscribe [:editor/virkailija-translation :logout])]])

(defn snackbar []
  (when-let [snackbar-messages @(subscribe [:snackbar-message])]
    [:div.snackbar
      (let [status (first snackbar-messages)
            message (second snackbar-messages)]
        [:span.snackbar__content
         [:div.snackbar__content-status status]
         [:div.snackbar__content-paragraph message]])]))

(defn- toast [message id]
  [:div.toaster__content
   [:i.material-icons-outlined.toaster-notification
    {:title @(subscribe [:editor/virkailija-translation :archived])}
    "report"]
   [:div.toaster__content-paragraph message]
   [:div.toaster__close {:on-click #(dispatch [:delete-toast-message id])} [:i.material-icons-outlined.toaster-close
                                                                            {:title @(subscribe [:editor/virkailija-translation :archived])}
                                                                            "close"]]])

(defn toaster []
  (let [toast-messages (not-empty @(subscribe [:toast-messages]))
        to-show (take 5 (sort (fn [a b] (> (:id a) (:id b))) toast-messages))]
    (when (not-empty to-show)
      [:div.toaster
       [:div.toaster__wrapper
        (map (fn [t] ^{:key (:id t)} [toast  (:message t) (:id t)]) to-show)]])))

(defn top-banner []
  (let [banner-type (subscribe [:state-query [:banner :type]])]
    [:div
     [:div.top-banner {:class (case @banner-type
                                :fixed   "fixed-top-banner"
                                :in-flow "in-flow-top-banner"
                                "fixed-top-banner")}
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
