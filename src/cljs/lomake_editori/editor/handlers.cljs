(ns lomake-editori.editor.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [lomake-editori.handlers :refer [http]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn refresh-forms []
  (http
    :get
    "/lomake-editori/api/forms"
    :handle-get-forms))

(register-handler
  :editor/refresh-forms
  (fn [db _]
    (refresh-forms)
    db))
