(ns ataru.virkailija.editor.handlers-macros)

(defmacro with-path-and-index
  [bindings & body]
  `(let [form-key#             (get-in ~(first bindings) [:editor :selected-form-key])
         root-component-path# [:editor :forms form-key# :content]
         ~(bindings 2)        (if
                                (= 1 (count ~(second bindings)))
                                root-component-path#
                                (concat root-component-path# (butlast ~(second bindings))))
         ~(bindings 3)        (last ~(second bindings))]
     ~@body))
