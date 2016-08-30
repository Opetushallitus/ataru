(ns ataru.virkailija.editor.editor-macros)

(defmacro with-form-key
  [bindings & body]
  `(let [~(second bindings) (get-in ~(first bindings) [:editor :selected-form-key])]
     ~@body))
