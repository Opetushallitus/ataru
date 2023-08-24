(ns ataru.virkailija.editor.components.drag-n-drop-spacer
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]))

(defn drag-n-drop-spacer [_]
  (let [expanded?         (r/atom false)
        copy-component    (re-frame/subscribe [:editor/copy-component])]
    (fn [path]
      [:div.editor-form__drag_n_drop_spacer_container_for_component
       {:on-mouse-over (fn [_] (when (some? @copy-component)
                                 (reset! expanded? true)))
        :on-mouse-out  (fn [_] (reset! expanded? false))}
       (when @expanded?
         [:div.editor-form__drag_n_drop_spacer--dashbox
          (if @(re-frame/subscribe [:editor/can-copy-or-paste?])
            [:button.editor-form__component-button
             {:on-click (fn [_] (when @expanded?
                                  (reset! expanded? false)
                                  (re-frame/dispatch [:editor/paste-component @copy-component path])))}
             @(re-frame/subscribe [:editor/virkailija-translation :paste-element])]
            [:button.editor-form__component-button
             {:disabled true}
             @(re-frame/subscribe [:editor/virkailija-translation :paste-element])])])])))
