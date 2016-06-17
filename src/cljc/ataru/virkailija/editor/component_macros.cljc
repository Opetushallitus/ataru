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
