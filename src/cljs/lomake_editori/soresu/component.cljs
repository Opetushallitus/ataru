(ns lomake-editori.soresu.component
  (:require [lomake-editori.soresu.components :refer [component]]
            [taoensso.timbre :refer-macros [spy]]))

(def form-component (:form-component component))

(spy (keys component))
