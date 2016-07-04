(ns ataru.virkailija.editor.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [clojure.data :refer [diff]]
            [clojure.walk :as walk]
            [cljs-time.core :as c]
            [cljs.core.match :refer-macros [match]]
            [ataru.virkailija.soresu.component :as component]
            [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.dev.lomake :as dev]
            [ataru.virkailija.editor.editor-macros :refer-macros [with-form-id]]
            [ataru.virkailija.editor.handlers-macros :refer-macros [with-path-and-index]]
            [ataru.virkailija.handlers :refer [fetch-application-counts!]]
            [ataru.virkailija.routes :refer [set-history!]]
            [ataru.virkailija.virkailija-ajax :refer [http post]]
            [ataru.util :as util]
            [taoensso.timbre :refer-macros [spy debug]]
            [ataru.virkailija.temporal :as temporal]))

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
        form-before             (get-in db-before [:editor :forms selected-form-id-before])]
    (if (not= selected-form-id-before
              selected-form-id-after)
      (assoc-in db-after [:editor :form-undodata] '())
      (update-in db-after [:editor :form-undodata]
                 (fn [undodata]
                   (seq (eduction
                          (comp (take undo-limit)
                                (filter some?))
                          (cons form-before
                                undodata))))))))

(register-handler
   :editor/do
  (fn [db-after [_ db-before]]
    (-> (push-to-undo-stack db-before db-after)
        ; Removes potential previously set undo boxes from ui
        (update :editor dissoc :forms-meta))))

(register-handler
  :editor/undo
  (fn [db _]
    (with-form-id [db selected-form-id]
      (let [[form & xs]      (-> db :editor :form-undodata)
            modified-time    (get-in db [:editor :forms selected-form-id :modified-time])]
        (if (and (not-empty form)
              (= selected-form-id (:id form)))
          (-> db
              (assoc-in [:editor :form-undodata] xs)
              (update :editor dissoc :forms-meta)
              (assoc-in [:editor :forms selected-form-id]
                (assoc form :modified-time modified-time)))
          db)))))

(register-handler
  :editor/clear-undo
  (fn [db _]
    (-> (update db :editor dissoc :form-undodata)
        (update :editor dissoc :forms-meta))))

(defn- empty-options-in-select?
  [options]
  (some #(clojure.string/blank? (:value %)) options))

(defn- remove-nth
  "remove nth elem in vector"
  [v n]
  (vec (concat (subvec v 0 n) (subvec v (inc n)))))

(defn- remove-option
  [db path]
  (update-in db (drop-last path) remove-nth (last path)))

(defn- add-option
  [db path]
  (update-in db path into [(ataru.virkailija.soresu.component/dropdown-option)]))

(register-handler
  :editor/set-dropdown-option-value
  (fn [db [_ value & path]]
    (let [label-path (flatten [:editor :forms (-> db :editor :selected-form-id) :content [path]])
          this-option-path (drop-last 2 label-path)
          options-path (drop-last 3 label-path)
          value-path (flatten [this-option-path :value])
          option-updated-db (-> db
                                (assoc-in label-path value)
                                (assoc-in value-path value))
          blank-removed-db (if (clojure.string/blank? (:value (get-in option-updated-db this-option-path)))
                             (remove-option option-updated-db this-option-path)
                             option-updated-db)
          blank-added-db (if (not (empty-options-in-select? (get-in blank-removed-db options-path)))
                           (add-option blank-removed-db options-path)
                           blank-removed-db)]
      blank-added-db)))

(register-handler
  :editor/set-component-value
  (fn [db [_ value & path]]
    (assoc-in
      db
      (flatten [:editor :forms (-> db :editor :selected-form-id) :content [path]])
      value)))

(defn generate-component
  [db [_ generate-fn path]]
  (with-form-id [db form-id]
    (let [form-id       (get-in db [:editor :selected-form-id])
          path-vec      (flatten [:editor :forms form-id :content [path]])]
      (if (zero? (last path-vec))
        (assoc-in db (butlast path-vec) [(generate-fn)])
        (assoc-in db path-vec (generate-fn))))))

(register-handler :generate-component generate-component)

(defn remove-component
  [db path]
  (with-form-id [db form-id]
    (let [remove-index (last path)
          path-vec     (-> [:editor :forms form-id :content [path]]
                         flatten
                         butlast)]
      (->> (get-in db path-vec)
           (keep-indexed (fn [index element]
                           (when-not (= index remove-index) element)))
           (into [])
           (assoc-in db path-vec)))))

(register-handler
  :remove-component
  (fn [db [_ path dom-node]]
    (.addEventListener
      dom-node
      "animationend"
      #(do
         (.removeEventListener (.-target %) "animationend" (-> (cljs.core/js-arguments) .-callee))
         (dispatch [:editor/do db])
         (dispatch [:state-update
                    (fn [db_]
                      (-> (remove-component db_ path)
                          (update-in [:editor :forms-meta] assoc path :removed)))])))
    (assoc-in db [:editor :forms-meta path] :fade-out)))

(register-handler
  :editor/handle-user-info
  (fn [db [_ user-info-response]]
    (assoc-in db [:editor :user-info] user-info-response)))

(defn refresh-forms []
  (http
    :get
    "/lomake-editori/api/forms"
    (fn [db {:keys [forms]}]
      (assoc-in db [:editor :forms] (-> (util/group-by-first :id forms)
                                        (sorted-by-time))))))

(register-handler
  :editor/refresh-forms
  (fn [db _]
    (autosave/stop-autosave! (-> db :editor :autosave))
    (refresh-forms)
    (update db :editor dissoc :forms)))

(defn fetch-form-content! [form-id]
  (http :get
        (str "/lomake-editori/api/forms/content/" form-id)
        (fn [db response _]
          (->
            (update-in db
                       [:editor :forms form-id]
                       merge
                       (select-keys response [:content :modified-by :modified-time]))
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
                                                 (dispatch [:editor/save-form]))}))))))

(register-handler
  :editor/fetch-form-content
  (fn [db [_ selected-form-id]]
    (fetch-form-content! selected-form-id)
    db))

(register-handler
  :editor/select-form
  (fn [db [_ form-id]]
    (with-form-id [db previous-form-id]
      (do
        (when (not= previous-form-id form-id)
          (autosave/stop-autosave! (-> db :editor :autosave)))
        (fetch-form-content! form-id)
        (fetch-application-counts! form-id)
        (assoc-in db [:editor :selected-form-id] form-id)))))

(defn- remove-empty-options
  [options]
  (vec (remove #(clojure.string/blank? (:value %)) options)))

(defn- add-empty-option
  [options]
  (into [(component/dropdown-option)] options))

(defn- update-options-in-dropdown-field
  [dropdown-field]
  (let [updated-options (-> (:options dropdown-field)
                            (remove-empty-options)
                            (add-empty-option))]
    (merge dropdown-field {:options updated-options})))

(defn- update-dropdown-field-options
  [form]
  (let [new-content
        (walk/prewalk
          #(if (and (= (:fieldType %) "dropdown") (= (:fieldClass %) "formField"))
            (update-options-in-dropdown-field %)
            %)
          (:content form))]
    (merge form {:content new-content})))

(defn- set-modified-time
  [form]
  (assoc-in form [:modified-time] (temporal/time->iso-str (:modified-time form))))

(defn save-form
  [db _]
  (let [form (-> (get-in db [:editor :forms (-> db :editor :selected-form-id)])
                 (set-modified-time)
                 (update-dropdown-field-options))]
    (when (not-empty (:content form))
      (post
        "/lomake-editori/api/form"
        form
        (fn [db updated-form]
          (assoc-in db [:editor :forms (:id updated-form) :modified-time] (:modified-time updated-form)))))
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
    (with-form-id [db selected-form-id]
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

(defn- alter-component-index?
  [source-path target-path]
  (let [target-index (last target-path)
        fixed-target-path (if-not
                            (or
                              (= 0 target-index)
                              (< 1 (count source-path)))
                            (assoc target-path (dec (count target-path)) (dec target-index))
                            target-path)]
      fixed-target-path))

(defn- recalculate-target-path
  [source-path target-path]
  (let [altered-target-path (alter-component-index? source-path target-path)]
    (if (and
          (= 1 (count source-path))
          (< 1 (count altered-target-path))
          (< (source-path 0) (altered-target-path 0)))
        (into
          [(dec (altered-target-path 0))]
          (rest altered-target-path))
        altered-target-path)))

(defn move-component
  [db [_ source-path target-path]]
  (with-form-id [db form-id]
    (let [component                (get-in db (concat [:editor :forms form-id :content] source-path))
          recalculated-target-path (recalculate-target-path source-path target-path)
          result-is-nested-component-group (and (contains? (set recalculated-target-path) :children) (= "wrapperElement" (:fieldClass component)))]
      (if result-is-nested-component-group
        db ; Nesting is not allowed/supported
        (-> db
          (remove-component-from-list source-path)
          (add-component-to-list component recalculated-target-path))))))

(register-handler :editor/move-component move-component)
