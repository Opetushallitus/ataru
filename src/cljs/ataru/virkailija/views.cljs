(ns ataru.virkailija.views
    (:require [re-frame.core :as re-frame]
              [reagent.core :as r]
              [ataru.virkailija.views.banner :refer [snackbar top-banner]]
              [ataru.virkailija.application.view :refer [application application-version-changes]]
              [ataru.virkailija.application.attachments.virkailija-attachment-view :as attachments]
              [ataru.virkailija.views.template-editor :refer [email-template-editor]]
              [ataru.virkailija.dev.lomake :as l]
              [ataru.virkailija.editor.view :refer [editor]]
              [taoensso.timbre :refer-macros [spy]]))

(def panel-components
  {:editor editor :application application})

(defn no-privileges []
  [:div.privilege-info-outer [:div.privilege-info-inner "Ei oikeuksia"]])

(defn some-right-exists-for-user? [rights orgs]
  (boolean (some rights (->> orgs (map :rights) flatten (map keyword)))))

(defn privileged-panel [panel rights]
  (let [organizations (re-frame/subscribe [:state-query [:editor :user-info :organizations]])]
    (fn [panel rights]
      (if (some-right-exists-for-user? rights @organizations)
        [(get panel-components panel)]
        [no-privileges]))))

(defmulti panels identity)
(defmethod panels :application []
  [privileged-panel :application #{:view-applications :edit-applications}])
(defmethod panels :editor []
  [privileged-panel :editor #{:form-edit}])
(defmethod panels :default [])

(defn main-panel []
  (let [active-panel             (re-frame/subscribe [:active-panel])
        template-editor-visible? (re-frame/subscribe [:state-query [:editor :ui :template-editor-visible?]])
        texts                    (re-frame/subscribe [:editor/virkailija-texts])
        attachment-preview-mode? (re-frame/subscribe [:state-query [:application :attachment-preview :visible?]])]
    (fn []
      (when (not-empty @texts)
        [:div.main-container
         [:div.modal-container
          [:input#editor-form__copy-question-id-container
           {:value ""}]
          [snackbar]
          [application-version-changes]
          (when @template-editor-visible?
            [email-template-editor])]
         [top-banner]
         (if @attachment-preview-mode?
           [attachments/attachment-preview]
           [:div (panels @active-panel)])]))))
