(ns ataru.async-macros)

(defmacro <?
  [c]
  `(let [x# (#?(:cljs clojure.core.async/<!
                :clj cljs.core.async/<!)
             ~c)]
     (if (instance? #?(:cljs Exception :clj js/Error) x#)
       (throw x#)
       x#)))

(defmacro go-try
  [& body]
  `(#?(:cljs clojure.core.async/go
       :clj cljs.core.async.macros/go)
    (try ~@body
         (catch #?(:cljs Exception :clj js/Error) e# e#))))
