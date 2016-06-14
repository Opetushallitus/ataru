(ns ataru.virkailija.editor.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [clojure.data :refer [diff]]
            [cljs-time.core :as c]
            [cljs.core.match :refer-macros [match]]
            [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.dev.lomake :as dev]
            [ataru.virkailija.virkailija-ajax :refer [http post]]
            [ataru.virkailija.editor.handlers-macros :refer-macros [with-path-and-index]]
            [ataru.virkailija.routes :refer [set-history!]]
            [ataru.util :as util]
            [taoensso.timbre :refer-macros [spy debug]]
            [ataru.virkailija.temporal :as temporal]))

(defn refresh-forms []
  (http
    :get
    "/lomake-editori/api/forms"
    :handle-get-forms))

(defn get-user-info [db _]
  (http
    :get
    "/lomake-editori/api/user-info"
    :editor/handle-user-info)
  db)

(register-handler :editor/get-user-info get-user-info)

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

(defn- push-to-undo-stack [db-before db-after]
  (let [undo-limit              999
        selected-form-id-before (-> db-before :editor :selected-form-id)
        selected-form-id-after  (-> db-after :editor :selected-form-id)
        form-before             (get-in db-before [:editor :forms selected-form-id-before])
        form-after              (get-in db-after  [:editor :forms selected-form-id-after])]
    (if (not= selected-form-id-before
              selected-form-id-after)
      (assoc-in db-after [:editor :form-undodata] '())
      (update-in db-after [:editor :form-undodata]
                 (fn [undodata]
                   (seq (eduction
                          (comp (take undo-limit)
                                (filter some?))
                          (cons form-before
                                (or undodata
                                    '())))))))))

(register-handler
   :editor/do
  (fn [db-after [_ db-before]]
    (-> (push-to-undo-stack db-before db-after)
        ; Removes potential previously set undo boxes from ui
        (update :editor dissoc :forms-meta))))

(register-handler
  :editor/undo
  (fn [db _]
    (let [[form & xs]      (-> db :editor :form-undodata)
          selected-form-id (-> db :editor :selected-form-id)]
      (if (and (not-empty form)
               (= selected-form-id (:id form)))
        (-> db
            (assoc-in [:editor :form-undodata] xs)
            (update :editor dissoc :forms-meta)
            (assoc-in [:editor :forms selected-form-id] form))
        db))))

(register-handler
  :editor/clear-undo
  (fn [db _]
    (-> (update db :editor dissoc :form-undodata)
        (update :editor dissoc :forms-meta))))

(register-handler
  :editor/set-component-value
  (fn [db [_ value & path]]
    (assoc-in
      db
      (flatten [:editor :forms (-> db :editor :selected-form-id) :content [path]])
      value)))

(register-handler
  :handle-get-forms
  (fn [db [_ forms-response]]
    (if-let [forms (not-empty (:forms forms-response))]
      (assoc-in db [:editor :forms] (-> (util/group-by-first :id forms)
                                        (sorted-by-time)))
      db)))

(defn generate-component
  [db [_ generate-fn path]]
  (let [form-id       (get-in db [:editor :selected-form-id])
        path-vec      (flatten [:editor :forms form-id :content [path]])]
    (if (zero? (last path-vec))
      (assoc-in db (butlast path-vec) [(generate-fn)])
      (assoc-in db path-vec (generate-fn)))))

(register-handler :generate-component generate-component)

(defn remove-component
  [db path]
  (let [form-id      (get-in db [:editor :selected-form-id])
        remove-index (last path)
        path-vec     (-> [:editor :forms form-id :content [path]]
                         flatten
                         butlast)]
    (->> (get-in db path-vec)
         (keep-indexed (fn [index element]
                         (when-not (= index remove-index) element)))
         (into [])
         (assoc-in db path-vec))))

(def ^:private events
  ["webkitAnimationEnd" "mozAnimationEnd" "MSAnimationEnd" "oanimationend" "animationend"])

(register-handler
  :remove-component
  (fn [db [_ path dom-node]]
    (do
      (doseq [event events]
        (.addEventListener
          dom-node
          event
          #(do
             (dispatch [:editor/do db])
             (dispatch [:state-update
                        (fn [db_]
                          (-> (remove-component db_ path)
                              (update-in [:editor :forms-meta] assoc path :removed)))]))))

      (assoc-in db [:editor :forms-meta path] :fade-out))))

(register-handler
  :editor/handle-user-info
  (fn [db [_ user-info-response]]
    (assoc-in db [:editor :user-info] user-info-response)))

(register-handler
  :editor/refresh-forms
  (fn [db _]
    (autosave/stop-autosave! (-> db :editor :autosave))
    (refresh-forms)
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
  (fn [db [_ form-id]]
    (let [previous-form-id (-> db :editor :selected-form-id)]
      (do
        (when (not= previous-form-id form-id)
          (autosave/stop-autosave! (-> db :editor :autosave)))

        (dispatch [:editor/fetch-form-content form-id])

        (-> db
            (assoc-in [:editor :selected-form-id] form-id)
            (assoc-in [:editor :autosave]
                      (autosave/interval-loop {:subscribe-path [:editor :forms form-id]
                                               :changed-predicate
                                               (fn [current prev]
                                                 (match [current (merge {:content nil}
                                                                        prev)]
                                                        [_ {:content nil}]
                                                        false

                                                        :else
                                                        (not=
                                                          (dissoc prev :modified-time)
                                                          (dissoc current :modified-time))))
                                               :handler
                                               (fn [form previous-autosave-form]
                                                 (dispatch [:editor/save-form form]))})))))))

(defn save-form
  [db [_ form]]
  (let [with-iso-str-time (assoc form :modified-time (temporal/time->iso-str (:modified-time form)))]
    (post "/lomake-editori/api/form" with-iso-str-time
          (fn [db updated-form]
            (assoc-in db [:editor :forms (:id updated-form) :modified-time] (:modified-time updated-form))))
    db))

(register-handler :editor/save-form save-form)

(register-handler
  :editor/add-form
  (fn [db _]
    (post "/lomake-editori/api/form"
          {:name   "Uusi lomake"
           :content []}
          (fn [db new-or-updated-form]
            (autosave/stop-autosave! (-> db :editor :autosave))
            (set-history! (str "/editor/" (:id new-or-updated-form)))
            (assoc-in db [:editor :new-form-created?] true)))
    db))

(register-handler
  :editor/change-form-name
  (fn [db [_ new-form-name]]
    (let [selected-form-id (-> db :editor :selected-form-id)]
      (update-in db [:editor :forms selected-form-id]
                 assoc :name
                 new-form-name))))

(defn- remove-component-from-list
  [db source-path]
  (with-path-and-index [db source-path component-list-path remove-idx]
    (update-in db component-list-path
      (fn [components]
        (vec
          (concat
            (subvec components 0 remove-idx)
            (subvec components (inc remove-idx))))))))

(defn- add-component-to-list
  [db component target-path]
  (with-path-and-index [db target-path component-list-path add-idx]
    (update-in db component-list-path
      (fn [components]
        (vec
          (concat
            (subvec components 0 add-idx)
            [component]
            (subvec components add-idx)))))))

(defn move-component
  [db [_ source-path target-path]]
  (let [form-id   (get-in db [:editor :selected-form-id])
        component (get-in db (concat [:editor :forms form-id :content] source-path))]
    (-> db
        (remove-component-from-list source-path)
        (add-component-to-list component target-path))))

(register-handler :editor/move-component move-component)
