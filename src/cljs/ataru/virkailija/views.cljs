(ns ataru.virkailija.views
    (:require [re-frame.core :as re-frame]
              [ataru.virkailija.views.banner :refer [snackbar toaster top-banner]]
              [ataru.virkailija.application.view :refer [application]]
              [ataru.virkailija.application.view.virkailija-application-version-history :refer [application-version-changes]]
              [ataru.virkailija.application.attachments.virkailija-attachment-view :as attachments]
              [ataru.virkailija.views.template-editor :refer [email-template-editor]]
              [ataru.virkailija.editor.view :refer [editor]]
              [ataru.virkailija.error.view :refer [error]]))

(def panel-components
  {:editor      editor
   :application application
   :error       error})

(defn no-privileges []
  [:div.privilege-info-outer [:div.privilege-info-inner "Ei oikeuksia"]])

(defn- loading-spinner
  []
  [:div.loading__curtain
   [:i.zmdi.zmdi-spinner.spin]])

(defn some-right-exists-for-user? [rights orgs]
  (boolean (some rights (->> orgs (map :rights) flatten (map keyword)))))

(defn privileged-panel [_ _]
  (let [organizations (re-frame/subscribe [:state-query [:editor :user-info :organizations]])]
    (fn [panel rights]
      (if (some-right-exists-for-user? rights @organizations)
        [(get panel-components panel)]
        [no-privileges]))))

(defmulti panels identity)
(defmethod panels :application []
  [privileged-panel :application #{:view-applications :edit-applications :opinto-ohjaaja}])
(defmethod panels :editor []
  [privileged-panel :editor #{:form-edit}])
(defmethod panels :error []
  [(get panel-components :error)])
(defmethod panels :default [])

(defn main-panel []
  (let [active-panel             (re-frame/subscribe [:active-panel])
        template-editor-visible? (re-frame/subscribe [:state-query [:editor :ui :template-editor-visible?]])
        texts                    (re-frame/subscribe [:editor/virkailija-texts])
        attachment-skimming-mode? (re-frame/subscribe [:state-query [:application :attachment-skimming :visible?]])
        load-spinner-running? (re-frame/subscribe [:editor/load-spinner-running?])]
    (fn []
      (when (not-empty @texts)
        [:div.main-container
         [:div.modal-container
          [:input#editor-form__copy-question-id-container
           {:value "" 
            :read-only true}]
          [snackbar]
          [application-version-changes]
          (when @template-editor-visible?
            [email-template-editor])]
         [top-banner]
         [toaster]
         (when @load-spinner-running?
           [loading-spinner])
         (if @attachment-skimming-mode?
           [attachments/attachment-skimming]
           [:div (panels @active-panel)])]))))
