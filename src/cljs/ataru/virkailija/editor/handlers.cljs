(ns ataru.virkailija.editor.handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [clojure.data :refer [diff]]
            [clojure.walk :as walk]
            [cljs-time.core :as c]
            [cljs.core.async :as async]
            [cljs.core.match :refer-macros [match]]
            [ataru.virkailija.component-data.component :as component]
            [ataru.virkailija.component-data.person-info-module :as pm]
            [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.dev.lomake :as dev]
            [ataru.virkailija.editor.editor-macros :refer-macros [with-form-id]]
            [ataru.virkailija.editor.handlers-macros :refer-macros [with-path-and-index]]
            [ataru.virkailija.routes :refer [set-history!]]
            [ataru.virkailija.virkailija-ajax :refer [http post dispatch-flasher-error-msg]]
            [ataru.util :as util]
            [taoensso.timbre :refer-macros [spy debug]]
            [ataru.virkailija.temporal :as temporal])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

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

(defn- remove-nth
  "remove nth elem in vector"
  [v n]
  (vec (concat (subvec v 0 n) (subvec v (inc n)))))

(defn- current-form-content-path
  [db further-path]
  (flatten [:editor :forms (-> db :editor :selected-form-id) :content [further-path]]))

(register-handler
  :editor/remove-dropdown-option
  (fn [db [_ & path]]
    (let [option-path (current-form-content-path db [path])]
      (update-in db (drop-last option-path) remove-nth (last option-path)))))

(register-handler
  :editor/add-dropdown-option
  (fn [db [_ & path]]
    (let [dropdown-path (current-form-content-path db [path :options])]
      (update-in db dropdown-path into [(ataru.virkailija.component-data.component/dropdown-option)]))))

(register-handler
  :editor/set-dropdown-option-value
  (fn [db [_ value & path]]
    (let [label-path (current-form-content-path db [path])
          this-option-path (drop-last 2 label-path)
          value-path (flatten [this-option-path :value])
          option-updated-db (-> db
                                (assoc-in label-path value)
                                (assoc-in value-path value))]
      option-updated-db)))

(defn add-validator
  [db [_ validator & path]]
  (let [path (current-form-content-path db [path :validators])]
    (update-in db path (fn [validators]
                         (when-not (some #(= % validator) validators)
                           (conj validators validator))))))

(register-handler :editor/add-validator add-validator)

(defn remove-validator
  [db [_ validator & path]]
  (let [path       (current-form-content-path db [path :validators])
        validators (get-in db path)]
    (if-not (nil? validators)
      (update-in db path (fn [validators]
                           (remove #(= % validator) validators)))
      db)))

(register-handler :editor/remove-validator remove-validator)

(register-handler
  :editor/set-component-value
  (fn [db [_ value & path]]
    (assoc-in db (current-form-content-path db [path]) value)))

(defn generate-component
  [db [_ generate-fn path]]
  (with-form-id [db form-id]
    (let [form-id       (get-in db [:editor :selected-form-id])
          path-vec      (current-form-content-path db [path])
          component     (generate-fn)]
      (if (zero? (last path-vec))
        (assoc-in db (butlast path-vec) [component])
        (assoc-in db path-vec component)))))

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
         (dispatch [:state-update
                    (fn [db_]
                      (-> (remove-component db_ path)
                          (update-in [:editor :forms-meta] assoc path :removed)))])))
    (assoc-in db [:editor :forms-meta path] :fade-out)))

(register-handler
  :editor/handle-user-info
  (fn [db [_ user-info-response]]
    (assoc-in db [:editor :user-info] user-info-response)))

(defn- languages->kwd [form]
  (update form :languages
    (partial mapv keyword)))

(defn refresh-forms []
  (http
    :get
    "/lomake-editori/api/forms"
    (fn [db {:keys [forms]}]
      (assoc-in db [:editor :forms] (->> forms
                                         (mapv languages->kwd)
                                         (util/group-by-first :id)
                                         (sorted-by-time))))))

(register-handler
  :editor/refresh-forms
  (fn [db _]
    (autosave/stop-autosave! (-> db :editor :autosave))
    (refresh-forms)
    (update db :editor dissoc :forms)))

(defn fetch-form-content! [form-id]
  (http :get
        (str "/lomake-editori/api/forms/" form-id)
        (fn [db response _]
          (let [response (languages->kwd response)]
          (->
            (update-in db
                       [:editor :forms form-id]
                       merge
                       (select-keys response [:content :modified-by :modified-time :languages]))
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
                                                 (dispatch [:editor/save-form]))})))))))

(register-handler
  :editor/select-form
  (fn [db [_ form-id]]
    (with-form-id [db previous-form-id]
      (do
        (when (not= previous-form-id form-id)
          (autosave/stop-autosave! (-> db :editor :autosave)))
        (fetch-form-content! form-id)
        (assoc-in db [:editor :selected-form-id] form-id)))))

(defn- remove-empty-options
  [options]
  (vec (remove #(clojure.string/blank? (:value %)) options)))

(defn- add-empty-option
  [options]
  (into [(component/dropdown-option)] options))

(defn- update-options-in-dropdown-field
  [dropdown-field no-blank-option?]
  (let [add-blank-fn (if no-blank-option? identity add-empty-option)
        updated-options (-> (:options dropdown-field)
                            (remove-empty-options)
                            (add-blank-fn))]
    (merge dropdown-field {:options updated-options})))

(defn- update-dropdown-field-options
  [form]
  (let [new-content
        (walk/prewalk
          #(if (and (= (:fieldType %) "dropdown") (= (:fieldClass %) "formField"))
            (update-options-in-dropdown-field % (:no-blank-option %))
            %)
          (:content form))]
    (merge form {:content new-content})))

(defn- set-modified-time
  [form]
  (assoc-in form [:modified-time] (temporal/time->iso-str (:modified-time form))))

(defn- remove-focus
  [form]
  (clojure.walk/prewalk
    (fn [x]
      (if (map? x)
        (dissoc x :focus?)
        x))
    form))

(def save-chan (async/chan (async/sliding-buffer 1)))
(def response-chan (async/chan))

(register-handler :editor/handle-response-sync
  (fn [db [_ response {:keys [response-chan]}]]
    (async/put! response-chan response)
    db))

(defn save-loop [save-chan response-chan]
  (let [modified-time (atom nil)]
    (go-loop [form (async/<! save-chan)]
      (when (nil? @modified-time)
        (reset! modified-time (:modified-time form)))
      (let [form (-> form
                     (assoc :modified-time @modified-time)
                     (set-modified-time))]
        (post "/lomake-editori/api/forms" form :editor/handle-response-sync
          :handler-args  {:response-chan response-chan}
          :override-args {:error-handler (fn [error]
                                           (async/put! response-chan false)
                                           (dispatch-flasher-error-msg :post error))})
        (let [updated-form (async/<! response-chan)]
          (when-not (false? updated-form)
            (reset! modified-time (:modified-time updated-form))
            (dispatch [:state-update (fn [db]
                                       (assoc-in db [:editor :forms (:id updated-form) :modified-time] (:modified-time updated-form)))]))
          (recur (async/<! save-chan)))))))

(save-loop save-chan response-chan)

(defn save-form
  [db _]
  (let [form (-> (get-in db [:editor :forms (-> db :editor :selected-form-id)])
                 (update-dropdown-field-options)
                 (remove-focus))]
    (when (not-empty (:content form))
      (async/put! save-chan form))
    db))

(register-handler :editor/save-form save-form)

(register-handler
  :editor/add-form
  (fn [db _]
    (post "/lomake-editori/api/forms"
          {:name      "Uusi lomake"
           :content   [(pm/person-info-module)]
           :languages [:fi]}
          (fn [db new-or-updated-form]
            (let [new-or-updated-form (languages->kwd new-or-updated-form)]
              (autosave/stop-autosave! (-> db :editor :autosave))
              (set-history! (str "/editor/" (:id new-or-updated-form)))
              (assoc-in db [:editor :new-form-created?] true))))
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

(defn- recalculate-target-path-prevent-oob
  [source-path target-path]
  (match [source-path target-path]
    ; moving-top-level components
    [[n :guard integer?] [nn :guard integer?]]
    (or (when (> nn n)
          [(dec nn)])
      [(max 0 nn)])

    [[a :children xa] [b :children xb]]
    (or
      (when (and (> xb xa) (= a b))
       [b :children (dec xb)])  ; moving within same component-group, index out of bounds prevention
      [b :children (max 0 xb)]) ; moving between component-groups

    ; moving component from root-level into a component-group
    [[a] [b :children xb]]
    (if (spy (-> b (< a)))
      [b :children xb]       ; topwards
      [(dec b) :children xb] ; bottomwards
      )

    :else target-path))

(defn move-component
  [db [_ source-path target-path]]
  (with-form-id [db form-id]
    (let [component                (get-in db (concat [:editor :forms form-id :content] source-path))
          recalculated-target-path (recalculate-target-path-prevent-oob source-path target-path)
          result-is-nested-component-group? (and
                                              (contains?
                                                (set recalculated-target-path) :children)
                                              (= "wrapperElement" (:fieldClass component)))]
      (if result-is-nested-component-group?
        db ; Nesting is not allowed/supported
        (-> db
          (remove-component-from-list source-path)
          (add-component-to-list component recalculated-target-path))))))

(register-handler :editor/move-component move-component)

(def ^:private lang-order
  [:fi :sv :en])

(defn- index-of [coll t]
  (let [length (count coll)]
    (loop [idx 0]
      (cond
        (<= length idx) -1
        (= (get coll idx) t) idx
        :else (recur (inc idx))))))

(register-handler :editor/toggle-language
  (fn [db [_ lang]]
    (let [lang-path [:editor :forms (get-in db [:editor :selected-form-id]) :languages]]
      (update-in db lang-path
        (fn [languages]
          (let [languages (or languages [:fi])]
            (cond
              (not (some #{lang} languages)) (sort-by (partial index-of lang-order)
                                                      (conj languages lang))
              (> (count languages) 1)        (filter (partial not= lang) languages)
              :else                          languages)))))))
