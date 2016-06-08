(ns ataru.virkailija.editor.component-macros)

(defmacro component-with-fade-effects
  [bindings & body]
  `(let [component# (do ~@body)
         status-path# [:params :status]
         fade-out-class# "animated fadeOutUp"]
     (if
       (= "fading-out" (get-in ~(first bindings) status-path#))
       (let [element# (first component#)
             content# (subvec component# 1)]
         (into [element# {:class fade-out-class#}] content#))
       component#)))
