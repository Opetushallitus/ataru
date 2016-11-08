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
  (fn [db [_ option-path]]
    (get-in db [:editor :followup-overlay option-path :visible?])))

(reg-event-db
  :editor/followup-overlay-open
  (fn [db [_ option-path]]
    (assoc-in db [:editor :followup-overlay option-path :visible?] true)))

(reg-event-db
  :editor/followup-overlay-close
  (fn [db [_ option-path]]
    (assoc-in db [:editor :followup-overlay option-path :visible?] false)))

(reg-event-fx
  :editor/followup-remove
  (fn [cofx [_ final-path]]
    {:db       (update-in (:db cofx) (flatten-path (:db cofx) (butlast final-path)) dissoc :followup)
     :dispatch [:editor/followup-overlay-close final-path final-path]}))

(reg-event-db
  :editor/generate-followup-component
  (fn [db [_ generate-fn option-path]]
    (let [component (generate-fn)]
      (assoc-in db (flatten-path db option-path :followup) component))))

(defn followup-question-overlay [option-path]
  (let [layer-visible?     (subscribe [:editor/followup-overlay option-path :visible?])
        followup-component (subscribe [:editor/get-component-value (flatten [option-path :followup])])]
    (fn [option-path]
      (when (or @layer-visible? @followup-component)
        [:div.editor-form__followup-question-overlay-outer
         [:div.editor-form__followup-question-overlay
          (if-let [followup @followup-component]
            [ataru.virkailija.editor.core/soresu->reagent followup (flatten [option-path :followup])]
            [toolbar/followup-add-component option-path
             (fn [generate-fn]
               (dispatch [:editor/generate-followup-component generate-fn option-path]))])]]))))

(defn followup-question [option-path]
  (let [layer-visible?      (subscribe [:editor/followup-overlay option-path :visible?])
        followup-component  (subscribe [:editor/get-component-value (flatten [option-path :followup])])
        ; disallow nesting followup questions
        top-level-followup? (nil? ((set option-path) :followup))]
    (fn [option-path]
      [:div.editor-form__followup-question
       (when top-level-followup?
         (match [@followup-component @layer-visible?]
           [nil true] [:a {:on-click #(dispatch [:editor/followup-overlay-close option-path])} "Poista Lisäkysymys"]
           [(_ :guard some?) _] nil
           :else [:a {:on-click #(dispatch [:editor/followup-overlay-open option-path])} "Lisäkysymys"]))])))
