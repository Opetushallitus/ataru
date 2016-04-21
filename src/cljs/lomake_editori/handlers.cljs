(ns lomake-editori.handlers
    (:require [re-frame.core :as re-frame :refer [register-handler dispatch]]
              [ajax.core :refer [GET POST]]
              [lomake-editori.db :as db]
              [taoensso.timbre :refer-macros [spy]]))

(register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(register-handler
  ::hadle-get-forms
  (fn [db [_ forms-response]]
    (assoc-in db [:editor :forms] (:forms forms-response))))

(register-handler
  ::hadle-error
  (fn [db [_ error]]
    ;; TODO make a generic error message panel in UI which is shown when
    ;; :error-message exists and has a control for emptying it
    (assoc db :error-message "Lomakelistan hakeminen epäonnistui")))

(register-handler
  :fetch-initial-data
  (fn [db _]
    (GET
      "/api/forms"
      {:handler       #(dispatch [::hadle-get-forms %1])
       :error-handler #(dispatch [::hadle-error %1])
       :response-format :json
       :keywords? true})

    ;; update a flag in `app-db` ... presumably to trigger UI changes
    (assoc db :loading? true)))

(register-handler
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

