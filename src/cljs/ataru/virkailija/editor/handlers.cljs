(ns ataru.virkailija.editor.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [cljs-time.core :as c]
            [cljs.core.match :refer-macros [match]]
            [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.dev.lomake :as dev]
            [ataru.virkailija.handlers :refer [http post]]
            [ataru.util :as util]
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
    (if (seq (:forms forms-response))
      (let [mdb         (-> (assoc-in db [:editor :forms] (-> (util/group-by-first
                                                                :id (mapv with-author
                                                                          (:forms forms-response)))
                                                              (sorted-by-time)))
                            (update :editor dissoc :selected-form-id))
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
        mdb)
      db)))

(defn generate-component
  [db [_ generate-fn path]]
  (let [form-id       (get-in db [:editor :selected-form-id])
        path-vec      (flatten [:editor :forms form-id :content [path]])]
    (if (zero? (last path-vec))
      (assoc-in db (butlast path-vec) [(generate-fn)])
      (assoc-in db path-vec (generate-fn)))))

(register-handler :generate-component generate-component)

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
  :editor/fetch-form-content
  (fn [db [_ selected-form-id]]
    (http :get
          (str "/lomake-editori/api/forms/content/" selected-form-id)
          (fn [db response _]
            (update-in db
              [:editor :forms selected-form-id]
              merge
              (select-keys response [:content :modified-by :modified-time]))))
    db))

(register-handler
  :editor/select-form
  (fn [db [_ clicked-form]]
    (let [previous-form-id (-> db :editor :selected-form-id)]
      (do
        (when (not= previous-form-id (:id clicked-form))
          (autosave/stop-autosave! (-> db :editor :autosave)))

        (dispatch [:editor/fetch-form-content (:id clicked-form)])

        (-> db
            (assoc-in [:editor :forms (:id clicked-form)] clicked-form)
            (assoc-in [:editor :selected-form-id] (:id clicked-form))
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
                                               :initial        clicked-form})))))))

(register-handler
  :editor/save-form
  (fn [db [_ form]]
    (post "/lomake-editori/api/form"
          (dissoc form :modified-time))
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
    (let [selected-form-id (-> db :editor :selected-form-id)]
      (update-in db [:editor :forms selected-form-id]
                 assoc :name
                 new-form-name))))
