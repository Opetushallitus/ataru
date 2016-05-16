(ns lomake-editori.editor.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [cljs-time.core :as c]
            [cljs.core.match :refer-macros [match]]
            [lomake-editori.autosave :as autosave]
            [lomake-editori.dev.lomake :as dev]
            [lomake-editori.handlers :refer [http post]]
            [lomake-editori.temporal :refer [coerce-timestamp]]
            [lomake-editori.util :as util]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn refresh-forms
  ([] (http
        :get
        "/lomake-editori/api/forms"
        :handle-get-forms))
  ([selected-form-id] (http
                        :get
                        "/lomake-editori/api/forms"
                        :handle-get-forms
                        :handler-args selected-form-id)))

(defn get-user-info [db _]
  (http
    :get
    "/lomake-editori/api/user-info"
    :editor/handle-user-info)
  db)

(register-handler :editor/get-user-info get-user-info)

(defn with-author [form]
  (assoc form :author {:last "Turtiainen" :first "Janne"}))

(defn sorted-by-time [m]
  (into (sorted-map-by
          (fn [k1 k2]
            (let [v1 (-> (get m k1) :modified-time)
                  v2 (-> (get m k2) :modified-time)]
              (match [v1 v2]
                     [nil nil] 0
                     [_   nil] 1
                     [nil   _] -1
                     :else (c/after? v1 v2)))))
        m))

(register-handler
  :handle-get-forms
  (fn [db [_ forms-response selected-form-id]]
    (let [mdb         (-> (assoc-in db [:editor :forms] (-> (util/group-by-first
                                                              :id (mapv (comp with-author
                                                                              (coerce-timestamp :modified-time))
                                                                        (:forms forms-response)))
                                                            (sorted-by-time)))
                          (update-in [:editor] dissoc :selected-form))
          forms       (-> mdb :editor :forms)
          active-form (or
                        (->> forms
                          (filter #(= selected-form-id (:id (second %))))
                          first
                          second)
                        (->> forms
                          first
                          second))]
      (dispatch [:editor/select-form active-form])
      mdb)))

(register-handler
  :generate-component
  (fn [db [_ generate-fn path]]
    (let [form-id  (get-in db [:editor :selected-form :id])
          path-vec (if
                     (coll? path)
                     path
                     [path])
          new-form (-> db
                     (get-in [:editor :forms form-id :content])
                     (assoc-in path-vec (generate-fn)))]
      (-> db
        (assoc-in [:editor :selected-form :content] new-form)
        (assoc-in [:editor :forms form-id :content] new-form)))))

(register-handler
  :editor/handle-user-info
  (fn [db [_ user-info-response]]
    (assoc-in db [:editor :user-info] user-info-response)))

(register-handler
  :editor/refresh-forms
  (fn [db [_ selected-form-id]]
    (autosave/stop-autosave! (-> db :editor :autosave))
    (refresh-forms selected-form-id)
    db))

(register-handler
  :editor/select-form
  (fn [db [_ clicked-form]]
    (let [previous-form (-> db :editor :selected-form)
          clicked-form-with-content (merge clicked-form dev/placeholder-content)]
      (when (not= (:id previous-form) (:id clicked-form))
       (autosave/stop-autosave! (-> db :editor :autosave)))

      (-> db
          (assoc-in [:editor :forms (:id clicked-form)] clicked-form-with-content)
          (assoc-in [:editor :selected-form] clicked-form-with-content)
          (assoc-in [:editor :autosave]
                    (autosave/interval-loop {:subscribe-path [:editor :forms (:id clicked-form)]
                                             :changed-predicate
                                             (fn [current prev]
                                               (not=
                                                 (dissoc prev :modified-time :content)
                                                 (dissoc current :modified-time :content)))
                                             :handler
                                             (fn [form previous-autosave-form]
                                               (dispatch [:editor/save-form form]))
                                             :initial clicked-form-with-content}))))))

(defn- callback-after-post [db new-or-updated-form]
  (let [form-with-time (-> ((coerce-timestamp :modified-time) new-or-updated-form)
                           (assoc :author {:last  "Testaaja" ;; placeholder
                                           :first "Teppo"}))]
    (assoc-in db [:editor :forms (:id form-with-time)] form-with-time)))

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
            (autosave/stop-autosave! (-> db :editor :autosave))
            (refresh-forms)
            db))
    db))

(register-handler
  :editor/change-form-name
  (fn [db [_ new-form-name]]
    (let [selected-form (-> db :editor :selected-form)]
      (update-in db [:editor :forms (:id selected-form)]
                 assoc :name
                 new-form-name))))
