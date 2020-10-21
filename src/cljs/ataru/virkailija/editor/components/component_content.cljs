(ns ataru.virkailija.editor.components.component-content
  (:require [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [reagent.dom :as r-dom]))

(defn- component-fold-transition
  [component folded? state height]
  (cond (= [true :unfolded] [@folded? @state])
        ;; folding, calculate and set height
        (do (reset! height (.-scrollHeight (r-dom/dom-node component)))
            (reset! state :set-height))
        (= [true :set-height] [@folded? @state])
        ;; folding, render folded
        (reset! state :folded)
        (= [false :folded] [@folded? @state])
        ;; unfolding, set height
        (reset! state :set-height)))

(defn- unfold-ended-listener
  [folded? state]
  (fn [_]
    (when (= [false :set-height] [@folded? @state])
      ;; unfolding, render unfolded
      (reset! state :unfolded))))

(defn component-content
  [path _]
  (let [folded?  (subscribe [:editor/path-folded? path])
        state    (r/atom (if @folded?
                           :folded :unfolded))
        height   (r/atom nil)
        listener (unfold-ended-listener folded? state)]
    (r/create-class
      {:component-did-mount
       (fn [component]
         (.addEventListener (r-dom/dom-node component)
                            "transitionend"
                            listener)
         (component-fold-transition component folded? state height))
       :component-will-unmount
       (fn [component]
         (.removeEventListener (r-dom/dom-node component)
                               "transitionend"
                               listener))
       :component-did-update
       (fn [component]
         (component-fold-transition component folded? state height))
       :reagent-render
       (fn [_ content-component]
         (let [_ @folded?]
           (case @state
             :unfolded
             [:div.editor-form__component-content-wrapper
              content-component]
             :set-height
             [:div.editor-form__component-content-wrapper
              {:style {:height @height}}
              content-component]
             :folded
             [:div.editor-form__component-content-wrapper.editor-form__component-content-wrapper--folded])))})))
