(ns lomake-editori.editor.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [lomake-editori.handlers :refer [http]]
            [lomake-editori.temporal :refer [coerce-timestamp]]
            [lomake-editori.handlers :refer [post]]
            [lomake-editori.autosave :as autosave]
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
    (let [previous-form (-> db :editor :selected-form)]
      (when (not= (:id previous-form) (:id clicked-form))
        (autosave/stop-autosave! (-> db :editor :autosave)))

      (-> db
          (assoc-in [:editor :selected-form] clicked-form)
          (assoc-in [:editor :autosave]
                    (autosave/interval-loop {:subscribe-path [:editor :forms (:id clicked-form)]
                                             :changed-predicate
                                             (fn [current prev]
                                               (not=
                                                 (dissoc prev :modified-time)
                                                 (dissoc current :modified-time)))
                                             :handler
                                             (fn [form previous-autosave-form]
                                               (dispatch [:editor/save-form form]))
                                             :initial clicked-form}))))))

(defn- callback-after-post [db new-or-updated-form]
  (let [form-with-time (-> ((coerce-timestamp :modified-time) new-or-updated-form)
                           (assoc :author {:last  "Testaaja" ;; placeholder
                                           :first "Teppo"}))]
    (-> db
        ; take note :selected-form is not watched/subscribed by autosave or the ui
        ; it is only changed here to keep it in sync with [:editor :forms]
        ; changes to selected-form should be done via (dispatch [:editor/select-form form])
        (assoc-in [:editor :selected-form] form-with-time)
        (assoc-in [:editor :forms (:id form-with-time)] form-with-time))))

(register-handler
  :editor/save-form
  (fn [db [_ form]]
    (post "/lomake-editori/api/form"
          form
          callback-after-post)
    db))

(register-handler
  :editor/add-form
  (fn [db _]
    (post "/lomake-editori/api/form"
          {:name   "Uusi lomake"
           :author {:last  "Testaaja" ;; placeholder
                    :first "Teppo"}}
          (fn [db new-or-updated-form]
            (refresh-forms)
            db))
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
