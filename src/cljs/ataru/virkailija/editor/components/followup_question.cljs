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

(defn followup-question-overlay [option-index followups option-path show-followups]
  (when (get @show-followups option-index)
    [:div.editor-form__followup-question-overlay-parent
     [:div.editor-form__followup-question-overlay-outer
      [:div.editor-form__followup-indicator]
      [:div.editor-form__followup-indicator-inlay]
      [:div.editor-form__followup-question-overlay
       (into [:div] followups)
       [toolbar/followup-toolbar option-path
        (fn [generate-fn]
          (dispatch [:editor/generate-followup-component generate-fn option-path]))]]]]))

(defn followup-question [option-index followups option-path show-followups]
  (let [attrs {:on-click #(swap! show-followups
                                 (fn [v] (update v option-index not)))}]
    [:div.editor-form__followup-question
     (if (empty? followups)
       [:a attrs "Lisäkysymykset"]
       [:a attrs
        (str "Lisäkysymykset (" (count followups) ") ")
        (if (get @show-followups option-index)
          [:i.zmdi.zmdi-chevron-up.zmdi-hc-lg]
          [:i.zmdi.zmdi-chevron-down.zmdi-hc-lg])])]))
