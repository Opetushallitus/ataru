(ns ataru.virkailija.editor.handlers
  (:require [re-frame.core :refer [reg-event-db reg-event-fx dispatch dispatch-sync subscribe]]
            [clojure.data :refer [diff]]
            [clojure.walk :as walk]
            [cljs-time.core :as c]
            [cljs.core.async :as async]
            [cljs.core.match :refer-macros [match]]
            [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.component-data.component :as component]
            [ataru.virkailija.component-data.person-info-module :as pm]
            [ataru.virkailija.dev.lomake :as dev]
            [ataru.virkailija.editor.components.followup-question :as followup]
            [ataru.virkailija.editor.editor-macros :refer-macros [with-form-key]]
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

(reg-event-db :editor/get-user-info get-user-info)

(defn- sorted-by-time-and-deletedness [m]
  (into (sorted-map-by
          (fn [k1 k2]
            (let [v1 (get m k1)
                  v2 (get m k2)
                  v1-deleted? (:deleted v1)
                  v2-deleted? (:deleted v2)
                  v1-created (:created-time v1)
                  v2-created (:created-time v2)]
              (cond
                (and v1-deleted? (not v2-deleted?)) 1
                (and v2-deleted? (not v1-deleted?)) -1
                (and (nil? v1-created) (nil? v2-created)) 0
                (nil? v2-created) 1
                (nil? v1-created) -1
                :else (c/after? v1-created v2-created)))))
        m))

(defn- remove-nth
  "remove nth elem in vector"
  [v n]
  (vec (concat (subvec v 0 n) (subvec v (inc n)))))

(defn- current-form-content-path
  [db further-path]
  (flatten [:editor :forms (-> db :editor :selected-form-key) :content [further-path]]))

(reg-event-fx
  :editor/remove-dropdown-option
  (fn [cofx [_ & path]]
    (let [option-path (current-form-content-path (:db cofx) [path])]
      {:db (update-in (:db cofx) (drop-last option-path) remove-nth (last option-path))
       :dispatch [:editor/remove-followup-question (first path)]})))

(reg-event-db
  :editor/add-dropdown-option
  (fn [db [_ & path]]
    (let [dropdown-path (current-form-content-path db [path :options])
          component     (ataru.virkailija.component-data.component/dropdown-option)]
      (->
        (update-in db dropdown-path into [component])
        (assoc-in [:editor :ui (:id component) :focus?] true)))))

(reg-event-db
  :editor/set-dropdown-option-value
  (fn [db [_ value & path]]
    (let [label-path (current-form-content-path db [path])
          this-option-path (drop-last 2 label-path)
          value-path (flatten [this-option-path :value])
          option-updated-db (-> db
                                (assoc-in label-path value)
                                (assoc-in value-path value))]
      option-updated-db)))

(reg-event-db
  :editor/select-custom-multi-options
  (fn [db [_ & path]]
    (let [dropdown-path (current-form-content-path db [path])]
      (update-in db dropdown-path dissoc :koodisto-source))))

(reg-event-db
  :editor/select-koodisto-options
  (fn [db [_ uri version title & path]]
    (let [dropdown-path (current-form-content-path db [path])]
      (update-in db dropdown-path assoc :koodisto-source {:uri uri :version version :title title}))))

(defn add-validator
  [db [_ validator & path]]
  (let [path (current-form-content-path db [path :validators])]
    (update-in db path (fn [validators]
                         (when-not (some #(= % validator) validators)
                           (conj validators validator))))))

(reg-event-db :editor/add-validator add-validator)

(defn remove-validator
  [db [_ validator & path]]
  (let [path       (current-form-content-path db [path :validators])
        validators (get-in db path)]
    (if-not (nil? validators)
      (update-in db path (fn [validators]
                           (remove #(= % validator) validators)))
      db)))

(reg-event-db :editor/remove-validator remove-validator)

(reg-event-db
  :editor/set-component-value
  (fn [db [_ value & path]]
    (assoc-in db (current-form-content-path db [path]) value)))

(defn generate-component
  [db [_ generate-fn path]]
  (with-form-key [db form-key]
    (let [form-key  (get-in db [:editor :selected-form-key])
          path-vec  (current-form-content-path db [path])
          component (generate-fn)]
      (->
        (if (zero? (last path-vec))
          (assoc-in db (butlast path-vec) [component])
          (assoc-in db path-vec component))
        (assoc-in [:editor :ui (:id component) :focus?] true)))))

(reg-event-db :generate-component generate-component)

(defn remove-component
  [db path]
  (with-form-key [db form-key]
    (let [remove-index (last path)
          path-vec     (-> [:editor :forms form-key :content [path]]
                         flatten
                         butlast)]
      (->> (get-in db path-vec)
           (keep-indexed (fn [index element]
                           (when-not (= index remove-index) element)))
           (into [])
           (assoc-in db path-vec)))))

(reg-event-db
  :remove-component
  (fn [db [_ path dom-node]]
    (.addEventListener
      dom-node
      "animationend"
      #(do
         (.removeEventListener (.-target %) "animationend" (-> (cljs.core/js-arguments) .-callee))
         (dispatch [:state-update-fx
                    (fn [{:keys [db]}]
                      (let [forms-meta-db (update-in db [:editor :forms-meta] assoc path :removed)
                            followup? (= :followup (last path))]
                        (if followup?
                          {:db forms-meta-db
                           :dispatch [:editor/followup-remove path]}
                          {:db (remove-component forms-meta-db path)})))])))
    (assoc-in db [:editor :forms-meta path] :fade-out)))

(reg-event-db
  :editor/handle-user-info
  (fn [db [_ user-info-response]]
    (assoc-in db [:editor :user-info] user-info-response)))

(defn- languages->kwd [form]
  (update form :languages
    (partial mapv keyword)))

(defn refresh-forms
  ([include-deleted?]
   (http
     :get
     (str "/lomake-editori/api/forms" (if include-deleted? "?include-deleted=true" ""))
     (fn [db {:keys [forms]}]
       (assoc-in db [:editor :forms] (->> forms
                                          (mapv languages->kwd)
                                          (util/group-by-first :key)
                                          (sorted-by-time-and-deletedness))))))
  ([]
    (refresh-forms false)))

(reg-event-db
  :editor/refresh-forms-with-deleteds
  (fn [db _]
    (when-let [autosave (-> db :editor :autosave)]
      (autosave/stop-autosave! autosave))
    (refresh-forms true)
    (-> db
        (update :editor dissoc :forms)
        (update :editor dissoc :show-remove-confirm-dialog?))))

(reg-event-db
  :editor/refresh-forms
  (fn [db _]
    (autosave/stop-autosave! (-> db :editor :autosave))
    (refresh-forms)
    (-> db
        (update :editor dissoc :forms)
        (update :editor dissoc :show-remove-confirm-dialog?))))

(reg-event-db
  :editor/refresh-hakukohteet-from-applications
  (fn [db _]
    (http
      :get
      "/lomake-editori/api/hakukohteet"
      (fn [db hakukohteet]
        (assoc-in db [:editor :hakukohteet] hakukohteet)))
    db))

(defn- editor-autosave-predicate [current prev]
  (match [current (merge {:content []} prev)]
    [_ {:content []}]
    false

    :else
    (not=
      ; :id changes when new version is created,
      ; :key remains the same across versions
      (dissoc prev :created-time :id)
      (dissoc current :created-time :id))))

(defn- handle-fetch-form [db {:keys [key] :as response} _]
  (-> db
      (update :editor dissoc :ui)
      (assoc-in [:editor :forms key] (languages->kwd response))
      (assoc-in [:editor :autosave]
        (autosave/interval-loop {:subscribe-path    [:editor :forms key]
                                 :changed-predicate editor-autosave-predicate
                                 :handler           (fn [form previous-autosave-form-at-time-of-dispatch]
                                                      (dispatch [:editor/save-form]))}))))

(defn fetch-form-content! [form-id]
  (http :get
        (str "/lomake-editori/api/forms/" form-id)
        handle-fetch-form))

(reg-event-db
  :editor/select-hakukohde
  (fn [db [_ hakukohde]]
    (-> db
        (update-in [:editor] dissoc :selected-form-key)
        (assoc-in [:editor :selected-hakukohde] hakukohde))))

(reg-event-db
  :editor/select-form
  (fn [db [_ form-key]]
    (with-form-key [db previous-form-key]
      (do
        (when (not= previous-form-key form-key)
          (autosave/stop-autosave! (-> db :editor :autosave)))
        (when-let [id (get-in db [:editor :forms form-key :id])]
          (fetch-form-content! id))
        (-> db
            (update-in [:editor] dissoc :selected-hakukohde)
            (assoc-in [:editor :selected-form-key] form-key))))))

(def save-chan (async/chan (async/sliding-buffer 1)))

(defn save-loop [save-chan]
  (go-loop [_ (async/<! save-chan)]
    (let [form (-> @(subscribe [:editor/selected-form])
                   (dissoc :created-time))
          response-chan (async/chan)]
      (when (not-empty (:content form))
        (do
          (post "/lomake-editori/api/forms" form
            (fn [db response] (do (async/put! response-chan response)
                                  db))
            :override-args {:error-handler (fn [error]
                                             (async/close! response-chan)
                                             (dispatch-flasher-error-msg :post error))})
          (when-let [updated-form (some->
                                    (async/<! response-chan)
                                    (languages->kwd))]
            (dispatch-sync
              [:state-update
               (fn [db]
                 ; Merge updated form without content, because
                 ; user might have typed something between requests and
                 ; updated-form would replace the newer content
                 (do (async/close! response-chan)
                     (update-in db [:editor :forms (:key updated-form)]
                       merge (dissoc updated-form :content :name))))])))))
    (recur (async/<! save-chan))))

(save-loop save-chan)

(defn save-form
  [db _]
  (async/put! save-chan true)
  db)

(reg-event-db :editor/save-form save-form)

(defn- post-new-form
  ([] (post-new-form {}))
  ([{:keys [name
            content
            languages]
     :or   {name      "Uusi lomake"
            content   [(pm/person-info-module)]
            languages [:fi]}}]
   (post "/lomake-editori/api/forms"
     {:name      name
      :content   content
      :languages languages}
     (fn [db form]
       (let [stop-fn (get-in db [:editor :autosave])
             path (str "/lomake-editori/editor/" (:key (languages->kwd form)))]
         (autosave/stop-autosave! stop-fn)
         (set-history! path)
         (assoc-in db [:editor :new-form-created?] true))))))

(reg-event-db
  :editor/add-form
  (fn [db _]
    (post-new-form)
    db))

(defn- copy-form [db _]
  (let [form-id (get-in db [:editor :selected-form-key])
        form    (-> (get-in db [:editor :forms form-id])
                    (update :name str " - KOPIO"))]
    (post-new-form form)
    db))

(reg-event-db :editor/copy-form copy-form)

(defn- removed-application-review-active? [db]
  (let [selected-form-key (get-in db [:editor :selected-form-key])
        form-in-review    (get-in db [:application :selected-application-and-form :form])]
    (= selected-form-key (:key form-in-review))))

(defn- reset-application-review-state [db]
  (assoc db :application {}))

(defn- remove-form [{:keys [db]} _]
  (let [form-key (get-in db [:editor :selected-form-key])
        form-id  (get-in db [:editor :forms form-key :id])]
    (-> {:db   (cond-> (update db :editor dissoc :selected-form-key)
                 (removed-application-review-active? db)
                 (reset-application-review-state))
         :http {:method              :delete
                :path                (str "/lomake-editori/api/forms/" form-id)
                :handler-or-dispatch :editor/refresh-forms}})))

(reg-event-fx :editor/remove-form remove-form)

(reg-event-db
  :editor/change-form-name
  (fn [db [_ new-form-name]]
    (with-form-key [db selected-form-key]
      (update-in db [:editor :forms selected-form-key]
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
  (with-form-key [db form-key]
    (let [component                (get-in db (concat [:editor :forms form-key :content] source-path))
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

(reg-event-db :editor/move-component move-component)

(def ^:private lang-order
  [:fi :sv :en])

(defn- index-of [coll t]
  (let [length (count coll)]
    (loop [idx 0]
      (cond
        (<= length idx) -1
        (= (get coll idx) t) idx
        :else (recur (inc idx))))))

(defn- toggle-language [db [_ lang]]
  (let [form-path [:editor :forms (get-in db [:editor :selected-form-key])]
        lang-path (conj form-path :languages)]
    (->
      (update-in db lang-path
        (fn [languages]
          (let [languages (or languages [:fi])]
            (cond
              (not (some #{lang} languages)) (sort-by (partial index-of lang-order)
                                               (conj languages lang))
              (> (count languages) 1)        (filter (partial not= lang) languages)
              :else                          languages))))
      (update-in [:editor :ui]
        (fn [ui]
          (clojure.walk/prewalk
            (fn [x]
              (if (= [:focus? true] x)
                [:focus? false]
                x))
            ui))))))

(reg-event-db :editor/toggle-language toggle-language)
