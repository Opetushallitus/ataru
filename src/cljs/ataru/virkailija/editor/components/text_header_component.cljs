(ns ataru.virkailija.editor.components.text-header-component
  (:require [ataru.virkailija.temporal :as temporal]
            [clojure.string :as string]
            [goog.string :as s]
            [re-frame.core :refer [subscribe dispatch]]))

(defn- cut-component-button [path & {:keys [data-test-id]}]
  (case @(subscribe [:editor/component-button-state path :cut])
    :enabled
    [:button.editor-form__component-button
     {:on-click #(dispatch [:editor/copy-component path true])
      :data-test-id data-test-id}
     @(subscribe [:editor/virkailija-translation :cut-element])]
    :active
    [:button.editor-form__component-button
     {:on-click #(dispatch [:editor/cancel-copy-component])}
     @(subscribe [:editor/virkailija-translation :cancel-cut])]
    :disabled
    [:button.editor-form__component-button
     {:disabled true}
     @(subscribe [:editor/virkailija-translation :cut-element])]))

(defn- copy-component-button [path]
  (case @(subscribe [:editor/component-button-state path :copy])
    :enabled
    [:button.editor-form__component-button
     {:on-click #(dispatch [:editor/copy-component path false])}
     @(subscribe [:editor/virkailija-translation :copy-element])]
    :active
    [:button.editor-form__component-button
     {:on-click #(dispatch [:editor/cancel-copy-component])}
     @(subscribe [:editor/virkailija-translation :cancel-copy])]
    :disabled
    [:button.editor-form__component-button
     {:disabled true}
     @(subscribe [:editor/virkailija-translation :copy-element])]))

(defn- remove-component-button [path & {:keys [data-test-id property-key]}]
  (case @(subscribe [:editor/component-button-state path :remove])
    :enabled
    [:button.editor-form__component-button
     {:on-click #(dispatch [:editor/start-remove-component path])
      :data-test-id data-test-id}
     @(subscribe [:editor/virkailija-translation :remove])]
    :confirm
    [:div.editor-form__component-button-group
     [:button.editor-form__component-button.editor-form__component-button--confirm
      {:on-click (fn [_] (dispatch [:editor/confirm-remove-component path {:property-key property-key}]))
       :data-test-id (some-> data-test-id (str "-confirm"))}
      @(subscribe [:editor/virkailija-translation :confirm-delete])]
     [:button.editor-form__component-button
      {:on-click #(dispatch [:editor/cancel-remove-component path])}
      @(subscribe [:editor/virkailija-translation :cancel-remove])]]
    :disabled
    [:button.editor-form__component-button
     {:disabled true}
     @(subscribe [:editor/virkailija-translation :remove])]))

(defn- header-metadata
  [path metadata]
  [:div
   [:span.editor-form__component-main-header-metadata
    (s/format "%s %s, %s %s %s"
              @(subscribe [:editor/virkailija-translation :created-by])
              (-> metadata :created-by :name)
              @(subscribe [:editor/virkailija-translation :last-modified-by])
              (if (= (-> metadata :created-by :oid)
                     (-> metadata :modified-by :oid))
                ""
                (-> metadata :modified-by :name))
              (-> metadata :modified-by :date temporal/str->googdate temporal/time->date))]
   [:button.editor-form__component-lock-button
    {:on-click #(dispatch [:editor/toggle-component-lock path])}
    (if @(subscribe [:editor/component-locked? path])
      [:i.zmdi.zmdi-lock-outline.zmdi-hc-lg.editor-form__component-lock-button--locked]
      [:i.zmdi.zmdi-lock-open.zmdi-hc-lg])]])

(defn text-header
  [id label path metadata & {:keys [foldable?
                                    can-copy?
                                    can-cut?
                                    can-remove?
                                    sub-header
                                    data-test-id
                                    property-key]
                             :or   {foldable?   true
                                    can-copy?   true
                                    can-cut?    true
                                    can-remove? true}}]
  (let [folded? @(subscribe [:editor/folded? id])]
    [:div.editor-form__header-wrapper
     [:header.editor-form__component-header
      (when foldable?
        (if folded?
          [:button.editor-form__component-fold-button
           {:on-click #(dispatch [:editor/unfold id])}
           [:i.zmdi.zmdi-chevron-down]]
          [:button.editor-form__component-fold-button
           {:on-click #(dispatch [:editor/fold id])}
           [:i.zmdi.zmdi-chevron-up]]))
      [:span.editor-form__component-main-header
       (cond-> {}
               data-test-id
               (assoc :data-test-id (str data-test-id "-label")))
       label]
      [:span.editor-form__component-sub-header
       {:class (if (and folded? (some? sub-header))
                 "editor-form__component-sub-header-visible"
                 "editor-form__component-sub-header-hidden")}
       (when (some? sub-header)
         (->> [:fi :sv :en]
              (map (partial get sub-header))
              (remove string/blank?)
              (string/join " - ")))]]
     (when metadata
       [header-metadata path metadata])
     (when can-cut?
       [cut-component-button
        path
        :data-test-id (some-> data-test-id (str "-cut-component-button"))])
     (when can-copy?
       [copy-component-button path])
     (when can-remove?
       [remove-component-button
        path
        :data-test-id (some-> data-test-id (str "-remove-component-button"))
        :property-key property-key])]))
