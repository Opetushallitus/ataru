(ns ataru.virkailija.editor.components.drag-n-drop-spacer
  (:require [re-frame.core :as re-frame]
            [ataru.cljs-util :as util :refer [get-virkailija-translation]]
            [reagent.core :as r]))

(defn drag-n-drop-spacer [path]
  (let [expanded?           (r/atom false)
        copy-component-path (re-frame/subscribe [:editor/copy-component-path])]
    (fn [path]
      [:div.editor-form__drag_n_drop_spacer_container_for_component
       {:on-mouse-over (fn [_] (when (some? @copy-component-path)
                                 (reset! expanded? true)))
        :on-mouse-out  (fn [_] (reset! expanded? false))}
       (when @expanded?
         [:div.editor-form__drag_n_drop_spacer--dashbox
          [:button.editor-form__move-component-button
           {:data-tooltip (get-virkailija-translation :paste-element)
            :on-click (fn [_] (when (and @expanded? (some? @copy-component-path))
                                (reset! expanded? false)
                                (re-frame/dispatch [:editor/copy-paste-component @copy-component-path path false])))}
           [:i.zmdi.zmdi-assignment-o]]
          [:button.editor-form__move-component-button
           {:data-tooltip (get-virkailija-translation :copy-element)
            :on-click (fn [_] (when (and @expanded? (some? @copy-component-path))
                                (reset! expanded? false)
                                (re-frame/dispatch [:editor/copy-paste-component @copy-component-path path true])))}
           [:i.zmdi.zmdi-copy]]])])))
