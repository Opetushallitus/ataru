(ns ataru.virkailija.editor.components.drag-n-drop-spacer
  (:require [ataru.cljs-util :as util]
            [re-frame.core :as re-frame]
            [reagent.core :as r]))

; IE only allows this data attribute name for drag event dataTransfer
; http://stackoverflow.com/questions/26213011/html5-dragdrop-issue-in-internet-explorer-datatransfer-property-access-not-pos
(def ^:private ie-compatible-drag-data-attribute-name "Text")

(defn drag-n-drop-spacer [path]
  (let [expanded? (r/atom false)]
    (fn [path]
      [:div.editor-form__drag_n_drop_spacer_container_for_component
       {:on-drop       (fn [event]
                         (.preventDefault event)
                         (reset! expanded? false)
                         (let [source-path (-> event .-dataTransfer (.getData ie-compatible-drag-data-attribute-name) util/str->cljs)]
                           (re-frame/dispatch [:editor/move-component source-path path])))
        :on-drag-enter (fn [event] (.preventDefault event)) ;; IE needs this, otherwise on-drag-over doesn't occur
        :on-drag-over  (fn [event]
                         (.preventDefault event)
                         (reset! expanded? true)
                         nil)
        :on-drag-leave (fn [event]
                         (.preventDefault event)
                         (reset! expanded? false)
                         nil)}
       [:div
        {:class (if @expanded?
                  "editor-form__drag_n_drop_spacer--dashbox-visible"
                  "editor-form__drag_n_drop_spacer--dashbox-hidden")}
        [:div.editor-form__drag_n_drop_spacer--dashbox]]])))
