(ns ataru.virkailija.editor.component-macros)

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

(defmacro component-with-fade-effects
  [bindings component]
  `(let [path# ~(first bindings)]
     (reagent.core/create-class
       {:component-did-mount
                        (fn [this#]
                          (let [handler-fn# (animation-did-end-handler
                                              (re-frame.core/dispatch [:component-did-fade-in path#]))
                                target#     (reagent.core/dom-node this#)]
                            (doseq [event# ["webkitAnimationEnd" "mozAnimationEnd" "MSAnimationEnd" "oanimationend" "animationend"]]
                              (.addEventListener target# event# handler-fn#))))
        :reagent-render (fn [& args#]
                          (let [soresu-data# (first args#)
                                hiccup-form# (apply ~component args#)]
                            (if-let [status# (get-in soresu-data# [:params :status])]
                              (let [element# (first hiccup-form#)
                                    content# (subvec hiccup-form# 1)
                                    class#   (case status#
                                               "fading-out" "animated fadeOutUp"
                                               "fading-in"  "animated fadeInUp"
                                               "ready"      nil)]
                                (into [element# {:class class#}] content#))
                              hiccup-form#)))})))
