(ns ataru.virkailija.editor.component-macros)

(defmacro component-with-fade-effects
  [bindings & body]
  `(let [component# (do ~@body)]
     (if-let [status# (get-in ~(first bindings) [:params :status])]
       (let [element# (first component#)
             content# (subvec component# 1)
             class#   (case status#
                        "fading-out" "animated fadeOutUp"
                        "fading-in"  "animated fadeInUp"
                        "ready"      nil)]
         (into [element# {:class class#}] content#))
       component#)))

(defmacro animation-did-end-handler
  [& body]
  `(let [listener-fn# (fn [event#]
                        (let [target# (.-target event#)]
                          (doseq [event# ["webkitAnimationEnd" "mozAnimationEnd" "MSAnimationEnd" "oanimationend" "animationend"]]
                            (->> (cljs.core/js-arguments)
                                 .-callee
                                 (.removeEventListener target# event#)))
                          ~@body))]
     listener-fn#))

(defmacro component-with-fade-in-effect
  [path component]
  `(reagent.core/create-class
     {:component-did-mount
                      (fn [this#]
                        (let [handler-fn# (animation-did-end-handler
                                            (re-frame.core/dispatch [:component-did-fade-in ~path]))
                              target#     (reagent.core/dom-node this#)]
                          (doseq [event# ["webkitAnimationEnd" "mozAnimationEnd" "MSAnimationEnd" "oanimationend" "animationend"]]
                            (.addEventListener target# event# handler-fn#))))
      :reagent-render ~component}))
