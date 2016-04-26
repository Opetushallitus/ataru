(ns lomake-editori.editor.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [lomake-editori.handlers :refer [http]]
            [lomake-editori.temporal :refer [coerce-timestamp]]
            [lomake-editori.handlers :refer [post]]
            [cljs.core.match :refer-macros [match]]
            [goog.date :as gd]
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

(register-handler
  :editor/select-form
  (fn [db [_ clicked-form]]
    (assoc-in db [:editor :selected-form]
              clicked-form)))

(register-handler
  :editor/add-form
  (fn [db _]
    (post "/lomake-editori/api/form"
          {:name   "Uusi lomake"
           :author {:last  "Testaaja" ;; placeholder
                    :first "Teppo"}}
          (fn [db new-form]
            (let [form-with-time (-> ((coerce-timestamp :modified-time) new-form)
                                     (assoc :author {:last  "Testaaja" ;; placeholder
                                                     :first "Teppo"}))]
              (-> db
                  (assoc-in [:editor :selected-form] form-with-time)
                  (assoc-in [:editor :forms (:id form-with-time)] form-with-time)))))
    db))

(register-handler
  :editor/change-form-name
  (fn [db [_ new-form-name]]
    (let [selected-form (-> db :editor :selected-form)
          name-before-edit (:name selected-form)]
      (update-in db [:editor :forms (:id selected-form)]
                 assoc :name
                 (if (empty? new-form-name)
                   name-before-edit
                   new-form-name)))))

