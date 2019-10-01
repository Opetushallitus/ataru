(ns ataru.virkailija.application.attachments.virkailija-attachment-view
  (:require [ataru.application.application-states :as application-states]
            [ataru.application.review-states :as review-states]
            [ataru.util :as u]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

(defn- attachment-header [selected-liitepyynto]
  (let [selected-application @(re-frame/subscribe [:application/selected-application])
        lang                 @(re-frame/subscribe [:editor/virkailija-lang])
        name-and-ssn-text    (str (-> selected-application :person :preferred-name)
                                  " "
                                  (-> selected-application :person :last-name)
                                  " - "
                                  (-> selected-application :person :ssn))
        liitepyynto-text     (-> selected-liitepyynto :label lang)]
    [:div.attachment-skimming-header-section.attachment-skimming-header-details-section
     [:span.attachment-skimming-header__text name-and-ssn-text]
     [:span.attachment-skimming-header__text.attachment-skimming-header__text--bold.attachment-skimming-header__text--no-overflow.animated.fadeIn
      {:title liitepyynto-text}
      liitepyynto-text]]))

(defn- attachment-skimming-navigation-button [direction current-index max-index selected-attachment-keys]
  (let [new-index                   (cond (and (= direction :left)
                                               (= current-index 0))
                                          max-index

                                          (and (= direction :right)
                                               (= current-index max-index))
                                          0

                                          (= direction :left)
                                          (dec current-index)

                                          (= direction :right)
                                          (inc current-index))
        new-selected-attachment-key (nth selected-attachment-keys new-index)]
    [:a.attachment-skimming-control-button
     {:on-click #(re-frame/dispatch [:virkailija-attachments/select-attachment new-selected-attachment-key])}
     (case direction
       :left [:i.zmdi.zmdi-chevron-left.attachment-skimming-control-button__icon--offset-left]
       :right [:i.zmdi.zmdi-chevron-right.attachment-skimming-control-button__icon--offset-right])]))

(defn- attachment-skimming-close-button []
  [:a.attachment-skimming-control-button
   {:on-click #(re-frame/dispatch [:virkailija-attachments/close-attachment-skimming])}
   [:i.zmdi.zmdi-close]])

(def liitepyynnot->attachment-keys-xform (comp (map (fn [liitepyynto]
                                                      (let [values (:values liitepyynto)]
                                                        (cond->> values
                                                          (every? vector? values)
                                                          (flatten)))))
                                               (mapcat identity)
                                               (map :key)
                                               (distinct)
                                               (filter (fn [attachment-key]
                                                         @(re-frame/subscribe [:virkailija-attachments/attachment-selected? attachment-key])))))

(defn- attachment-skimming-index-text [current-index max-index]
  [:span.attachment-skimming-index-text (str (inc current-index) " / " (inc max-index))])

(defn- download-url [attachment]
  (str "/lomake-editori/api/files/content/" (:key attachment)))

(defn- attachment-skimming-filename [selected-attachment]
  (let [download-label    (str "lataa ("
                               (-> selected-attachment :size u/size-bytes->str)
                               ")")
        download-url      (download-url selected-attachment)
        filename          (-> selected-attachment :filename)
        can-display-file? @(re-frame/subscribe [:virkailija-attachments/can-display-file? (:key selected-attachment)])]
    [:<>
     [:div]
     [:span.attachment-skimming-header__text filename]
     [:div.attachment-skimming-header__naming-bar-right-element-container
      [:span.attachment-skimming-header__text.attachment-skimming-header__naming-bar-right-element
       [:a {:href download-url}
        download-label]
       (when-not
         can-display-file?
         [:div.attachment-skimming-header__cannot-display-text.animated.fadeIn
          [:div.attachment-skimming-header__cannot-display-text-indicator]
          [:span "Tätä liitettä ei valitettavasti voida näyttää esikatselussa, mutta voit ladata sen tästä tiedostona."]])]]]))

(defn- attachment-skimming-state-list []
  (let [list-opened? (reagent/atom false)]
    (fn [liitepyynto-for-selected-hakukohteet]
      (let [lang                         @(re-frame/subscribe [:editor/virkailija-lang])
            all-liitepyynto-states       (map :state liitepyynto-for-selected-hakukohteet)
            all-hakukohde-oids           (map :hakukohde-oid liitepyynto-for-selected-hakukohteet)
            multiple-liitepyynto-states? (->> all-liitepyynto-states
                                              (distinct)
                                              (count)
                                              (< 1))
            effective-liitepyynto        (first liitepyynto-for-selected-hakukohteet)
            effective-liitepyynto-state  (or (when multiple-liitepyynto-states?
                                               "multiple-values")
                                             (:state effective-liitepyynto)
                                             "not-checked")
            review-types                 (if multiple-liitepyynto-states?
                                           review-states/attachment-hakukohde-review-types-with-multiple-values
                                           review-states/attachment-hakukohde-review-types)
            can-edit?                    @(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])]
        [:div.attachment-skimming-review-dropdown
         [:div.attachment-skimming-review-dropdown__list
          (if @list-opened?
            (for [[state labels] review-types]
              (let [label-i18n (-> labels lang)]
                ^{:key state}
                [:div.attachment-skimming-review-dropdown__list-item
                 {:on-click (fn []
                              (when-not (= state effective-liitepyynto-state)
                                (doseq [hakukohde-oid all-hakukohde-oids]
                                  (re-frame/dispatch [:application/update-attachment-review (:key effective-liitepyynto) hakukohde-oid state])))
                              (swap! list-opened? not))}
                 (when (= state effective-liitepyynto-state)
                   [:i.zmdi.attachment-review-dropdown__checkmark
                    {:class (if (= state "multiple-values")
                              "zmdi-check-all"
                              "zmdi-check")}])
                 [:span.attachment-review-dropdown__label
                  (str label-i18n)]]))
            [:div.attachment-skimming-review-dropdown__list-item
             (if can-edit?
               {:on-click #(swap! list-opened? not)}
               {:class "attachment-skimming-review-dropdown--disabled"})
             [:i.zmdi.attachment-review-dropdown__checkmark
              {:class (if (= effective-liitepyynto-state "multiple-values")
                        "zmdi-check-all"
                        "zmdi-check")}]
             [:span.attachment-review-dropdown__label
              (application-states/get-review-state-label-by-name review-types effective-liitepyynto-state lang)]])]]))))

(defn- attachment-skimming-image-view [selected-attachment]
  (let [download-url      (download-url selected-attachment)
        can-display-file? @(re-frame/subscribe [:virkailija-attachments/can-display-file? (:key selected-attachment)])]
    [:div.attachment-skimming-image-view
     (if can-display-file?
       [:img.attachment-skimming-image-view__image
        {:src download-url}]
       [:div.attachment-skimming-image-view-no-preview
        [:span.attachment-skimming-image-view-no-preview__text "?"]])]))

(defn attachment-skimming []
  (let [liitepyynnot-for-selected-hakukohteet @(re-frame/subscribe [:virkailija-attachments/liitepyynnot-for-selected-hakukohteet])
        selected-attachment-key               @(re-frame/subscribe [:state-query [:application :attachment-skimming :selected-attachment-key]])
        selected-attachment-and-liitepyynto   @(re-frame/subscribe [:virkailija-attachments/selected-attachment-and-liitepyynto])
        selected-liitepyynto                  (:liitepyynto selected-attachment-and-liitepyynto)
        selected-attachment                   (:attachment selected-attachment-and-liitepyynto)
        selected-attachment-keys              (transduce liitepyynnot->attachment-keys-xform
                                                         conj
                                                         liitepyynnot-for-selected-hakukohteet)
        current-index                         (->> selected-attachment-keys
                                                   (transduce (comp (map-indexed (fn [idx attachment-key]
                                                                                   [idx attachment-key]))
                                                                    (filter (fn [[_ attachment-key]]
                                                                              (= attachment-key selected-attachment-key)))
                                                                    (map first))
                                                              conj)
                                                   (first))
        max-index                             (-> selected-attachment-keys count dec)
        liitepyynto-for-selected-hakukohteet  (->> liitepyynnot-for-selected-hakukohteet
                                                   (filter (comp (partial = (:key selected-liitepyynto))
                                                                 :key)))]
    [:div.attachment-skimming
     [:div.attachment-skimming-fixed-headers
      [:div.attachment-skimming-header
       [attachment-header selected-liitepyynto]
       [:div.attachment-skimming-header-section.attachment-skimming-header-control-buttons-section
        [attachment-skimming-state-list liitepyynto-for-selected-hakukohteet]
        [attachment-skimming-navigation-button :left current-index max-index selected-attachment-keys]
        [attachment-skimming-index-text current-index max-index]
        [attachment-skimming-navigation-button :right current-index max-index selected-attachment-keys]]
       [:div.attachment-skimming-header-section.attachment-skimming-header-close-button-section
        [attachment-skimming-close-button]]]
      [:div.attachment-skimming-naming-bar
       [attachment-skimming-filename selected-attachment]]]
     [attachment-skimming-image-view selected-attachment]]))
