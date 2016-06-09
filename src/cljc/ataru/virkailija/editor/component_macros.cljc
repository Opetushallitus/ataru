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
