(ns ataru.virkailija.editor.components.followup-question
  (:require
    [ataru.virkailija.component-data.component :as component]
    [ataru.virkailija.editor.components.toolbar :as toolbar]
    [cljs.core.match :refer-macros [match]]
    [re-frame.core :refer [subscribe dispatch reg-sub reg-event-db reg-fx reg-event-fx]]
    [reagent.core :as r]
    [reagent.ratom :refer-macros [reaction]]
    [taoensso.timbre :refer-macros [spy debug]]))

(defn flatten-path [db & parts]
  (flatten [:editor :forms (-> db :editor :selected-form-key) :content [parts]]))

(defn followups? [options]
  (some some? (map :followup options)))

(reg-sub :editor/followup-overlay
  (fn [db [_ path option-path]]
    (get-in db [:editor :followup-overlay path option-path :visible?])))

(reg-event-db
  :editor/followup-overlay-open
  (fn [db [_ path option-path]]
    (assoc-in db [:editor :followup-overlay path option-path :visible?] true)))

(reg-event-db
  :editor/followup-overlay-close
  (fn [db [_ path option-path]]
    (-> db
      (assoc-in [:editor :followup-overlay path option-path :visible?] false)
      (update-in (flatten [:editor :forms (-> db :editor :selected-form-key) :content path (drop 1 option-path)]) dissoc :followup))))

(reg-event-db
  :editor/add-followup-question
  (fn [db [_ path option-path]]
    (do
      (flatten-path db path option-path)
      db)))

(reg-event-db
  :editor/generate-followup-component
  (fn [db [_ generate-fn path option-path]]
    (let [component (generate-fn)]
      (assoc-in db (flatten-path db path (drop 1 option-path) :followup) component))))

(defn followup-question-overlay [followup-renderer path option-path]
  (let [layer-visible?     (subscribe [:editor/followup-overlay path option-path :visible?])
        followup-component (when followup-renderer
                             (subscribe (spy [:editor/get-component-value (flatten [option-path :followup])])))]
    (fn [followup-renderer path option-path]
      (when @layer-visible?
        [:div.editor-form__followup-editor-outer
         [:div.editor-form__followup-editor
          (when-let [followup (and followup-component @followup-component)]
            ; this is actually calling, but it cannot be directly required because of
            ; circular dependencies
            ; ataru.virkailija.editor.core/soresu->reagent
            [followup-renderer followup (spy (flatten [option-path :followup]))])
          [toolbar/followup-add-component path
           (fn [generate-fn]
             (dispatch [:editor/generate-followup-component generate-fn path option-path]))]]]))))

(defn followup-question [path option-path]
  (let [layer-visible? (subscribe [:editor/followup-overlay path option-path :visible?])]
    (fn [path option-path]
      [:div.editor-form__followup-questions
       (if @layer-visible?
         [:a {:on-click #(dispatch [:editor/followup-overlay-close path option-path])} "Poista Lisäkysymys"]
         [:a {:on-click #(dispatch [:editor/followup-overlay-open path option-path])} "Lisäkysymys"])])))
