(ns lomake-editori.handlers
    (:require [re-frame.core :as re-frame :refer [register-handler dispatch]]
              [ajax.core :refer [GET POST PUT DELETE]]
              [lomake-editori.db :as db]
              [lomake-editori.util :as util]
              [taoensso.timbre :refer-macros [spy]]))

(defn http [method path handler-or-dispatch & [override-args]]
  (let [f (case method
            :get    GET
            :post   POST
            :put    PUT
            :delete DELETE)]
    (dispatch [:set-state [:loading?] true])
    (f path
       (merge {:response-format :json
               :keywords?       true
               :error-handler   #(dispatch [:handle-error %])
               :finally         #(dispatch [:set-state [:loading?] false])
               :handler (if (keyword? handler-or-dispatch)
                            #(dispatch [handler-or-dispatch %])
                            handler-or-dispatch)}
              override-args))))

(register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(register-handler
  :set-state
  (fn [db [_ path args]]
    (assert (or (vector? path)
                (seq? path)))
    (if (map? args)
      (update-in db path merge args)
      (assoc-in db path args))))

(register-handler
  :handle-get-forms
  (fn [db [_ forms-response]]
    (-> (assoc-in db [:editor :forms] (util/group-by-first :id (:forms forms-response)))
        (update-in [:editor :forms] dissoc :selected-form))))

(register-handler
  :handle-error
  (fn [db [_ error]]
    ;; TODO make a generic error message panel in UI which is shown when
    ;; :error-message exists and has a control for emptying it
    (assoc db :error-message "Lomakelistan hakeminen epäonnistui")))

(register-handler
  :fetch-initial-data
  (fn [db _]
    (http
      :get
      "/lomake-editori/api/forms"
      :handle-get-forms)
    db))

(register-handler
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

