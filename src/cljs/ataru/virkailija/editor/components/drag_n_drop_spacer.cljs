(ns ataru.virkailija.editor.components.drag-n-drop-spacer
  (:require [re-frame.core :as re-frame]
            [ataru.cljs-util :as util :refer [get-virkailija-translation]]
            [reagent.core :as r]))

(defn drag-n-drop-spacer [path]
  (let [expanded?         (r/atom false)
        copy-component    (re-frame/subscribe [:editor/copy-component])
        selected-form-key (re-frame/subscribe [:editor/selected-form-key])]
    (fn [path]
      [:div.editor-form__drag_n_drop_spacer_container_for_component
       {:on-mouse-over (fn [_] (when (some? @copy-component)
                                 (reset! expanded? true)))
        :on-mouse-out  (fn [_] (reset! expanded? false))}
       (when @expanded?
         (let [{copy-component-path     :copy-component-path
                copy-component-form-key :copy-component-form-key
                copy-component-paste?   :copy-component-paste?} @copy-component]
           [:div.editor-form__drag_n_drop_spacer--dashbox
            (if (= @selected-form-key copy-component-form-key)
              [:button.editor-form__move-component-button
               {
                :on-click     (fn [_] (when (and @expanded?)
                                        (reset! expanded? false)
                                        (re-frame/dispatch [:editor/copy-paste-component @copy-component path])))}
               (if copy-component-paste?
                 (get-virkailija-translation :paste-element)
                 (get-virkailija-translation :copy-element))]
              [:button.editor-form__move-component-button.editor-form__move-component-button--disabled
               {:data-tooltip (get-virkailija-translation :paste-element)}
               [:i.zmdi.zmdi-assignment-o]])]))])))
