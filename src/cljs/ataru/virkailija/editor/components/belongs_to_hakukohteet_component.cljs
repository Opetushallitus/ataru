(ns ataru.virkailija.editor.components.belongs-to-hakukohteet-component
  (:require [ataru.virkailija.views.hakukohde-and-hakukohderyhma-search :as h-and-h]
            [re-frame.core :refer [subscribe dispatch]]))

(defn- belongs-to-hakukohteet-modal
  [_ id _ _ _]
  (let [used-by-haku? (subscribe [:editor/used-by-haku?])
        haut (subscribe [:editor/filtered-haut id])
        hakukohderyhmat (subscribe [:editor/filtered-hakukohderyhmat id])]
    (fn [path id selected-hakukohteet selected-hakukohderyhmat is-option?]
      (if @used-by-haku?
        [h-and-h/popup
         [h-and-h/search-input
          {:id                       id
           :haut                     (map second @haut)
           :hakukohderyhmat          @hakukohderyhmat
           :hakukohde-selected?      #(contains? (set selected-hakukohteet) %)
           :hakukohderyhma-selected? #(contains? (set selected-hakukohderyhmat) %)}]
         [h-and-h/visibility-checkbox id path is-option?]
         [h-and-h/search-listing
          {:id                         id
           :haut                       (map second @haut)
           :hakukohderyhmat            @hakukohderyhmat
           :hakukohde-selected?        #(contains? (set selected-hakukohteet) %)
           :hakukohderyhma-selected?   #(contains? (set selected-hakukohderyhmat) %)
           :on-hakukohde-select        #(dispatch [:editor/add-to-belongs-to-hakukohteet path %])
           :on-hakukohde-unselect      #(dispatch [:editor/remove-from-belongs-to-hakukohteet path %])
           :on-hakukohderyhma-select   #(dispatch [:editor/add-to-belongs-to-hakukohderyhma path %])
           :on-hakukohderyhma-unselect #(dispatch [:editor/remove-from-belongs-to-hakukohderyhma path %])}]
         #(dispatch [:editor/hide-belongs-to-hakukohteet-modal id])]
        [h-and-h/popup
         [:div.belongs-to-hakukohteet-modal__no-haku-row
          [:p.belongs-to-hakukohteet-modal__no-haku
           @(subscribe [:editor/virkailija-translation :set-haku-to-form])]]
         [h-and-h/visibility-checkbox id path is-option?]
         [:div]
         #(dispatch [:editor/hide-belongs-to-hakukohteet-modal id])
         ]))))

(defn- belongs-to
  [_ _ _ _]
  (let [fetching? (subscribe [:editor/fetching-haut?])]
    (fn [_ _ name on-click]
      [:li.belongs-to-hakukohteet__hakukohde-list-item.animated.fadeIn
       [:span.belongs-to-hakukohteet__hakukohde-label
        (if @fetching?
          [:i.zmdi.zmdi-spinner.spin]
          name)]
       [:button.belongs-to-hakukohteet__hakukohde-remove
        {:on-click on-click}
        [:i.zmdi.zmdi-close.zmdi-hc-lg]]])))

(defn belongs-to-hakukohteet-option
  [parent-key index _]
  (let [id            (str parent-key "-" index)
        on-click-show (fn [_]
                        (dispatch [:editor/show-belongs-to-hakukohteet-modal id]))
        on-click-hide (fn [_]
                        (dispatch [:editor/hide-belongs-to-hakukohteet-modal id]))
        show-modal?   (subscribe [:editor/show-belongs-to-hakukohteet-modal id])
        form-locked?  (subscribe [:editor/form-locked?])]
    (fn [parent-key index path]
      (let [id            (str parent-key "-" index)
            initial-content @(subscribe [:editor/get-component-value path])
            visible-hakukohteet     (mapv (fn [oid] {:oid      oid
                                                     :name     @(subscribe [:editor/belongs-to-hakukohde-name oid])
                                                     :on-click (fn [_] (dispatch [:editor/remove-from-belongs-to-hakukohteet
                                                                                  path oid]))})
                                          (:belongs-to-hakukohteet initial-content))
            visible-hakukohderyhmat (mapv (fn [oid] {:oid      oid
                                                     :name     @(subscribe [:editor/belongs-to-hakukohderyhma-name oid])
                                                     :on-click (fn [_] (dispatch [:editor/remove-from-belongs-to-hakukohderyhma
                                                                                  path oid]))})
                                          (:belongs-to-hakukohderyhma initial-content))
            hidden? @(subscribe [:editor/get-component-value (conj path :hidden)])
            visible                 (sort-by :name (concat visible-hakukohteet
                                                           visible-hakukohderyhmat))]
        [:div.belongs-to-hakukohteet
         [:button.belongs-to-hakukohteet__modal-toggle
          {:disabled @form-locked?
           :class    (when @form-locked? "belongs-to-hakukohteet__modal-toggle--disabled")
           :on-click (when-not @form-locked?
                       (if @show-modal? on-click-hide on-click-show))}
          (str @(subscribe [:editor/virkailija-translation :visibility-on-form]) " ")]
         [:span.belongs-to-hakukohteet__modal-toggle-label
          (cond hidden?
                @(subscribe [:editor/virkailija-translation :hidden])

                (and (empty? visible))
                @(subscribe [:editor/virkailija-translation :visible-to-all])

                :else
                @(subscribe [:editor/virkailija-translation :visible-to-hakukohteet]))]
         (when @show-modal?
           [belongs-to-hakukohteet-modal path
            id
            (map :oid visible-hakukohteet)
            (map :oid visible-hakukohderyhmat)
            true])
         [:ul.belongs-to-hakukohteet__hakukohde-list
          (for [{:keys [oid name on-click]} visible]
            ^{:key oid}
            [belongs-to path oid name on-click])]]))))

(defn belongs-to-hakukohteet
  [path initial-content]
  (let [id            (:id initial-content)
        on-click-show (fn [_]
                        (dispatch [:editor/show-belongs-to-hakukohteet-modal id]))
        on-click-hide (fn [_]
                        (dispatch [:editor/hide-belongs-to-hakukohteet-modal id]))
        show-modal?   (subscribe [:editor/show-belongs-to-hakukohteet-modal id])
        component-locked?  (subscribe [:editor/component-locked? path])]
    (fn [path initial-content]
      (let [visible-hakukohteet     (mapv (fn [oid] {:oid      oid
                                                     :name     @(subscribe [:editor/belongs-to-hakukohde-name oid])
                                                     :on-click (fn [_] (dispatch [:editor/remove-from-belongs-to-hakukohteet
                                                                                  path oid]))})
                                          (:belongs-to-hakukohteet initial-content))
            visible-hakukohderyhmat (mapv (fn [oid] {:oid      oid
                                                     :name     @(subscribe [:editor/belongs-to-hakukohderyhma-name oid])
                                                     :on-click (fn [_] (dispatch [:editor/remove-from-belongs-to-hakukohderyhma
                                                                                  path oid]))})
                                          (:belongs-to-hakukohderyhma initial-content))
            hidden?                 (subscribe [:editor/get-component-value path :params :hidden])
            visible                 (sort-by :name (concat visible-hakukohteet
                                                           visible-hakukohderyhmat))]
        [:div.belongs-to-hakukohteet
         [:button.belongs-to-hakukohteet__modal-toggle
          {:disabled @component-locked?
           :class    (when @component-locked? "belongs-to-hakukohteet__modal-toggle--disabled")
           :on-click (when-not @component-locked?
                       (if @show-modal? on-click-hide on-click-show))}
          (str @(subscribe [:editor/virkailija-translation :visibility-on-form]) " ")]
         [:span.belongs-to-hakukohteet__modal-toggle-label
          (cond @hidden?
                @(subscribe [:editor/virkailija-translation :hidden])

                (and (empty? visible))
                @(subscribe [:editor/virkailija-translation :visible-to-all])

                :else
                @(subscribe [:editor/virkailija-translation :visible-to-hakukohteet]))]
         (when @show-modal?
           [belongs-to-hakukohteet-modal path
            (:id initial-content)
            (map :oid visible-hakukohteet)
            (map :oid visible-hakukohderyhmat)
            false])
         [:ul.belongs-to-hakukohteet__hakukohde-list
          (for [{:keys [oid name on-click]} visible]
            ^{:key oid}
            [belongs-to path oid name on-click])]]))))
