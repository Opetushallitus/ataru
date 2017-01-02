(ns ataru.virkailija.editor.components.followup-question
  (:require
    [ataru.cljs-util :as util]
    [ataru.virkailija.component-data.component :as component]
    [ataru.virkailija.editor.components.toolbar :as toolbar]
    [cljs.core.match :refer-macros [match]]
    [re-frame.core :refer [subscribe dispatch reg-sub reg-event-db reg-fx reg-event-fx]]
    [reagent.core :as r]
    [reagent.ratom :refer-macros [reaction]]
    [taoensso.timbre :refer-macros [spy debug]]))

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

(reg-event-db
  :editor/generate-followup-component
  (fn [db [_ generate-fn option-path]]
    (let [component (generate-fn)]
      (update-in db (util/flatten-path db option-path :followups) (fnil conj []) component))))

(defn followup-question-overlay [option-path]
  (let [layer-visible? (subscribe [:editor/followup-overlay option-path :visible?])
        followups      (subscribe [:editor/get-component-value (flatten [option-path :followups])])]
    (fn [option-path]
      (when (or @layer-visible? (not-empty @followups))
        [:div.editor-form__followup-question-overlay-parent
         [:div.editor-form__followup-question-overlay-outer
          [:div.editor-form__followup-indicator]
          [:div.editor-form__followup-indicator-inlay]
          [:div.editor-form__followup-question-overlay
           (into [:div]
             (for [[index followup] (map vector (range) @followups)]
               [ataru.virkailija.editor.core/soresu->reagent followup (flatten [option-path :followups index])]))
           [toolbar/followup-toolbar option-path
            (fn [generate-fn]
              (dispatch [:editor/generate-followup-component generate-fn option-path]))]]]]))))

(defn followup-question [option-path]
  (let [layer-visible?      (subscribe [:editor/followup-overlay option-path :visible?])
        followup-component  (subscribe [:editor/get-component-value (flatten [option-path :followups])])
        ; disallow nesting followup questions
        top-level-followup? (nil? ((set (flatten option-path)) :followups))]
    (fn [option-path]
      [:div.editor-form__followup-question
       (when top-level-followup?
         (match [@followup-component @layer-visible?]
           [nil true] [:a {:on-click #(dispatch [:editor/followup-overlay-close option-path])} "Lisäkysymykset"]
           [(_ :guard some?) _] "Lisäkysymykset"
           :else [:a {:on-click #(dispatch [:editor/followup-overlay-open option-path])} "Lisäkysymykset"]))])))
