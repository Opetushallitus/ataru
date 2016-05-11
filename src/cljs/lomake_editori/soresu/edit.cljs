(ns lomake-editori.soresu.edit
  (:require [lomake-editori.soresu.components :refer [edit]]
            [taoensso.timbre :refer-macros [spy]]))

(defonce form-edit (:form-edit edit))
(defonce form-edit-component (:form-edit-component edit))
(defonce form-editor-controller (:form-editor-controller edit))
