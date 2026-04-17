(ns ataru.virkailija.application.view.application-heading
  (:require [re-frame.core :refer [subscribe dispatch]]
            [ataru.cljs-util :as cljs-util]
            [ataru.util :as util]
            [ataru.virkailija.application.view.virkailija-application-names :as names]))

(defn- select-application
  ([application-key selected-hakukohde-oid]
   (select-application application-key selected-hakukohde-oid nil))
  ([application-key selected-hakukohde-oid with-newest-form?]
   (cljs-util/update-url-with-query-params {:application-key application-key})
   (dispatch [:application/select-application application-key selected-hakukohde-oid with-newest-form?])))

(defn- notification [_]
  (fn [{:keys [text link-text href on-click]}]
    [:div.application__message-display--details-notification
     {:id (str "notification-label-" (name text))}
     @(subscribe [:editor/virkailija-translation text])
     [:a.application-handling__form-outdated--button.application-handling__button
      {:id (str "notification-link-" (name text))
       :href     href
       :target   "_blank"
       :on-click on-click}
      [:span @(subscribe [:editor/virkailija-translation link-text])]]]))

(defn- notifications-display []
  (fn []
    (let [application                   @(subscribe [:application/selected-application])
          toisen-asteen-yhteishaku?     @(subscribe [:application/toisen-asteen-yhteishaku-oid? (:haku application)])
          admin?                        @(subscribe [:editor/superuser?])
          person-oid                    (-> application :person :oid)
          selected-review-hakukohde     @(subscribe [:state-query [:application :selected-review-hakukohde-oids]])
          show-not-latest-form?         (some? @(subscribe [:state-query [:application :latest-form]]))
          show-creating-henkilo-failed? @(subscribe [:application/show-creating-henkilo-failed?])
          show-tutkinto-fetch-failed?   @(subscribe [:application/show-tutkinto-fetch-failed?])
          show-henkilo-info-incomplete? (and (some? person-oid)
                                             (not (-> application :person :language)))
          show-not-yksiloity?           (and (some? person-oid)
                                             (not (-> application :person :yksiloity))
                                             (or (not toisen-asteen-yhteishaku?)
                                                 admin?))
          show-metadata-not-found?      @(subscribe [:state-query [:application :metadata-not-found]])]
      (when (or show-not-latest-form?
                show-creating-henkilo-failed?
                show-henkilo-info-incomplete?
                show-not-yksiloity?
                show-metadata-not-found?)
        [:div.application__message-display.application__message-display--notification
         {:id "notifications-display"}
         [:div.application__message-display--exclamation [:i.zmdi.zmdi-alert-triangle]]
         [:div.application__message-display--details
          (when show-not-latest-form?
            [notification {:text      :form-outdated
                           :link-text :show-newest-version
                           :on-click  (fn [evt]
                                        (.preventDefault evt)
                                        (select-application (:key application) selected-review-hakukohde true))}])
          (when show-creating-henkilo-failed?  ; henkilo details are missing entirely
            [notification {:text :creating-henkilo-failed}])
          (when show-tutkinto-fetch-failed?
            [notification {:text      :koski-tutkinto-fetch-failed}])
          (when show-henkilo-info-incomplete?  ; henkilo is missing some essential information, such as language
            [notification {:text      :henkilo-info-incomplete
                           :link-text :review-in-henkilopalvelu
                           :href      (str "/henkilo-ui/oppija/"
                                           person-oid)}])
          (when show-not-yksiloity?
            [notification {:text      :person-not-individualized
                           :link-text :individualize-in-henkilopalvelu
                           :href      (str "/henkilo-ui/oppija/"
                                           person-oid
                                           "/duplikaatit?permissionCheckService=ATARU")}])
          (when show-metadata-not-found?
            [notification {:text :metadata-not-found}])]]))))

(defn- close-application []
  [:a {:href     "#"
       :on-click (fn [event]
                   (.preventDefault event)
                   (dispatch [:application/close-application]))}
   [:div.close-details-button
    [:i.zmdi.zmdi-close.close-details-button-mark]]])

(defn application-heading []
  (let [loading?            @(subscribe [:state-query [:application :loading?]])
        application         @(subscribe [:state-query [:application :selected-application-and-form :application]])
        master-oid          (-> application :person :master-oid)
        answers             (:answers application)
        pref-name           (-> application :person :preferred-name)
        last-name           (-> application :person :last-name)
        ssn                 (-> application :person :ssn)
        birth-date          (-> application :person :birth-date)
        person-oid          (-> application :person :oid)
        oppilaitos-name     (-> application :person :oppilaitos-name)
        luokka              (-> application :person :luokka)
        email               (get-in answers [:email :value])
        applications-count  (:applications-count application)
        hakemus-oid         (:key application)
        haku-oid            (:haku application)
        lang                (subscribe [:editor/virkailija-lang])
        suorituspalvelu-user? @(subscribe [:editor/suorituspalvelu-user?])]
    [:<>
     [close-application]
     [:div.application__handling-heading
      [:div.application-handling__review-area-main-heading-container
       (when-not loading?
         [:div.application-handling__review-area-main-heading-person-info
          [:div.application-handling__review-area-main-heading-name-row
           (when pref-name
             [:h2.application-handling__review-area-main-heading
              (str last-name ", " pref-name " — " (or ssn birth-date))])]
          (when oppilaitos-name
            [:div.application-handling__review-area-main-heading-oppilaitos-name-row
             [:span (str (util/from-multi-lang oppilaitos-name @lang) ", " luokka)]])
          [:div.application-handling__review-area-main-heading-application-oid-row
           [:span (str @(subscribe [:editor/virkailija-translation :application-number]) " " hakemus-oid)]]
          [:div.application-handling__review-area-main-heading-person-oid-row
           [:div.application-handling__applicant-links
            (when master-oid
              [:a
               {:href   (str "/henkilo-ui/oppija/"
                             master-oid
                             "?permissionCheckService=ATARU")
                :target "_blank"}
               [:i.zmdi.zmdi-account-circle.application-handling__review-area-main-heading-person-icon]
               [:span.application-handling__review-area-main-heading-person-oid
                (str @(subscribe [:editor/virkailija-translation :student-number]) " " master-oid)]])
            (when person-oid
              [:a
               {:href   (str "/henkilo-ui/oppija/"
                             person-oid
                             "?permissionCheckService=ATARU")
                :target "_blank"}
               [:i.zmdi.zmdi-account-circle.application-handling__review-area-main-heading-person-icon]
               [:span.application-handling__review-area-main-heading-person-oid
                (str @(subscribe [:editor/virkailija-translation :person-oid]) " " person-oid)]])
            (when person-oid
              [:div
               [:a
                {:href   (str "/suoritusrekisteri/#/opiskelijat?henkilo=" person-oid)
                 :target "_blank"}
                [:i.zmdi.zmdi-collection-text.application-handling__review-area-main-heading-person-icon]
                [:span.application-handling__review-area-main-heading-person-oid
                 @(subscribe [:editor/virkailija-translation :person-completed-education])]]
               (when suorituspalvelu-user?
                 [:a.application-handling__review-area-main-heading-applications-right-link
                  {:href   (str "/suorituspalvelu/redirect/" person-oid)
                   :target "_blank"}
                  [:i.zmdi.zmdi-collection-text.application-handling__review-area-main-heading-person-icon]
                  [:span.application-handling__review-area-main-heading-person-oid
                   @(subscribe [:editor/virkailija-translation :person-completed-education-suorituspalvelu])]])])
            (when (> applications-count 1)
              [:a.application-handling__review-area-main-heading-applications-link
               {:on-click (fn [_]
                            (dispatch [:application/navigate
                                       (str "/lomake-editori/applications/search"
                                            "?term=" (or ssn email))]))}
               [:i.zmdi.zmdi-collection-text.application-handling__review-area-main-heading-person-icon]
               [:span.application-handling__review-area-main-heading-person-oid
                (str @(subscribe [:editor/virkailija-translation :view-applications]) " (" applications-count ")")]])
            (when
              (and hakemus-oid
                   haku-oid)
              [:div
               [:a
                {:href   (.url js/window
                               "valintalaskenta-ui.valintojen-toteuttaminen.hakemus"
                               haku-oid
                               hakemus-oid
                               hakemus-oid
                               hakemus-oid)
                 :target "_blank"}
                [:i.zmdi.zmdi-collection-text.application-handling__review-area-main-heading-person-icon]
                [:span.application-handling__review-area-main-heading-person-oid
                 (str @(subscribe [:editor/virkailija-translation :valintojen-toteuttaminen]))]]
               [:a.application-handling__review-area-main-heading-applications-right-link
                {:href   (.url js/window
                               "valintojen-toteuttaminen.hakemus"
                               haku-oid
                               hakemus-oid)
                 :target "_blank"}
                [:i.zmdi.zmdi-collection-text.application-handling__review-area-main-heading-person-icon]
                [:span.application-handling__review-area-main-heading-person-oid
                 (str
                   @(subscribe [:editor/virkailija-translation :valintojen-toteuttaminen-uusi]))]]])]]
          [notifications-display]])
       (when (not (contains? (:answers application) :hakukohteet))
         [:ul.application-handling__hakukohteet-list
          (for [hakukohde-oid (:hakukohde application)]
            ^{:key (str "hakukohteet-list-row-" hakukohde-oid)}
            [:li.application-handling__hakukohteet-list-row
             [:div.application-handling__review-area-hakukohde-heading
              [names/hakukohde-and-tarjoaja-name hakukohde-oid]]])])]
      [:div.application-handling__navigation
       [:a.application-handling__navigation-link
        {:on-click #(dispatch [:application/navigate-application-list -1])}
        [:i.zmdi.zmdi-chevron-left]
        (str " " @(subscribe [:editor/virkailija-translation :navigate-applications-back]))]
       [:span.application-handling__navigation-link-divider "|"]
       [:a.application-handling__navigation-link
        {:on-click #(dispatch [:application/navigate-application-list 1])}
        (str @(subscribe [:editor/virkailija-translation :navigate-applications-forward]) " ")
        [:i.zmdi.zmdi-chevron-right]]]]]))
