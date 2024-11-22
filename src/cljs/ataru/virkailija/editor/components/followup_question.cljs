(ns ataru.virkailija.editor.components.followup-question
  (:require
    [ataru.cljs-util :as util]
    [ataru.virkailija.editor.components.drag-n-drop-spacer :as dnd]
    [ataru.virkailija.editor.components.toolbar :as toolbar]
    [ataru.virkailija.temporal :as temporal]
    [goog.string :as s]
    [re-frame.core :refer [subscribe dispatch reg-event-db]]))

(reg-event-db
  :editor/generate-followup-component
  (fn [db [_ generate-fn option-path]]
    (let [user-info (-> db :editor :user-info)
          metadata {:oid  (:oid user-info)
                    :name (:name user-info)
                    :date (temporal/datetime-now)}
          component (generate-fn {:created-by  metadata
                                  :modified-by metadata})]
      (update-in db (util/flatten-path db option-path :followups) (fnil conj []) component))))

(defn followup-question-overlay [option-index followups path show-followups]
  (when (get @show-followups option-index)
    (let [option-path (conj path :options option-index)]
      [:div.editor-form__followup-question-overlay-parent
       [:div.editor-form__followup-question-overlay-outer
        [:div.editor-form__followup-indicator]
        [:div.editor-form__followup-indicator-inlay]
        [:div.editor-form__followup-question-overlay
         followups
         [dnd/drag-n-drop-spacer (conj option-path :followups (count followups))]
         (when-not @(subscribe [:editor/component-locked? path])
           [toolbar/followup-toolbar option-path
            (fn [generate-fn]
              (dispatch [:editor/generate-followup-component generate-fn option-path]))])]]])))

(defn followup-question-overlay-readonly [option-index followups show-followups]
  (when (get @show-followups option-index)
    [:div.editor-form__followup-question-overlay-parent
     [:div.editor-form__followup-question-overlay-outer
      [:div.editor-form__followup-indicator]
      [:div.editor-form__followup-indicator-inlay]
        [:div.editor-form__followup-question-overlay followups]]]))

(defn followup-question
  ([option-index followups show-followups]
   (followup-question option-index followups show-followups
                      @(subscribe [:editor/virkailija-translation :followups]) (count followups)))
  ([option-index followups show-followups label-in-selected-lang followup-count]
   (let [attrs {:on-click     #(swap! show-followups
                                      (fn [v] (update v option-index not)))
                :data-test-id "followup-question-followups"}]
     [:div.editor-form__followup-question
      (if (empty? followups)
        [:a attrs label-in-selected-lang]
        [:a attrs
         (s/format "%s (%d) "
                   label-in-selected-lang
                   followup-count)
         (if (get @show-followups option-index)
           [:i.zmdi.zmdi-chevron-up.zmdi-hc-lg]
           [:i.zmdi.zmdi-chevron-down.zmdi-hc-lg])])])))
