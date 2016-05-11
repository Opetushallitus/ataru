(ns lomake-editori.soresu.component
  (:require [lomake-editori.soresu.components :refer [component]]
            [taoensso.timbre :refer-macros [spy]]))

(defonce form-component (:form-component component))
(defonce info-element (:info-element component))
