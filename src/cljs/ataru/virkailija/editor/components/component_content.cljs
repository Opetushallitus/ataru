(ns ataru.virkailija.editor.components.component-content
  (:require [re-frame.core :refer [subscribe]]
            [reagent.core :as r]))

(defn- component-fold-transition
  [node folded? state height]
  (cond (= [true :unfolded] [@folded? @state])
        ;; folding, calculate and set height
        (do (reset! height (.-scrollHeight node))
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
        content-node (r/atom nil)
        listener (unfold-ended-listener folded? state)]
    (r/create-class
      {:component-did-mount
       (fn [_]
         (.addEventListener @content-node
                            "transitionend"
                            listener)
         (component-fold-transition @content-node folded? state height))
       :component-will-unmount
       (fn [_]
         (.removeEventListener @content-node
                               "transitionend"
                               listener))
       :component-did-update
       (fn [_]
         (component-fold-transition @content-node folded? state height))
       :reagent-render
       (fn [_ content-component]
         (let [_ @folded?]
           (case @state
             :unfolded
             [:div.editor-form__component-content-wrapper
              {:ref #(reset! content-node %)}
              content-component]
             :set-height
             [:div.editor-form__component-content-wrapper
              {:ref #(reset! content-node %)
               :style {:height @height}}
              content-component]
             :folded
             [:div.editor-form__component-content-wrapper.editor-form__component-content-wrapper--folded
              {:ref #(reset! content-node %)}])))})))
