(ns ataru.virkailija.editor.components.followup-question
  (:require
   [ataru.cljs-util :as util]
   [ataru.component-data.component :as component]
   [ataru.virkailija.editor.components.toolbar :as toolbar]
   [cljs.core.match :refer-macros [match]]
   [re-frame.core :refer [subscribe dispatch reg-sub reg-event-db reg-fx reg-event-fx]]
   [reagent.core :as r]
   [reagent.ratom :refer-macros [reaction]]
   [taoensso.timbre :refer-macros [spy debug]]
   [ataru.virkailija.temporal :as temporal]))

(reg-event-db
  :editor/generate-followup-component
  (fn [db [_ generate-fn option-path]]
    (let [user-info (-> db :editor :user-info)
          metadata  {:oid  (:oid user-info)
                     :name (:name user-info)
                     :date (temporal/datetime-now)}
          component (generate-fn {:created-by  metadata
                                  :modified-by metadata})]
      (update-in db (util/flatten-path db option-path :followups) (fnil conj []) component))))

(defn followup-question-overlay [option-index option-path show-followups]
  (let [followups (subscribe [:editor/get-component-value (flatten [option-path :followups])])]
    (fn [option-index option-path show-followups]
      (let [layer-visible? (get @show-followups option-index)]
        (when layer-visible?
          [:div.editor-form__followup-question-overlay-parent
           [:div.editor-form__followup-question-overlay-outer
            [:div.editor-form__followup-indicator]
            [:div.editor-form__followup-indicator-inlay]
            [:div.editor-form__followup-question-overlay
             (into [:div]
               (for [[index followup] (map vector (range) @followups)]
                 [ataru.virkailija.editor.core/soresu->reagent followup (vec (flatten [option-path :followups index]))]))
             [toolbar/followup-toolbar option-path
              (fn [generate-fn]
                (dispatch [:editor/generate-followup-component generate-fn option-path]))]]]])))))

(defn followup-question [option-index option-path show-followups]
  (let [followup-component (subscribe [:editor/get-component-value (vec (flatten [option-path :followups]))])]
    (fn [option-index option-path show-followups]
      [:div.editor-form__followup-question
       (let [layer-visible? (get @show-followups option-index)
             followups?     (not-empty @followup-component)]
         [:a
          {:on-click #(swap! show-followups
                        (fn [v] (assoc v option-index
                                         (not (get v option-index)))))}
          (when followups? (str "Lisäkysymykset (" (count @followup-component) ") "))
          (if followups?
            (if layer-visible?
              [:i.zmdi.zmdi-chevron-up.zmdi-hc-lg]
              [:i.zmdi.zmdi-chevron-down.zmdi-hc-lg])
            "Lisäkysymykset")])])))
