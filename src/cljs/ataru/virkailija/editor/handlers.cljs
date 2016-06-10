(ns ataru.virkailija.editor.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [cljs-time.core :as c]
            [cljs.core.match :refer-macros [match]]
            [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.dev.lomake :as dev]
            [ataru.virkailija.virkailija-ajax :refer [http post]]
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

(register-handler
  :editor/set-component-value
  (fn [db [_ value & path]]
    (assoc-in db
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
  [db [_ path]]
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

(register-handler :remove-component remove-component)

(defn- component-status-handler
  [status]
  (fn [db [_ path]]
    (let [form-id   (get-in db [:editor :selected-form-id])
          path-vec  (flatten [:editor :forms form-id :content [path] :params :status])
          new-state (assoc-in db path-vec status)]
      new-state)))

(register-handler :hide-component (component-status-handler "fading-out"))

(register-handler :component-did-fade-in (component-status-handler "ready"))

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

(defn- remove-params
  [components]
  (let [update-child (fn [components]
                       (if
                         (contains? components :children)
                         (update components :children remove-params)
                         components))]
    (into
      []
      (map
        (fn [component]
          (let [remove-param (fn [component]
                               (if
                                 (contains? component :params)
                                 (update-in component [:params] #(dissoc % :status))
                                 component))
                filtered (-> component
                           remove-param
                           update-child)]
            filtered))
        components))))

(defn save-form
  [db [_ form]]
  (let [filtered-form (-> form
                          (assoc :modified-time (temporal/time->iso-str (:modified-time form)))
                          (update :content remove-params))]
    (post "/lomake-editori/api/form" filtered-form
          (fn [db updated-form]
            (assoc-in db [:editor :forms (:id updated-form) :modified-time] (:modified-time updated-form)))))
  db)

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
