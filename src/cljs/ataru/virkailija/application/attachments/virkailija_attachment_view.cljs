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
    [:div.attachment-preview-header-section.attachment-preview-header-details-section
     [:span.attachment-preview-header__text name-and-ssn-text]
     [:span.attachment-preview-header__text.attachment-preview-header__text--bold.attachment-preview-header__text--no-overflow.animated.fadeIn
      liitepyynto-text]]))

(defn- attachment-preview-navigation-button [direction current-index max-index selected-attachment-keys]
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
    [:a.attachment-preview-control-button
     {:on-click #(re-frame/dispatch [:virkailija-attachments/select-attachment new-selected-attachment-key])}
     (case direction
       :left [:i.zmdi.zmdi-chevron-left]
       :right [:i.zmdi.zmdi-chevron-right])]))

(defn- attachment-preview-close-button []
  [:a.attachment-preview-control-button
   {:on-click #(re-frame/dispatch [:virkailija-attachments/close-attachment-preview])}
   [:i.zmdi.zmdi-close]])

(def liitepyynnot->attachment-keys-xform (comp (mapcat (partial map (fn [liitepyynto]
                                                                      (let [values (:values liitepyynto)]
                                                                        (cond->> values
                                                                                 (every? vector? values)
                                                                                 (flatten))))))
                                               (mapcat identity)
                                               (map :key)
                                               (distinct)
                                               (filter (fn [attachment-key]
                                                         @(re-frame/subscribe [:virkailija-attachments/attachment-selected? attachment-key])))))

(defn- attachment-preview-index-text [current-index max-index]
  [:span.attachment-preview-index-text (str (inc current-index) " / " (inc max-index))])

(defn- download-url [attachment]
  (str "/lomake-editori/api/files/content/" (:key attachment)))

(defn- attachment-preview-filename [selected-attachment]
  (let [download-label (str "lataa ("
                            (-> selected-attachment :size u/size-bytes->str)
                            ")")
        download-url   (download-url selected-attachment)
        filename       (-> selected-attachment :filename)]
    [:<>
     [:div]
     [:span.attachment-preview-header__text filename]
     [:div.attachment-preview-header__naming-bar-right-element-container
      [:span.attachment-preview-header__text.attachment-preview-header__naming-bar-right-element
       [:a {:href download-url}
        download-label]]]]))

(defn- attachment-preview-state-list [liitepyynto-for-selected-hakukohteet]
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
        [:div.application-review-dropdown
         [:div.application-review-dropdown__list
          (if @list-opened?
            (for [[state labels] review-types]
              (let [label-i18n (-> labels lang)]
                ^{:key state}
                [:div.application-review-dropdown__list-item
                 {:on-click (fn []
                              (when-not (= state effective-liitepyynto-state)
                                (doseq [hakukohde-oid all-hakukohde-oids]
                                  (re-frame/dispatch [:application/update-attachment-review (:key effective-liitepyynto) hakukohde-oid state])))
                              (swap! list-opened? not))}
                 (when (= state effective-liitepyynto-state)
                   [:i.zmdi.zmdi-check.attachment-review-dropdown__checkmark])
                 [:span.attachment-review-dropdown__label
                  (str label-i18n)]]))
            [:div.application-review-dropdown__list-item
             (if can-edit?
               {:on-click #(swap! list-opened? not)}
               {:class "application-review-dropdown--disabled"})
             [:i.zmdi.zmdi-check.attachment-review-dropdown__checkmark]
             [:span.attachment-review-dropdown__label
              (application-states/get-review-state-label-by-name review-types effective-liitepyynto-state lang)]])]]))))



(defn- attachment-preview-image-view [selected-attachment]
  (let [download-url (download-url selected-attachment)]
    [:img.attachment-preview-image-view__image {:src download-url}]))

(defn attachment-preview []
  (let [selected-hakukohde-oids               @(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
        liitepyynnot-for-selected-hakukohteet (map (fn [selected-hakukohde-oid]
                                                     (map (fn [liitepyynto]
                                                            (assoc liitepyynto :hakukohde-oid selected-hakukohde-oid))
                                                          @(re-frame/subscribe [:application/get-attachment-reviews-for-selected-hakukohde selected-hakukohde-oid])))
                                                   selected-hakukohde-oids)
        selected-attachment-key               @(re-frame/subscribe [:state-query [:application :attachment-preview :selected-attachment-key]])
        selected-attachment-and-liitepyynto   (->> liitepyynnot-for-selected-hakukohteet
                                                   (transduce (comp (mapcat identity)
                                                                    (mapcat (fn [liitepyynto]
                                                                              (let [values      (:values liitepyynto)
                                                                                    attachments (cond->> values
                                                                                                         (every? vector? values)
                                                                                                         (flatten))]
                                                                                (map (fn [attachment]
                                                                                       {:liitepyynto (dissoc liitepyynto :values)
                                                                                        :attachment  attachment})
                                                                                     attachments))))
                                                                    (filter (comp (partial = selected-attachment-key)
                                                                                  :key
                                                                                  :attachment)))
                                                              conj)
                                                   (first))
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
                                                   (flatten)
                                                   (filter (comp (partial = (:key selected-liitepyynto))
                                                                 :key)))]
    [:div.attachment-preview
     [:div.attachment-preview-header
      [attachment-header selected-liitepyynto]
      [:div.attachment-preview-header-section.attachment-preview-header-control-buttons-section
       [attachment-preview-state-list liitepyynto-for-selected-hakukohteet]
       [attachment-preview-navigation-button :left current-index max-index selected-attachment-keys]
       [attachment-preview-index-text current-index max-index]
       [attachment-preview-navigation-button :right current-index max-index selected-attachment-keys]]
      [:div.attachment-preview-header-section.attachment-preview-header-close-button-section
       [attachment-preview-close-button]]]
     [:div.attachment-preview-naming-bar
      [attachment-preview-filename selected-attachment]]
     [:div.attachment-preview-image-view
      [attachment-preview-image-view selected-attachment]]]))
