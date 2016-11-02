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
                             (subscribe [:editor/get-component-value (flatten [option-path :followup])]))]
    (fn [followup-renderer path option-path]
      (when (or @layer-visible? (and followup-component @followup-component))
        [:div.editor-form__followup-question-overlay-outer
         [:div.editor-form__followup-question-overlay
          (if-let [followup (and followup-component @followup-component)]
            ; this is actually calling ataru.virkailija.editor.core/soresu->reagent,
            ; because of circular dependencies it cannot be directly required
            [followup-renderer followup (flatten [option-path :followup])]
            [toolbar/followup-add-component path
             (fn [generate-fn]
               (dispatch [:editor/generate-followup-component generate-fn path option-path]))])]]))))

(defn followup-question [path option-path]
  (let [layer-visible?      (subscribe [:editor/followup-overlay path option-path :visible?])
        followup-component  (subscribe [:editor/get-component-value (flatten [option-path :followup])])
        ; disallow nesting followup questions
        top-level-followup? (nil? ((set path) :followup))]
    (fn [path option-path]
      [:div.editor-form__followup-question
       (when top-level-followup?
         (if (or @layer-visible? @followup-component)
           [:a {:on-click #(dispatch [:editor/followup-overlay-close path option-path])} "Poista Lisäkysymys"]
           [:a {:on-click #(dispatch [:editor/followup-overlay-open path option-path])} "Lisäkysymys"]))])))
