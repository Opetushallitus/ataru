(ns ataru.hakija.application-readonly
  (:require [clojure.string :refer [trim]]
            [cljs.core.match :refer-macros [match]]))

(defn render-readonly-fields [form]
  [[:div "readonly"]])
