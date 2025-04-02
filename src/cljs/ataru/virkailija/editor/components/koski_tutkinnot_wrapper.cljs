(ns ataru.virkailija.editor.components.koski-tutkinnot-wrapper
  (:require [ataru.cljs-util :as util]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.ratom :refer-macros [reaction]]
            [ataru.virkailija.editor.components.toolbar :as toolbar]
            [ataru.virkailija.editor.components.drag-n-drop-spacer :as dnd]
            [ataru.virkailija.editor.components.component-content :as component-content]
            [ataru.virkailija.editor.components.input-field-component :as input-field-component]
            [ataru.virkailija.editor.components.input-fields-with-lang-component :as input-fields-with-lang-component]
            [ataru.virkailija.editor.components.text-header-component :as text-header-component]
            [ataru.translations.texts :refer [koski-tutkinnot-texts]]))

(defn tutkinnot-wrapper [content path children]
  (let [id (:id content)
        languages @(subscribe [:editor/languages])
        virkailija-lang @(subscribe [:editor/virkailija-lang])
        value @(subscribe [:editor/get-component-value path])
        component-locked? (subscribe [:editor/component-locked? path])
        group-header-text @(subscribe [:editor/virkailija-translation :wrapper-element])
        header-label-text @(subscribe [:editor/virkailija-translation :wrapper-header])
        description (get-in content [:tutkinnot :description])
        field-list (get-in content [:tutkinnot :field-list])
        ;completed-studies-question-id (util/new-uuid)
        koski-update-policy-only-once-id (util/new-uuid)
        koski-update-policy-allways-id (util/new-uuid)
        ;completed-studies-checked? (reaction @(subscribe [:editor/get-property-value :tutkinto-properties :show-completed-studies]))
        save-koski-tutkinnot? (reaction @(subscribe [:editor/get-property-value :tutkinto-properties :save-koski-tutkinnot]))
        is-superuser? @(subscribe [:application/superuser?])]
    [:div.editor-form__component-wrapper
     {:data-test-id "tutkinnot-wrapper"}
     [text-header-component/text-header id group-header-text path (:metadata content)
      :sub-header (:label value) :data-test-id "tutkinnot-header" :property-key :tutkinto-properties]
     [component-content/component-content
      path                                                  ;id
      [:div
       [:div.editor-form__tutkinto-field-wrapper
        [:div.editor-form__component-item-description
         (when (some? description)
           [:span (get-in description [virkailija-lang])])
         (when (some? field-list)
           [:div
            [:span.editor-form__module-fields-label @(subscribe [:editor/virkailija-translation :contains-fields])]
            " "
            [:span (get-in field-list [virkailija-lang])]])]
        [:header.editor-form__component-item-header header-label-text]
        (input-fields-with-lang-component/input-fields-with-lang
          (fn [lang]
            [input-field-component/input-field {:path        path
                                                :lang        lang
                                                :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                              (-> % .-target .-value)
                                                                              path :label lang])}])
          languages
          :header? true)
        [:div.editor-form__component-content-wrapper--no-indent
         [:div.editor-form__checkbox-container
          ; @TODO Otetaan käyttöön kun opintosuoritukset lisätään
          ;[:input.editor-form__checkbox
          ; {:id        completed-studies-question-id
          ;  :data-test-id "completed-studies-question-id"
          ;  :type      "checkbox"
          ;  :disabled  @component-locked?
          ;  :checked   (boolean @completed-studies-checked?)
          ;  :on-change (fn [event]
          ;               (.preventDefault event)
          ;               (dispatch
          ;                 [:editor/set-property-value :tutkinto-properties :show-completed-studies (-> event .-target .-checked)]))}]
          ; [:label.editor-form__checkbox-label
          ; {:for completed-studies-question-id}
          ; (get-in koski-tutkinnot-texts [:completed-study-question-label virkailija-lang])]
          ]]
        [:div.editor-form__single-choice-button-container
         {:role "radiogroup"}
         [:label.editor-form__single-choice-button-container.label (get-in koski-tutkinnot-texts [:koski-update-policy-label virkailija-lang])]
         [:div.editor-form__single-choice-button-container.choice
          [:input.editor-form__plain-radio
           {:type      "radio"
            :value     false
            :id        koski-update-policy-allways-id
            :data-test-id "koski-update-policy-allways-id"
            :checked   (not (boolean @save-koski-tutkinnot?))
            :disabled  (or @component-locked? (not is-superuser?))
            :on-change (fn [event]
                         (.preventDefault event)
                         (dispatch
                           [:editor/set-property-value :tutkinto-properties :save-koski-tutkinnot false]))}]
          [:label.editor-form__single-choice-button-container.choice.label
           {:for koski-update-policy-allways-id}
           (get-in koski-tutkinnot-texts [:koski-update-option-allways-label virkailija-lang])]]
         [:div.editor-form__single-choice-button-container.choice
          [:input.editor-form__plain-radio
           {:type      "radio"
            :value     true
            :id        koski-update-policy-only-once-id
            :data-test-id "koski-update-policy-only-once-id"
            :checked   (boolean @save-koski-tutkinnot?)
            :disabled  (or @component-locked? (not is-superuser?))
            :on-change (fn [event]
                         (.preventDefault event)
                         (dispatch
                           [:editor/set-property-value :tutkinto-properties :save-koski-tutkinnot true]))}]
          [:label.editor-form__single-choice-button-container.choice.label
           {:for koski-update-policy-only-once-id}
           (get-in koski-tutkinnot-texts [:koski-update-option-only-once-label virkailija-lang])]]]]
       [:div.editor-form__wrapper-element-well
        children]
       [dnd/drag-n-drop-spacer (conj path :children (count children))]
       (when-not @(subscribe [:editor/component-locked? path])
         [toolbar/add-component (conj path :children (count children)) false])]]]))
