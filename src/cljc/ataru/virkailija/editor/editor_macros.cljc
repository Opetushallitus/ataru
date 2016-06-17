(ns ataru.virkailija.editor.editor-macros)

(defmacro with-form-id
  [bindings & body]
  `(let [~(second bindings) (get-in ~(first bindings) [:editor :selected-form-id])]
     ~@body))
