(ns ataru.virkailija.editor.handlers-macros)

(defmacro with-path-and-index
  [bindings & body]
  `(let [form-id#             (get-in ~(bindings 0) [:editor :selected-form-id])
         root-component-path# [:editor :forms form-id# :content]
         ~(bindings 2)        (if
                                (= 1 (count ~(bindings 1)))
                                root-component-path#
                                (concat root-component-path# (butlast ~(bindings 1))))
         ~(bindings 3)        (last ~(bindings 1))]
     ~@body))
