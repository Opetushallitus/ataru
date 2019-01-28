(ns ataru.virkailija.views.banner
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]])
  (:require [ataru.cljs-util :as util :refer [get-virkailija-translation]]
            [ataru.translations.texts :refer [virkailija-texts]]
            [ataru.virkailija.routes :as routes]
            [cljs.core.async :refer [<! timeout]]
            [cljs.core.match :refer-macros [match]]
            [clojure.string :as string]
            [goog.string :as s]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [taoensso.timbre :refer-macros [spy debug]]))

(def panels
  {:editor      {:text (:forms-panel virkailija-texts) :href "/lomake-editori/editor/"}
   :application {:text (:applications-panel virkailija-texts) :href "/lomake-editori/applications/"}})

(def right-labels {:form-edit (:form-edit-rights-panel virkailija-texts)
                   :view-applications (:view-applications-rights-panel virkailija-texts)
                   :edit-applications (:edit-applications-rights-panel virkailija-texts)})

(def active-section-arrow [:i.active-section-arrow.zmdi.zmdi-chevron-down.zmdi-hc-lg])

(defn section-link [panel-kw]
  (let [active-panel (subscribe [:active-panel])
        active?      (reaction (= @active-panel panel-kw))
        lang         (subscribe [:editor/virkailija-lang])]
    (fn []
      [:div.section-link {:class (name panel-kw)}
       [:a {:href (-> panels panel-kw :href)}
        (when @active?
          active-section-arrow)
        (get-in panels [panel-kw :text @lang])]])))

(defn title []
  (fn []
    [:div.title
     [section-link :editor]
     [:div.divider]
     [section-link :application]]))

(defn- get-label
  [label]
  (some #(-> label %) [:fi :sv :en]))

(defn create-org-labels [organizations lang]
  (map
    (fn [org]
      (str (get-label (:name org))
           (when (not-empty (:rights org))
             (str " (" (string/join ", " (map #(get-in right-labels [(keyword %) lang]) (:rights org))) ")"))))
   organizations))

(defn- org-label
  [organizations selected-organization]
  (let [org-count (count organizations)]
    (cond
      (some? selected-organization) (get-label (:name selected-organization))
      (zero? org-count) (get-virkailija-translation :no-organization)
      (< 1 org-count) (get-virkailija-translation :multiple-organizations)
      :else (-> organizations (first) :name (get-label)))))

(defn- organization-rights-select []
  (let [rights (subscribe [:state-query [:editor :user-info :selected-organization :rights]])]
    [:div.profile__organization-rights-selector
     (get-virkailija-translation :choose-user-rights)
     (doall
       (for [[right label] [["view-applications" (get-virkailija-translation :view-applications-rights-panel)]
                            ["edit-applications" (get-virkailija-translation :edit-applications-rights-panel)]
                            ["form-edit" (get-virkailija-translation :form-edit-rights-panel)]]]
         ^{:key (str "org-right-selector-for-" (name right))}
         [:label.profile__organization-select-right
          [:input
           {:type      "checkbox"
            :checked   (contains? (set @rights) right)
            :on-change #(dispatch [:editor/update-selected-organization-rights right (.. % -target -checked)])}]
          label]))]))

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
        lang                      (subscribe [:editor/virkailija-lang])
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
                    (create-org-labels (or selected-organization organizations) @lang)))
                (when selected-organization
                  [:div
                   (when (:superuser? @user-info)
                     [organization-rights-select])
                   [:a.profile__reset-to-default-organization
                    {:on-click #(dispatch [:editor/remove-selected-organization])}
                    (s/format "%s (%s)"
                              (get-virkailija-translation :reset-organization)
                              (org-label organizations nil))]])
                [:h4.profile__organization-select-title (get-virkailija-translation :change-organization)]
                [:input.editor-form__text-field.profile__organization-select-input
                 {:type        "text"
                  :placeholder (get-virkailija-translation :search-sub-organizations)
                  :value       @(subscribe [:state-query [:editor :organizations :query]])
                  :on-change   #(dispatch [:editor/update-organization-select-query (.-value (.-target %))])}]
                (into [:div.profile__organization-select-filters]
                      (apply concat [(organization-results-filter-checkbox :org-select-organizations "Organisaatiot")
                                     (organization-results-filter-checkbox :org-select-hakukohde-groups "Hakukohderyhmät")]))
                [:div.profile__organization-select-results-container
                 (into
                   [:ul.profile__organization-select-results.zmdi-hc-ul
                    (map
                      (fn [{:keys [oid name hakukohderyhma?]}]
                        [:li.profile__organization-select-result
                         {:key (str "organization-match-" oid)}
                         [:a
                          {:on-click #(dispatch [:editor/select-organization oid])}
                          (if hakukohderyhma?
                            [:i.zmdi.zmdi-hc-li.zmdi-collection-text]
                            [:i.zmdi.zmdi-hc-li.zmdi-accounts])
                          (let [label (get-label name)]
                            (if (clojure.string/blank? label)
                              "—"
                              label))]])
                      (take num-results-to-show @search-results))])
                 (when (<= (count @search-results) (inc num-results-to-show))
                   [:a.profile__organization-more-results
                    {:on-click #(dispatch [:editor/increase-organization-result-page])}
                    (get-virkailija-translation :more-results-refine-search)])]]])]])))))

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
   [:a {:href "/lomake-editori/auth/logout"} (get-virkailija-translation :logout)]])

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
