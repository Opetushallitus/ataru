(ns ataru.virkailija.editor.components.followup-question
  (:require
    [ataru.virkailija.component-data.component :as component]
    [cljs.core.match :refer-macros [match]]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [reagent.ratom :refer-macros [reaction]]
    [taoensso.timbre :refer-macros [spy debug]]))

(defn followup-question-overlay [path option-path]
  (fn [path option-path]
    [:div.editor-form__followup-editor-outer
     [:div.editor-form__followup-editor
      "layeri"]]))

(defn followup-question [path option-path]
  (let [layer-visible? (subscribe [:editor/followup-editor-overlay option-path :visible?])]
    [:div.editor-form__followup-questions
     (if @layer-visible?
       [:a {:on-click #(dispatch [:editor/followup-editor-overlay-close path option-path])} "Poista Lisäkysymys"]
       [:a {:on-click #(dispatch [:editor/followup-editor-overlay-open path option-path])} "Lisäkysymys"])]))
