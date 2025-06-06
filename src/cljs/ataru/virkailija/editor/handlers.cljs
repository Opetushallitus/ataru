(ns ataru.virkailija.editor.handlers
  (:require [ataru.cljs-util :as cu]
            [ataru.collections :as collections]
            [ataru.component-data.component :as component]
            [ataru.component-data.person-info-module :as pm]
            [ataru.component-data.value-transformers :refer [update-options-while-keeping-existing-followups]]
            [ataru.koodisto.koodisto-whitelist :as koodisto-whitelist]
            [ataru.number :as number]
            [ataru.user-rights :as user-rights]
            [ataru.util :as util :refer [assoc? collect-ids]]
            [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.editor.db :as db]
            [ataru.virkailija.editor.demo.handlers]
            [ataru.virkailija.editor.editor-macros :refer-macros [with-form-key]]
            [ataru.virkailija.editor.editor-selectors :refer [get-email-template]]
            [ataru.virkailija.editor.form-diff :as form-diff]
            [ataru.virkailija.routes :refer [set-history!]]
            [ataru.virkailija.temporal :as temporal]
            [ataru.virkailija.virkailija-ajax :refer [dispatch-flasher-error-msg
                                                      http post put]]
            [ataru.schema.maksut-schema :refer [astu-order-id-prefixes]]
            [ataru.config :as config]
            [cljs-time.core :as c]
            [cljs.core.async :as async]
            [cljs.core.match :refer-macros [match]]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx
                                   subscribe]])
  (:require-macros [ataru.async-macros :as asyncm]
                   [cljs.core.async.macros :refer [go-loop]]))

(defn get-user-info [db _]
  (http
    :get
    "/lomake-editori/api/user-info"
    :editor/handle-user-info)
  db)

(reg-event-db :editor/get-user-info get-user-info)

(defn- fold [db id]
  (assoc-in db [:editor :ui id :folded?] true))

(defn unfold [db id]
  (assoc-in db [:editor :ui id :folded?] false))

(defn fold-all [db]
  (->> (get-in db (vec (db/current-form-content-path db)))
       (reduce collect-ids [])
       (reduce fold db)))

(defn- set-non-koodisto-option-values
  [field-descriptor]
  (if (nil? (:koodisto-source field-descriptor))
    (update field-descriptor :options
            #(vec (collections/generate-missing-values %)))
    field-descriptor))

(reg-event-db
  :editor/remove-dropdown-option
  (fn [db [_ & path]]
    (let [option-path (db/current-form-content-path db [path])]
      (-> db
          (update-in (drop-last option-path) util/remove-nth (last option-path))))))

(reg-event-db
  :editor/add-dropdown-option
  (fn [db [_ & path]]
    (let [dropdown-path (db/current-form-content-path db [path :options])
          component     (component/dropdown-option nil)]
      (-> db
          (update-in dropdown-path into [component])
          (update-in (drop-last dropdown-path) set-non-koodisto-option-values)))))

(reg-event-db
  :editor/remove-text-field-option
  (fn [db [_ path]]
    (let [option-path (db/current-form-content-path db [path])]
      (-> db
          (update-in (drop-last option-path) util/remove-nth (last option-path))))))

(reg-event-db
  :editor/add-text-field-option
  (fn [db [_ path]]
    (let [text-field-path (db/current-form-content-path db [path :options])
          component       (component/text-field-option)]
      (-> db
          (update-in text-field-path into [component])
          (update-in (drop-last text-field-path) set-non-koodisto-option-values)))))

(reg-event-db
  :editor/poista-tekstikentän-arvon-perusteella-optio
  (fn [db [_ path]]
    (let [option-path (db/current-form-content-path db [path])]
      (-> db
          (update-in (drop-last option-path) util/remove-nth (last option-path))))))

(reg-event-db
  :editor/lisää-tekstikentän-arvon-perusteella-optio
  (fn [db [_ path]]
    (let [text-field-path (db/current-form-content-path db [path :options])
          component       (component/text-field-conditional-option)]
      (-> db
          (update-in text-field-path into [component])
          (update-in (drop-last text-field-path) set-non-koodisto-option-values)))))

(defn- add-section-visibility-condition
  [db path condition]
  (let [text-field-path (db/current-form-content-path db [path :section-visibility-conditions])
        hideable-form-sections @(subscribe [:editor/current-lomakeosiot])
        default-hidden-section-name (-> hideable-form-sections first :id)
        section-visibility (merge condition {:section-name default-hidden-section-name})]
    (-> db
      (update-in text-field-path (fnil #(conj %1 %2) []) section-visibility)
      (update-in (drop-last text-field-path) set-non-koodisto-option-values))))

(reg-event-db
  :editor/lisää-tekstikentän-arvon-perusteella-osion-piilottamis-ehto
  (fn [db [_ path]]
    (add-section-visibility-condition
      db
      path
      {:condition {:comparison-operator "<"
                   :data-type           "int"}})))

(reg-event-db
  :editor/lisää-tekstikentän-arvon-perusteella-piilotettavan-osion-nimi
  (fn [db [_ path option-index value]]
    (let [section-name-path (db/current-form-content-path db [path option-index :section-name])]
      (assoc-in db
                 section-name-path
                 value))))

(reg-event-db
  :editor/aseta-lisäkysymys-arvon-perusteella-operaattori
  (fn [db [_ path option-index value]]
    (let [condition-path (db/current-form-content-path db [path option-index :condition])]
      (update-in db
                 condition-path
                 (fn [condition]
                   (assoc condition :comparison-operator value))))))

#_{:clj-kondo/ignore [:dfreeman.re-frame/sub-in-event-handler]}
; TODO: Replace subscribe call in this event handler with a selector function call
(reg-event-db
  :editor/lisää-pudotusvalikon-arvon-perusteella-osion-piilottamis-ehto
  (fn [db [_ path]]
    (let [field                      @(subscribe [:editor/get-component-value path])
          default-answer-compared-to (some-> field :options first :value)
          condition                  {:condition {:comparison-operator "="
                                                  :data-type           "str"
                                                  :answer-compared-to  default-answer-compared-to}}]
      (add-section-visibility-condition
        db
        path
        condition))))

(reg-event-db
  :editor/remove-visibility-condition
  (fn [db [_ path visibility-condition-index]]
    (let [visibility-conditions-path (db/current-form-content-path db [path :section-visibility-conditions])]
      (update-in db visibility-conditions-path util/remove-nth visibility-condition-index))))

(reg-event-db
  :editor/set-visibility-condition-value
  (fn [db [_ component-path visibility-condition-index value]]
    (let [path (db/current-form-content-path
                 db
                 [component-path
                  :section-visibility-conditions
                  visibility-condition-index
                  :condition
                  :answer-compared-to])]
      (assoc-in db path value))))

(reg-event-db
  :editor/set-visibility-condition-section
  (fn [db [_ component-path visibility-condition-index value]]
    (let [path (db/current-form-content-path
                 db
                 [component-path
                  :section-visibility-conditions
                  visibility-condition-index
                  :section-name])]
      (assoc-in db path value))))

(reg-event-db
  :editor/aseta-lisäkysymys-arvon-perusteella-vertailuarvo
  (fn [db [_ path option-index value]]
    (let [condition-path (db/current-form-content-path db [path option-index :condition])]
      (update-in db
                 condition-path
                 (fn [condition]
                   (if (string/blank? value)
                     (dissoc condition :answer-compared-to)
                     (assoc condition :answer-compared-to (number/->int value))))))))

(reg-event-db
  :editor/set-ordered-by-user
  (fn [db [_ value & path]]
    (let [component-path (db/current-form-content-path db [path])]
      (update-in db component-path assoc :koodisto-ordered-by-user (not value)))))

(reg-event-db
  :editor/set-dropdown-option-value
  (fn [db [_ value & path]]
    (let [label-path (db/current-form-content-path db [path])]
      (assoc-in db label-path value))))

(reg-event-db
  :editor/set-dropdown-option-selection-limit
  (fn [db [_ value & path]]
    (let [label-path (db/current-form-content-path db [path])]
      (assoc-in db label-path value))))

(defn- swap-vector [v i1 i2]
  (assoc v i2 (v i1) i1 (v i2)))

(reg-event-db
  :editor/move-option-up
  (fn [db [_ path index]]
    (if (= 0 index)
      db
      (let [options-path (db/current-form-content-path db [path :options])]
        (-> db
            (update-in options-path swap-vector index (dec index)))))))

(reg-event-db
  :editor/move-option-down
  (fn [db [_ path index]]
    (let [options-path    (db/current-form-content-path db [path :options])
          options         (get-in db options-path)
          is-last-option? (= (inc index) (count options))]
      (if is-last-option?
        db
        (-> db
            (update-in options-path swap-vector index (inc index)))))))

(reg-event-fx
  :editor/select-koodisto-options
  (fn [{db :db} [_ uri path allow-invalid?]]
    (when-let [koodisto (some #(when (= uri (:uri %)) %) koodisto-whitelist/koodisto-whitelist)]
      (let [koodisto      (assoc koodisto :allow-invalid? (boolean allow-invalid?))
            dropdown-path (db/current-form-content-path db [path])
            id            (get-in db (concat dropdown-path [:id]))]
        {:db       (update-in db dropdown-path assoc
                              :koodisto-source koodisto
                              :options [])
         :dispatch [:editor/fetch-koodisto-for-component-with-id id koodisto]}))))

(defn- update-modified-by
  [db path]
  (let [metadata-path (db/current-form-content-path db (conj (first path) :metadata :modified-by))
        user-info     (-> db :editor :user-info)]
    (assoc-in db metadata-path {:oid  (:oid user-info)
                                :name (:name user-info)
                                :date (temporal/datetime-now)})))

(defn add-validator
  [db [_ validator & path]]
  (let [content-path (db/current-form-content-path db [path :validators])]
    (-> db
        (update-in content-path (fn [validators]
                                  (if-not (some #(= % validator) validators)
                                    (conj validators validator)
                                    validators)))
        (update-modified-by path))))

(reg-event-db :editor/add-validator add-validator)

(defn- remove-selection-limits-from-options
  [db path]
  (let [content-path (db/current-form-content-path db path)
        remove-selection-limit (fn [option] (dissoc option :selection-limit))]
    (update-in db content-path (fn [field] (update field :options (partial mapv remove-selection-limit))))))

(reg-event-fx
  :editor/set-selection-group-id
  (fn [{db :db} [_ selection-group-id & path]]
    (let [content-path (db/current-form-content-path db [path :params])]
      {:db         (-> db
                       (update-in content-path (fn [params]
                                                 (if selection-group-id
                                                   (assoc params :selection-group-id selection-group-id)
                                                   (dissoc params :selection-group-id))))
                       (remove-selection-limits-from-options path)
                       (update-modified-by path))
       :dispatch-n [(when selection-group-id
                      [:editor/set-required-to-all-in-path path])]})))

(reg-event-fx
  :editor/set-required-to-all-in-path
  (fn [{db :db} [_ & path]]
    (let [parent              (vec (drop-last 2 (flatten path)))
          is-option?          (= :options (last (drop-last 1 (flatten path))))
          is-wrapper-element? (= "wrapperElement" (get-in db (db/current-form-content-path db [path :fieldClass])))
          content-path        (db/current-form-content-path db [path :validators])
          validators          (set (get-in db content-path))]
      {:db         db
       :dispatch-n [(when-not (or is-option?
                                  is-wrapper-element?
                                  (contains? validators "required"))
                      [:editor/add-validator "required" (vec (flatten path))])
                    (when (seq parent)
                      [:editor/set-required-to-all-in-path parent])]})))

(defn any-child-is-selection-limit? [db path]
  (let [field (get-in db (db/current-form-content-path db [path]))]
    (first (filter :selection-group-id (tree-seq coll? seq field)))))

(defn remove-validator
  [db [_ validator & path]]
  (let [content-path (db/current-form-content-path db [path :validators])
        validators   (get-in db content-path)]
    (if (and (not (nil? validators))
             (not (and (= validator "required") (any-child-is-selection-limit? db path))))
      (-> db
          (update-in content-path (fn [validators]
                                    (remove #(= % validator) validators)))
          (update-modified-by path))
      db)))

(reg-event-db :editor/remove-validator remove-validator)

(reg-event-db
  :editor/set-component-value
  (fn [db [_ value & path]]
    (-> db
        (assoc-in (db/current-form-content-path db [path]) value)
        (update-modified-by path))))

(defn- number-of-decimals [v]
  (let [[_ decimals] (string/split v #",")]
    (count decimals)))

(defn- format-range [value]
  (string/replace (string/trim (or value "")) "." ","))

(defn valid-range? [value decimals]
                     (let [clean (format-range value)]
                       (or
                        (empty? clean)
                        (and (re-matches number/numeric-matcher clean)
                             (<= (number-of-decimals clean) decimals)))))

(defn- validate-min-max [min max decimals]
  (let [min (format-range min)
        max (format-range max)
        min-valid?  (valid-range? min decimals)
        max-valid?  (valid-range? max decimals)
        empty-range?   (and (not (empty? min))
                            (not (empty? max))
                            (and min-valid? max-valid?)
                            (number/gte min max))]
    (if empty-range?
      [false false]
      [min-valid? max-valid?])))

(reg-event-db
  :editor/set-decimals-value
  (fn [db [_ id value & path]]
    (let [path             (concat path [:params])
          min-value        (get-in db (db/current-form-content-path db [path :min-value]))
          max-value        (get-in db (db/current-form-content-path db [path :max-value]))
          [min? max?]      (validate-min-max min-value max-value value)]
      (cond-> (-> db
                  (assoc-in (db/current-form-content-path db [path :decimals]) value)
                  (update-modified-by path)
                  (assoc-in [:editor :ui id :min-value :invalid?] (not min?))
                  (assoc-in [:editor :ui id :max-value :invalid?] (not max?)))


              (not min?)
              (update-in (db/current-form-content-path db path) (fn [params]
                                                               (dissoc params :min-value)))

              (not max?)
              (update-in (db/current-form-content-path db path) (fn [params]
                                                               (dissoc params :max-value)))))))
#_{:clj-kondo/ignore [:dfreeman.re-frame/sub-in-event-handler]}
; TODO: Replace subscribe call in this event handler with a selector function call
(reg-event-db
  :editor/set-range-value
  (fn [db [_ id range value & path]]
    (let [path           (concat path [:params])
          min-range?     (= :min-value range)
          decimals       @(subscribe [:editor/get-component-value path :decimals])
          clean-value    (format-range value)
          opposing-range (if min-range?
                           :max-value
                           :min-value)
          opposing-value (get-in db (db/current-form-content-path db [path opposing-range]) "")
          [min? max?] (if min-range?
                        (validate-min-max value opposing-value decimals)
                        (validate-min-max opposing-value value decimals))]
      (cond-> (-> db
                  (update-modified-by path)
                  (assoc-in [:editor :ui id range :value] value)
                  (assoc-in [:editor :ui id :min-value :invalid?] (not min?))
                  (assoc-in [:editor :ui id :max-value :invalid?] (not max?)))

              (empty? clean-value)
              (update-in (db/current-form-content-path db path) (fn [params]
                                                                 (dissoc params range)))

              (and (not (empty? clean-value)) (if min-range? min? max?))
              (assoc-in (db/current-form-content-path db path range) clean-value)))))


(reg-event-db
  :editor/update-mail-attachment
  (fn [db [_ mail-attachment? & path]]
    (let [flip-mail-attachment (fn [{:keys [params validators] :as field}]
                                 (let [params (-> params
                                                  (dissoc :attachment-type :fetch-info-from-kouta?)
                                                  (assoc?
                                                      :mail-attachment? mail-attachment?
                                                      :info-text (when mail-attachment?
                                                                   (assoc (:info-text params) :enabled? true))))]
                                   (assoc? field
                                           :per-hakukohde false
                                           :params params
                                           :validators (when mail-attachment?
                                                         (filter #(not= "required" %) validators)))))]
      (-> db
          (update-in (db/current-form-content-path db [path]) flip-mail-attachment)
          (update-modified-by path)))))

(defn generate-component
  [{db :db} [_ generate-fn sub-path]]
  (let [yhteishaku?           (subscribe [:editor/yhteishaku?])
        parent-component-path (cond-> (db/current-form-content-path db)
                                      (not (number? sub-path))
                                      (concat (butlast sub-path)))
        user-info             (-> db :editor :user-info)
        metadata-element      {:oid  (:oid user-info)
                               :name (:name user-info)
                               :date (temporal/datetime-now)}
        metadata              {:created-by  metadata-element
                               :modified-by metadata-element}
        components            (cond->> (if (vector? generate-fn)
                                         (map #(apply % [metadata]) generate-fn)
                                         [(generate-fn metadata)])

                                       @yhteishaku?
                                       (map #(cond-> %
                                                     (not (contains? #{"pohjakoulutusristiriita"
                                                                       "fieldset"
                                                                       "hakukohteet"}
                                                                     (:fieldType %)))
                                                     (assoc-in [:params :hidden] @yhteishaku?))))
        first-component-idx   (cond-> sub-path
                                      (not (number? sub-path))
                                      (last))
        get-koodisto-params-fn (fn [component]
                                 {:uri (get-in component [:koodisto-source :uri])
                                  :version (get-in component [:koodisto-source :version])
                                  :allow-invalid? (get-in component [:koodisto-source :allow-invalid?] false)})
        koodisto-components-to-dispatch (->> components
                                             util/flatten-form-fields
                                             (filter #(and (some? (:koodisto-source %)) (> (count (get-in % [:koodisto-source :uri])) 0)))
                                             (map (fn [component]
                                                    [:editor/fetch-koodisto-for-component-with-id (:id component) (get-koodisto-params-fn component)])))
        koodisto-components-with-load (if (> (count koodisto-components-to-dispatch) 0)
                                        (into [] (concat [[:editor/start-load-spinner (count koodisto-components-to-dispatch)]] koodisto-components-to-dispatch))
                                        koodisto-components-to-dispatch)]
    (as-> db db'
      (update-in db' parent-component-path (cu/vector-of-length (count components)))
      (reduce (fn [db' [idx component]]
                (let [path (flatten [parent-component-path (+ first-component-idx idx)])]
                  (assoc-in db' path component)))
              db'
              (map vector (range) components))
      (assoc-in db' [:editor :ui (-> components first :id) :focus?] true)
      {:db db'
       :dispatch-n koodisto-components-with-load})))

(reg-event-fx :generate-component generate-component)

(reg-event-db
  :editor/start-load-spinner
  (fn [db [_ amount]]
    (assoc-in db [:editor :ui :spinner] amount)))

(reg-event-db
  :editor/remove-from-load-spinner
  (fn [db _]
    (let [amount (get-in db [:editor :ui :spinner])
          amount-after (js/Math.max 0 (- amount 1))]
      (assoc-in db [:editor :ui :spinner] amount-after))))

(defn- remove-component
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

(defn- remove-properties
  [db property-key]
  (if property-key
    (with-form-key [db form-key]
    (let [path [:editor :forms form-key :properties]]
      (update-in db path dissoc property-key)))
    db))


(reg-event-db
  :editor/remove-component
  (fn [db [_ path & {:keys [property-key]}]]
    (let [id (get-in db (vec (db/current-form-content-path db [path :id])))]
      (-> db
          (update-in [:editor :ui id] dissoc :remove)
          (remove-component path)
          (remove-properties property-key)))))

(reg-event-fx
  :editor/confirm-remove-component
  (fn [{db :db} [_ path & {:keys [property-key]}]]
    (let [id (get-in db (vec (db/current-form-content-path db [path :id])))]
      {:db             (assoc-in db [:editor :ui id :remove] :disabled)
       :dispatch       [:editor/fold id]
       :dispatch-later [{:ms 310 :dispatch [:editor/remove-component path {:property-key property-key}]}]})))

(reg-event-db
  :editor/start-remove-component
  (fn [db [_ path]]
    (let [id (get-in db (vec (db/current-form-content-path db [path :id])))]
      (assoc-in db [:editor :ui id :remove] :confirm))))

(reg-event-db
  :editor/cancel-remove-component
  (fn [db [_ path]]
    (let [id (get-in db (vec (db/current-form-content-path db [path :id])))]
      (update-in db [:editor :ui id] dissoc :remove))))

(defn stamp-user-organization [is-user-organization-fn hakukohderyhma]
  (merge hakukohderyhma
    {:user-organization? (boolean (is-user-organization-fn (:oid hakukohderyhma)))}))

(defn- on-haku-data-fetched [hakukohteet-promise hakukohderyhmat-promise]
  (async/go
    (try
      (let [haut                    (asyncm/<? hakukohteet-promise)
            hakukohteet             (->> (vals haut)
                                         (mapcat #(:hakukohteet %)))
            ryhmaliitokset          (set (mapcat #(:ryhmaliitokset %) hakukohteet))
            hakukohderyhmat         (->> (asyncm/<? hakukohderyhmat-promise)
                                         (filter #(contains? ryhmaliitokset (:oid %))))
            omat-hakukohteet        (->> hakukohteet
                                         (filter :user-organization?))
            omat-ryhmaliitokset     (set (mapcat #(:ryhmaliitokset %) omat-hakukohteet))
            is-user-organization-fn #(contains? omat-ryhmaliitokset %)]
        (dispatch [:editor/set-used-by-haut
                   haut
                   (map #(stamp-user-organization is-user-organization-fn %) hakukohderyhmat)]))
      (catch js/Error e
        (dispatch [:editor/unset-used-by-haut])
        (throw e)))))

(reg-event-fx
  :editor/refresh-used-by-haut
  (fn [{db :db} _]
    (when-let [form-key (get-in db [:editor :selected-form-key])]
      (let [haku-oids               (map :oid (get-in db [:editor :form-used-in-hakus form-key]))
            organization-oids       (map :oid (get-in db [:editor :user-info :organizations] []))
            hakukohderyhmat-promise (async/promise-chan)
            hakukohteet-promise     (async/promise-chan)]
        (on-haku-data-fetched hakukohteet-promise hakukohderyhmat-promise)
        {:db                          (-> db
                                          (assoc-in [:editor :used-by-haut :fetching?] true)
                                          (assoc-in [:editor :used-by-haut :error?] false))
         :fetch-haut-with-hakukohteet [hakukohteet-promise organization-oids haku-oids]
         :fetch-hakukohde-groups      [hakukohderyhmat-promise]}))))

(reg-event-db
  :editor/handle-user-info
  (fn [db [_ user-info-response]]
    (assoc-in db [:editor :user-info] user-info-response)))

(defn- languages->kwd [form]
  (update form :languages
    (partial mapv keyword)))

(defn- parse-form-created-times
  [form]
  (assoc form :created-time (temporal/str->googdate (:created-time form))))

(defn- keep-selected-form-as-is [db]
  (if-let [selected-form (get-in db [:editor :forms (get-in db [:editor :selected-form-key])])]
    (fn [form]
      (if (= (:key selected-form) (:key form))
        selected-form
        form))
    identity))

(defn refresh-forms-for-editor []
  (http
   :get
   "/lomake-editori/api/forms?include-closed=true"
   (fn [db {:keys [forms]}]
     (let [forms (->> forms
                      (mapv parse-form-created-times)
                      (mapv languages->kwd)
                      (mapv (keep-selected-form-as-is db))
                      (util/group-by-first :key))
           keys  (->> forms
                      (sort (fn [[k1 v1] [k2 v2]]
                              (let [now (c/now)
                                    c1  (get v1 :created-time now)
                                    c2  (get v2 :created-time now)]
                                (cond (= k1 k2)        0
                                      (c/equal? c1 c2) (compare (:id v1)
                                                                (:id v2))
                                      (c/after? c1 c2) -1
                                      :else            1))))
                      (map first))]
       (-> db
           (assoc-in [:editor :forms] forms)
           (assoc-in [:editor :sorted-form-keys] keys))))
   :skip-parse-times? true))

(reg-event-db
  :editor/refresh-forms-for-editor
  (fn [db _]
    (autosave/stop-autosave! (-> db :editor :autosave))
    (refresh-forms-for-editor)
    db))

(reg-event-db
  :editor/refresh-forms-if-empty
  (fn [db [_ form-key]]
    (if (-> db :editor :forms (get form-key))
      db
      (do
        (autosave/stop-autosave! (-> db :editor :autosave))
        (refresh-forms-for-editor)
        db))))

(defn- editor-autosave-predicate [current prev]
  (match [current (merge {:content []} prev)]
    [_ {:content []}]
    (if (= (:name current) (:name prev))
      false true)

    :else
    (not=
      ; :id changes when new version is created,
      ; :key remains the same across versions
      (dissoc prev :created-time :id)
      (dissoc current :created-time :id))))

(reg-event-db
  :editor/set-used-by-haut
  (fn [db [_ haut hakukohderyhmat]]
    (-> db
      (assoc-in [:editor :used-by-haut :fetching?] false)
      (assoc-in [:editor :used-by-haut :error?] false)
      (assoc-in [:editor :used-by-haut :haut] haut)
      (assoc-in [:editor :used-by-haut :hakukohderyhmat] hakukohderyhmat))))

(reg-event-db
  :editor/unset-used-by-haut
  (fn [db _]
    (-> db
      (assoc-in [:editor :used-by-haut :fetching?] false)
      (assoc-in [:editor :used-by-haut :error?] true)
      (update-in [:editor :used-by-haut] dissoc :haut)
      (update-in [:editor :used-by-haut] dissoc :hakukohderyhmat))))

(defn- create-autosave-loop
  ([initial-form]
   (create-autosave-loop initial-form nil))
  ([initial-form last-autosaved-form]
   (autosave/interval-loop {:last-autosaved-form last-autosaved-form
                            :subscribe-path      [:editor :forms (:key initial-form)]
                            :changed-predicate   editor-autosave-predicate
                            :handler             (fn [form _]
                                                   (dispatch [:editor/save-form form (or last-autosaved-form initial-form)]))})))

(defn- set-form-lock-state [form]
  (assoc form :lock-state (if (:locked form)
                            :locked
                            :open)))

(reg-event-db
  :editor/handle-fetch-form
  (fn [db [_ {:keys [key deleted] :as response} _]]
    (if deleted
      db
      (let [new-form (-> response
                       (languages->kwd)
                       (update :created-time temporal/str->googdate))]
      (-> db
          (update :editor dissoc :ui)
          (assoc-in [:editor :forms key] new-form)
          (fold-all)
          (assoc-in [:editor :save-snapshot] new-form)
          (assoc-in [:editor :autosave] (create-autosave-loop new-form))
          (update-in [:editor :forms key] set-form-lock-state)
          (assoc-in [:editor :form-loading] false))))))

(defn- fetch-form-content-fx
  [form-id]
  {:http {:method :get
          :path (str "/lomake-editori/api/forms/" form-id)
          :skip-parse-times? true
          :handler-or-dispatch :editor/handle-fetch-form}})

(reg-event-fx
  :editor/handle-refresh-form-used-in-hakus
  (fn [{db :db} [_ response args]]
    {:db       (assoc-in db [:editor :form-used-in-hakus (:form-key args)] response)
     :dispatch [:editor/refresh-used-by-haut]}))

(reg-event-fx
  :editor/refresh-form-used-in-hakus
  (fn [_ [_ form-key]]
    {:http {:method              :get
            :path                (str "/lomake-editori/api/tarjonta/haku?form-key=" form-key)
            :handler-or-dispatch :editor/handle-refresh-form-used-in-hakus
            :handler-args        {:form-key form-key}
            :skip-parse-times?   true}}))

(reg-event-fx
  :editor/select-form
  (fn [{db :db} [_ form-key]]
    (with-form-key [db previous-form-key]
      (merge
        {:db (cond-> db
                     (and (some? previous-form-key) (not= previous-form-key (:copy-component-form-key (get-in db [:editor :copy-component]))))
                     (update-in [:editor :forms previous-form-key] assoc :content [])

                     true
                     (assoc-in [:editor :selected-form-key] form-key)

                     (get-in db [:editor :forms form-key :id])
                     (assoc-in [:editor :form-loading] true))}
        {:dispatch [:editor/refresh-form-used-in-hakus form-key]}
        {:dispatch-debounced {:timeout 500
                              :id :fetch-attachment-types
                              :dispatch [:editor/fetch-attachment-types-koodisto]}}
        (when (and (some? previous-form-key)
                   (not= previous-form-key form-key))
          {:stop-autosave (get-in db [:editor :autosave])})
        (when-let [id (get-in db [:editor :forms form-key :id])]
          (fetch-form-content-fx id))))))

(def save-chan (async/chan (async/sliding-buffer 1)))

(defn update-form-with-fragment [form fragments]
  (let [response-promise (async/promise-chan)]
    (put (str "/lomake-editori/api/forms/" (:id form)) fragments
      (fn [db response]
        (async/put! response-promise response)
        db)
      :override-args {:skip-parse-times? true
                      :error-handler (fn [error]
                                       (async/put! response-promise (js/Error. error)))})
    response-promise))

(defn save-loop [save-chan]
  (let [last-saved-snapshot (atom nil)]
    (go-loop []
      (let [[form original-form] (async/<! save-chan)
            snapshot (let [snapshot @last-saved-snapshot]
                       (if (= (:key snapshot) (:key form))
                         snapshot
                         original-form))
            invalid-koodisto (fn [component]
                               (if-let [uri (-> component
                                                :koodisto-source
                                                :uri)]
                                 (string/blank? uri)
                                 false))
            omit-invalid (fn [component]
                           (if (vector? component)
                             (vec (remove invalid-koodisto component))
                             component))
            form     (-> form
                         (dissoc :created-time)
                         (update :content #(walk/postwalk omit-invalid %)))]
        (when (not-empty (:content form))
          (try
            (when-let [fragments (seq (form-diff/as-operations snapshot form))]
              (asyncm/<? (update-form-with-fragment form fragments))
              (reset! last-saved-snapshot form))
            (catch js/Error error
              (prn error)
              (dispatch [:snackbar-message
                         [(str "Toinen käyttäjä teki muutoksen lomakkeeseen \"" (some #(get-in form [:name %])
                                                                                      [:fi :sv :en]) "\"")
                          "Lataa sivu uudelleen ja tarkista omat muutokset"]])
              (dispatch-flasher-error-msg :post error)))))
      (recur))))

(save-loop save-chan)

(reg-event-db :editor/save-form
  (fn [db [_ form old-form]]
    (async/put! save-chan [form old-form])
    db))

(defn- post-new-form
  [form]
  (post "/lomake-editori/api/forms"
        form
        (fn [db form]
          (let [stop-fn (get-in db [:editor :autosave])
                path (str "/lomake-editori/editor/" (:key (languages->kwd form)))]
            (autosave/stop-autosave! stop-fn)
            (set-history! path)
            (assoc-in db [:editor :new-form-created?] true)))))

(reg-event-db
  :editor/add-form
  (fn [db _]
    (post-new-form
      {:name      {:fi "Uusi lomake"}
       :content   [(component/hakukohteet) (pm/person-info-module)]
       :languages [:fi]
       :locked    nil
       :locked-by nil})
    db))




(defn- copy-form [db _]
  (let [form-id                  (get-in db [:editor :selected-form-key])
        new-form-key             (cu/new-uuid)
        form                     (-> (get-in db [:editor :forms form-id])
                                     (update :name (fn [name]
                                                     (reduce-kv #(assoc %1 %2 (str %3 " - KOPIO"))
                                                       {}
                                                       name))))
        reset-selection-group-id (fn [x] (if (get-in x [:params :selection-group-id])
                                           (assoc-in x [:params :selection-group-id] new-form-key)
                                           x))
        set-hidden-if-belongs-to (fn [x]
                                     (cond

                                       (and
                                         (or (boolean (:belongs-to-hakukohderyhma x))
                                             (boolean (:belongs-to-hakukohteet x)))
                                         (or (boolean (:params x))
                                             (= "adjacentfieldset" (:fieldType x))))
                                       (assoc-in x [:params :hidden] true)


                                       (or (boolean (:belongs-to-hakukohderyhma x))
                                           (boolean (:belongs-to-hakukohteet x)))
                                       (assoc x :hidden true)

                                       :else
                                       x))
        remove-belongs-to (fn [x] (if (map? x)
                                        (-> x
                                            (dissoc :belongs-to-hakukohderyhma)
                                            (dissoc :belongs-to-hakukohteet))
                                        x))
        properties (-> (or (get-in form [:properties]) {})
                       (select-keys [:allow-hakeminen-tunnistautuneena]))]
    (post-new-form (merge
                     (-> (select-keys form [:name :content :languages :organization-oid])
                         (update :content (fn [content]
                                            (map (fn [component] (walk/prewalk
                                                                   #(-> %
                                                                        (reset-selection-group-id)
                                                                        (set-hidden-if-belongs-to)
                                                                        (remove-belongs-to)) component))
                                                 content)))
                         (assoc :key new-form-key))
                     {:locked nil :locked-by nil}
                     {:properties properties}))
    db))

(reg-event-db :editor/copy-form copy-form)

(defn- removed-application-review-active? [db]
  (let [selected-form-key (get-in db [:editor :selected-form-key])
        form-in-review    (get-in db [:application :selected-application-and-form :form])]
    (= selected-form-key (:key form-in-review))))

(defn- reset-application-review-state [db]
  (assoc db :application {}))

(reg-event-fx
  :editor/confirm-remove-form
  (fn [{:keys [db]} _]
    (let [form-key (get-in db [:editor :selected-form-key])
          form-id  (get-in db [:editor :forms form-key :id])]
      (-> {:db   (cond-> (-> db
                             (update :editor dissoc :selected-form-key)
                             (assoc-in [:editor :ui :remove-form-button-state] :disabled))
                   (removed-application-review-active? db)
                   (reset-application-review-state))
           :http {:method              :delete
                  :path                (str "/lomake-editori/api/forms/" form-id)
                  :handler-or-dispatch :editor/refresh-forms-for-editor}}))))

(reg-event-db
  :editor/unstart-remove-form
  (fn [db _]
    (cond-> db
      (= :confirm (get-in db [:editor :ui :remove-form-button-state]))
      (assoc-in [:editor :ui :remove-form-button-state] :active))))

(reg-event-fx
  :editor/start-remove-form
  (fn [{db :db} _]
    {:db (assoc-in db [:editor :ui :remove-form-button-state] :confirm)}))

(reg-event-db
  :editor/change-form-name
  (fn [db [_ lang new-form-name]]
    (with-form-key [db selected-form-key]
      (assoc-in db [:editor :forms selected-form-key :name lang] new-form-name))))

(reg-event-db
  :editor/change-form-organization
  (fn [db [_ new-form-organization-oid]]
    (with-form-key [db selected-form-key]
      (update-in db [:editor :forms selected-form-key]
                 assoc :organization-oid
                 new-form-organization-oid))))

(defn- remove-component-from-list
  [db form-key path]
  (update-in db (concat [:editor :forms form-key :content] (butlast path))
             #(util/remove-nth % (last path))))

(defn- add-component-to-list
  [db form-key component path]
  (update-in db (concat [:editor :forms form-key :content] (butlast path))
             #(util/add-to-nth (if (nil? %) [] %) (last path) component)))

(defn- common-path [source-path target-path]
  (loop [common   []
         [s & ss] source-path
         [t & tt] target-path]
    (if (and s t (= s t))
      (recur (conj common s) ss tt)
      [common (cons s ss) (cons t tt)])))

(defn- recalculate-target-path-prevent-oob
  [source-path target-path]
  (let [[common [s & ss] [t & tt]] (common-path source-path target-path)]
    (if (and (empty? ss) (< s t))
      ;; moving forward and maybe inward inside a common parent component
      (vec (concat common [(dec t)] tt))
      target-path)))

(defn clear-copy-component [db]
  (let [copy-component-form-key (get-in db [:editor :copy-component :copy-component-form-key])]
    (cond-> (update db :editor dissoc :copy-component)
            (not= copy-component-form-key (get-in db [:editor :selected-form-key]))
            (update-in [:editor :forms copy-component-form-key] assoc :content []))))

(defn paste-component
  [db [_
       {:keys [copy-component-form-key
               copy-component-path
               copy-component-content
               copy-component-cut?]}
       target-path]]
  (or
   (with-form-key [db form-key]
     (when (or (not copy-component-cut?) (= form-key copy-component-form-key))
       (let [copy?                             (not copy-component-cut?)
             component                         copy-component-content
             target-path                       (if copy?
                                                 target-path
                                                 (recalculate-target-path-prevent-oob copy-component-path target-path))
             result-is-nested-component-group? (and
                                                (contains? (set target-path) :children)
                                                (= "wrapperElement" (:fieldClass component)))
             adjacent-fieldset-cut?            (and
                                                 copy-component-cut?
                                                 (= "adjacentfieldset" (:fieldType component)))]
         (when (or (not result-is-nested-component-group?) adjacent-fieldset-cut?)
           (if copy?
             (let [reset-uuid               (fn [x] (if (and (:id x) (cu/valid-uuid? (:id x)))
                                                      (assoc x :id (cu/new-uuid))
                                                      x))
                   reset-selection-group-id (fn [x] (if (get-in x [:params :selection-group-id])
                                                      (assoc-in x [:params :selection-group-id] form-key)
                                                      x))
                   component                (walk/prewalk
                                             #(-> %
                                                  (reset-uuid)
                                                  (reset-selection-group-id)) component)
                   db                       (-> db
                                                (add-component-to-list form-key component target-path)
                                                (clear-copy-component))]
               (if (not= form-key copy-component-form-key)
                 (update-in db [:editor :forms copy-component-form-key] assoc :content [])
                 db))
             (-> db
                 (remove-component-from-list form-key copy-component-path)
                 (add-component-to-list form-key component target-path)
                 (clear-copy-component)))))))
   db))

(reg-event-db :editor/paste-component paste-component)

(reg-event-db
  :editor/copy-component
  (fn copy-component [db [_ path cut?]]
    (let [selected-form-key (-> db :editor :selected-form-key)
          selected-content (get-in db (into [:editor :forms selected-form-key :content] path))]
      (assoc-in db [:editor :copy-component] {:copy-component-form-key   selected-form-key
                                              :copy-component-path       path
                                              :copy-component-cut?       cut?
                                              :copy-component-content    selected-content
                                              :copy-component-unique-ids (->> (get-in db (db/current-form-content-path db path))
                                                                              (collect-ids [])
                                                                              (remove cu/valid-uuid?)
                                                                              set)}))))

(reg-event-db
  :editor/cancel-copy-component
  (fn [db _]
    (clear-copy-component db)))

(def ^:private lang-order
  [:fi :sv :en])

(defn- index-of [coll t]
  (let [length (count coll)]
    (loop [idx 0]
      (cond
        (<= length idx) -1
        (= (get coll idx) t) idx
        :else (recur (inc idx))))))

(defn- toggle [x set]
  (if (contains? set x)
    (disj set x)
    (conj set x)))

(defn- if-empty [default seq]
  (if (empty? seq) default seq))

(defn- toggle-language [db [_ lang]]
  (let [selected (get-in db [:editor :selected-form-key])]
    (-> db
        (update-in [:editor :forms selected :languages]
                   (fn [languages]
                     (->> (set languages)
                          (toggle lang)
                          (if-empty languages)
                          (sort-by (partial index-of lang-order)))))
        (update-in [:editor :ui]
                   (fn [ui]
                     (walk/prewalk
                      (fn [x]
                        (if (= [:focus? true] x)
                          [:focus? false]
                          x))
                      ui))))))

(reg-event-db :editor/toggle-language toggle-language)

(reg-event-fx
  :editor/fetch-koodisto-for-component-with-id
  (fn [_ [_ id {:keys [uri version allow-invalid?]}]]
    {:http {:method              :get
            :path                (str "/lomake-editori/api/koodisto/" uri "/" version "?allow-invalid=" allow-invalid?)
            :handler-or-dispatch :editor/set-new-koodisto-while-keeping-existing-followups
            :handler-args        {:id id :uri uri :version version}}}))

(reg-event-fx
  :editor/set-new-koodisto-while-keeping-existing-followups
  (fn [{db :db} [_ new-koodisto {:keys [id uri version]}]]
    (let [key                       (get-in db [:editor :selected-form-key])
          form                      (get-in db [:editor :forms key :content])
          lang                      (keyword (get-in db [:editor :user-info :lang]))
          new-options               (->> new-koodisto
                                         (map #(select-keys % [:value :label]))
                                         (sort-by (comp lang :label))
                                         vec)
          update-koodisto-component (fn [component]
                                      (assoc component :options
                                             (update-options-while-keeping-existing-followups new-options (:options component))))
          find-koodisto-component   (fn [component]
                                      (if (and (= id (:id component))
                                               (= uri (get-in component [:koodisto-source :uri]))
                                               (= version (get-in component [:koodisto-source :version])))
                                        (update-koodisto-component component)
                                        component))
          updated-form              (walk/prewalk find-koodisto-component form)
          updated-db                (assoc-in db [:editor :forms key :content] updated-form)]
      {:db updated-db
       :dispatch [:editor/remove-from-load-spinner]})))

(reg-event-fx
  :editor/fetch-attachment-types-koodisto
  (fn [{db :db}]
    (when (not (seq (get-in db [:editor :attachment-types-koodisto])))
      {:http {:method               :get
              :path                 (str "/lomake-editori/api/koodisto/liitetyypitamm/1?allow-invalid=false")
              :handler-or-dispatch  :editor/set-attachment-types-koodisto}})))

(reg-event-db
  :editor/set-attachment-types-koodisto
  (fn [db [_ koodisto]]
    (let [lang             (keyword (get-in db [:editor :user-info :lang]))
          attachment-types (->> koodisto
                                (map #(select-keys % [:value :label :uri]))
                                (sort-by (comp lang :label))
                                vec)]
      (assoc-in db [:editor :attachment-types-koodisto] attachment-types))))

(reg-event-fx
  :editor/show-belongs-to-hakukohteet-modal
  (fn [{db :db} [_ id]]
    (cond-> {:db (assoc-in db [:editor :ui id :belongs-to-hakukohteet :modal :show] true)}
      (get-in db [:editor :used-by-haut :error?])
      (assoc :dispatch [:editor/refresh-used-by-haut]))))

(reg-event-db
  :editor/hide-belongs-to-hakukohteet-modal
  (fn [db [_ id]]
    (assoc-in db [:editor :ui id :belongs-to-hakukohteet :modal :show] false)))

(reg-event-db
  :editor/belongs-to-hakukohteet-modal-show-more
  (fn [db [_ id haku-oid]]
      (update-in db [:editor :ui id :belongs-to-hakukohteet :modal haku-oid :show-more-value] + 15)))

(reg-event-db
  :editor/set-belongs-to-hakukohteet-modal-search-term
  (fn [db [_ id search-term]]
    (if (< (count search-term) 3)
      (update-in db [:editor :ui id :belongs-to-hakukohteet :modal] dissoc :search-term)
      (assoc-in db [:editor :ui id :belongs-to-hakukohteet :modal :search-term] search-term))))

(reg-event-fx
  :editor/on-belongs-to-hakukohteet-modal-search-term-change
  (fn [{db :db} [_ id search-term]]
    {:db (assoc-in db [:editor :ui id :belongs-to-hakukohteet :modal :search-term-value]
                   search-term)
     :dispatch-debounced {:timeout 200
                          :id [:belongs-to-hakukohteet-search id]
                          :dispatch [:editor/set-belongs-to-hakukohteet-modal-search-term
                                     id search-term]}}))

(defn remove-option-path [path]
  (if (= :options (last (butlast path)))
    (vec (butlast (butlast path)))
    path))


(reg-event-db
  :editor/toggle-element-visibility-on-form
  (fn [db [_ path]]
    (let [content-path (conj (vec (db/current-form-content-path db path))
                             :params :hidden)]
      (-> db
          (update-in content-path #(boolean (not %)))
          (update-modified-by [path])))))

(reg-event-db
  :editor/toggle-option-visibility-on-form
  (fn [db [_ path]]
    (let [option-path  (vec (db/current-form-content-path db path))
          content-path (conj option-path :hidden)
          is-hidden? (-> db
                         (get-in content-path)
                         boolean)
          flip (fn [db]
                 (if is-hidden?
                   (update-in db option-path dissoc :hidden)
                   (assoc-in db content-path true)))]
      (-> db
          (flip)
          (update-modified-by [(remove-option-path path)])))))


(reg-event-db
  :editor/add-to-belongs-to-hakukohteet
  (fn [db [_ path oid]]
    (let [content-path (conj (vec (db/current-form-content-path db path))
                         :belongs-to-hakukohteet)]
      (-> db
          (update-in content-path (fnil (comp vec #(conj % oid) set) []))
          (update-modified-by [(remove-option-path path)])))))

(reg-event-db
  :editor/remove-from-belongs-to-hakukohteet
  (fn [db [_ path oid]]
    (let [content-path (conj (vec (db/current-form-content-path db path))
                        :belongs-to-hakukohteet)]
      (-> db
          (update-in content-path (fnil (comp vec #(disj % oid) set) []))
          (update-modified-by [(remove-option-path path)])))))

(reg-event-db
  :editor/add-to-belongs-to-hakukohderyhma
  (fn [db [_ path oid]]
    (let [content-path (conj (vec (db/current-form-content-path db path))
                        :belongs-to-hakukohderyhma)]
      (-> db
          (update-in content-path (fnil (comp vec #(conj % oid) set) []))
          (update-modified-by [(remove-option-path path)])))))

(reg-event-db
  :editor/remove-from-belongs-to-hakukohderyhma
  (fn [db [_ path oid]]
    (let [content-path                                (conj (vec (db/current-form-content-path db path))
                                                        :belongs-to-hakukohderyhma)
          belongs-to-one-hakukohderyhma               (= (count (get-in db content-path)) 1)
          remove-per-hakukohde-if-last-hakukohderyhma (fn [db]
                                                        (if belongs-to-one-hakukohderyhma
                                                          (update-in
                                                            db
                                                            (db/current-form-content-path db path)
                                                            #(dissoc % :per-hakukohde))
                                                          db))]
      (-> db
        (remove-per-hakukohde-if-last-hakukohderyhma)
        (update-in content-path (fnil (comp vec #(disj % oid) set) []))
        (update-modified-by [(remove-option-path path)])))))

(reg-event-db
  :editor/clean-per-hakukohde-followups
  (let [is-mail-attachment?
        (fn [field]
          (and
            (= "attachment" (:fieldType field))
            (get-in field [:params :mail-attachment?])))

        remove-mail-attachment-per-hakukohde-info
        (fn [field]
          (update field :params #(dissoc % :fetch-info-from-kouta? :attachment-type)))

        clean-followups
        (fn [followup]
          (if (is-mail-attachment? followup)
            (remove-mail-attachment-per-hakukohde-info followup)
            followup))

        clean-options
        (fn [option]
          (update option :followups (comp vec (partial map clean-followups))))

        clean-field
        (fn [field]
          (update field :options (comp vec (partial map clean-options))))]
    (fn [db [_ path]]
      (let [field-path (db/current-form-content-path db [path])
            field      (get-in db field-path)
            new-field  (clean-field field)]
        (assoc-in db field-path new-field)))))

(reg-event-db
  :editor/fold
  (fn [db [_ id]]
    (fold db id)))

(reg-event-db
  :editor/unfold
  (fn [db [_ id]]
    (unfold db id)))

(reg-event-db
  :editor/fold-all
  (fn [db _]
    (fold-all db)))

(reg-event-db
  :editor/unfold-all
  (fn [db _]
    (reduce unfold db (keys (get-in db [:editor :ui])))))

(reg-event-fx
  :editor/toggle-email-template-editor
  (fn [{db :db} _]
    (let [now-visible? (-> db
                           (get-in [:editor :ui :template-editor-visible?])
                           (not))]
      (cond-> {:db (assoc-in db [:editor :ui :template-editor-visible?] now-visible?)}
              now-visible? (merge {:dispatch [:editor/load-email-template]})))))

(reg-event-db
  :editor/update-form-lock
  (fn [db [_ response args]]
    (let [form-key (-> args :form-key)]
      (-> db
          (assoc-in [:editor :forms form-key :locked] (:locked response))
          (assoc-in [:editor :forms form-key :locked-by] (-> db :editor :user-info :name))
          (assoc-in [:editor :forms form-key :id] (:id response))
          (assoc-in [:editor :forms form-key :lock-state] (if (:locked response)
                                                            :locked
                                                            :open))))))

(reg-event-fx
  :editor/toggle-form-editing-lock
  (fn [{db :db} _]
    (let [form-key         (-> db :editor :selected-form-key)
          form             (get-in db [:editor :forms form-key])
          operation        (if (= (get-in db [:editor :forms form-key :lock-state]) :locked)
                             "open"
                             "close")
          transition-state (case operation
                             "open" :opening
                             "close" :closing)]
      {:db   (assoc-in db
                       [:editor :forms form-key :lock-state]
                       transition-state)
       :http {:method              :put
              :path                (str "/lomake-editori/api/forms/" (:id form) "/lock/" operation)
              :handler-or-dispatch :editor/update-form-lock
              :handler-args        {:form-key form-key}}})))

(defn- descendant-paths
  [field path]
  (into [path]
        (concat
         (for [[i child]       (map vector (range) (:children field))
               descendant-path (descendant-paths child (conj path :children i))]
           descendant-path)
         (for [[i option]      (map vector (range) (:options field))
               [j followup]    (map vector (range) (:followups option))
               descendant-path (descendant-paths followup (conj path :options i :followups j))]
           descendant-path))))

(reg-event-db
  :editor/toggle-component-lock
  (fn [db [_ path]]
    (let [field      (get-in db (vec (db/current-form-content-path db path)))
          new-locked (not (get-in field [:metadata :locked]))]
      (reduce (fn [db path]
                (-> db
                    (assoc-in (vec (db/current-form-content-path db [path :metadata :locked])) new-locked)
                    (update-modified-by [path])))
              db
              (descendant-paths field (vec path))))))

(defn- add-stored-content-to-templates
  [previews]
  (map #(assoc % :stored-content (dissoc % :stored-content)) previews))

(defn- nils-to-string [m]
  (into {} (for [[k v] m] [k (or v "")])))

(defn- preview-map-to-list
  [previews]
  (let [contents              (->> previews
                                   vals
                                   (map #(select-keys % [:lang :content :content-ending :subject :signature])))
        contents-changed      @(subscribe [:editor/email-templates-altered])
        only-changed-contents (->> contents
                                   (filter #(get contents-changed (:lang %)))
                                   (map nils-to-string))]
    only-changed-contents))

(defn- preview-list-to-map
  [previews]
  (util/group-by-first :lang previews))

(defn- stored-preview-list-to-map
  [previews]
  (->> previews
      (add-stored-content-to-templates)
      (util/group-by-first :lang)))

(reg-event-fx
  :editor/update-email-preview-immediately
  (fn [{db :db} [_]]
    (let [form-key              (get-in db [:editor :selected-form-key])
          form-allows-ht?       (boolean (get-in db [:editor :forms form-key :properties :allow-hakeminen-tunnistautuneena]))
          contents              (preview-map-to-list (get-in db [:editor :email-template form-key]))]
      {:http {:method              :post
              :params              {:contents contents}
              :handler-args        {:form-key form-key}
              :path                (str "/lomake-editori/api/email-template/" form-key "/previews?form-allows-ht=" form-allows-ht?)
              :handler-or-dispatch :editor/update-email-template-preview}})))

(reg-event-fx
  :editor/update-email-preview
  (fn [{db :db} [_ lang path content]]
    {:db                 (assoc-in db [:editor :email-template (get-in db [:editor :selected-form-key]) lang path] content)
     :dispatch-debounced {:timeout  500
                          :id       :email-template-preview
                          :dispatch [:editor/update-email-preview-immediately]}}))

(reg-event-db
  :editor/update-email-template-preview
  (fn [db [_ contents {form-key :form-key}]]
    (update-in db
               [:editor :email-template form-key]
               (partial merge-with merge)
               (preview-list-to-map contents))))

(reg-event-db
  :editor/update-saved-email-template-preview
  (fn [db [_ contents {form-key :form-key}]]
    (assoc-in db
              [:editor :email-template form-key]
              (stored-preview-list-to-map contents))))

(reg-event-fx
  :editor/save-email-template
  (fn [{db :db} [_]]
    (let [contents (preview-map-to-list (get-email-template db))
          form-key (get-in db [:editor :selected-form-key])
          form-allows-ht? (boolean (get-in db [:editor :forms form-key :properties :allow-hakeminen-tunnistautuneena]))]
      {:http {:method              :post
              :params              {:contents contents}
              :handler-args        {:form-key form-key}
              :path                (str "/lomake-editori/api/email-templates/" form-key "?form-allows-ht=" form-allows-ht?)
              :handler-or-dispatch :editor/update-saved-email-template-preview}})))

(reg-event-fx
  :editor/load-email-template
  (fn [{db :db} [_]]
    (let [form-key (get-in db [:editor :selected-form-key])
          form-allows-ht? (boolean (get-in db [:editor :forms form-key :properties :allow-hakeminen-tunnistautuneena]))]
      {:http {:method              :get
              :handler-args        {:form-key form-key}
              :path                (str "/lomake-editori/api/email-templates/" form-key "?form-allows-ht=" form-allows-ht?)
              :handler-or-dispatch :editor/update-saved-email-template-preview}})))

(reg-event-fx
  :editor/update-organization-select-query
  (fn [{db :db} [_ query]]
    {:db                 (-> db
                             (assoc-in [:editor :organizations :query] query)
                             (assoc-in [:editor :organizations :results-page] 0))
     :dispatch-debounced {:timeout  500
                          :id       [:organization-query]
                          :dispatch [:editor/do-organization-query]}}))

(reg-event-fx
  :editor/toggle-organization-select-filter
  (fn [{db :db} [_ id]]
    {:db                 (-> db
                             (update-in [:editor :organizations id] not)
                             (assoc-in [:editor :organizations :results-page] 0))
     :dispatch-debounced {:timeout  500
                          :id       [:organization-query]
                          :dispatch [:editor/do-organization-query]}}))

(reg-event-fx
  :editor/do-organization-query
  (fn [{db :db} [_]]
    {:http {:method              :get
            :path                (str "/lomake-editori/api/organization/user-organizations?query="
                                      (-> db :editor :organizations :query)
                                      "&organizations=" (if (-> db :editor :organizations :org-select-organizations) "true" "false")
                                      "&hakukohde-groups=" (if (-> db :editor :organizations :org-select-hakukohde-groups) "true" "false")
                                      "&results-page=" (get-in db [:editor :organizations :results-page] 0))
            :handler-or-dispatch :editor/update-organization-query-results}}))

(reg-event-fx
  :application/do-organization-query-for-schools-of-departure
  (fn [{db :db} [_]]
    (when (empty? (get-in db [:editor :organizations :select]))
      {:http {:method              :get
              :path                "/lomake-editori/api/organization/user-organizations?organizations=true&hakukohde-groups=false&lahtokoulu-only=true&results-page=10000"
              :skip-parse-times?   true
              :handler-or-dispatch :editor/update-organization-query-results-for-schools-of-departure}})))

(reg-event-fx
 :application/do-organization-query-for-schools-of-departure-without-lahtokoulu
 (fn [{db :db} [_]]
   (when (empty? (get-in db [:editor :organizations :select]))
     {:http {:method              :get
             :path                "/lomake-editori/api/organization/user-organizations?organizations=true&hakukohde-groups=false&lahtokoulu-only=false&results-page=10000"
             :skip-parse-times?   true
             :handler-or-dispatch :editor/update-organization-query-results-for-schools-of-departure}})))

(reg-event-fx
  :editor/update-organization-query-results
  (fn [{db :db} [_ results]]
    {:db (assoc-in db [:editor :organizations :matches] results)}))

(reg-event-fx
  :editor/update-organization-query-results-for-schools-of-departure
  (fn [{db :db} [_ results]]
    (if (= (count results) 1)
      {:db (-> db
               (assoc-in [:editor :organizations :schools-of-departure] results)
               (assoc-in [:editor :organizations :schools-of-departure-filtered] results))
       :dispatch [:application/set-school-filter (:oid (first results))]}
      {:db (assoc-in db [:editor :organizations :schools-of-departure] results)})))

(defn- filter-organizations
  [orgs query lang]
  (let [query-lower-case (string/lower-case query)
        pred             (fn [org]
                           (or (string/includes? (string/lower-case (str "" (get-in org (:name lang)))) query-lower-case)
                               (string/includes? (str "" (:oid org)) query)))]
    (filter pred orgs)))

(reg-event-db
  :editor/filter-organizations-for-school-of-departure
  (fn [db [_ query]]
    (let [lang                    (keyword (get-in db [:editor :user-info :lang]))
          organizations           (get-in db [:editor :organizations :schools-of-departure])
          filtered-organizations  (filter-organizations organizations query lang)]
      (assoc-in db [:editor :organizations :schools-of-departure-filtered] filtered-organizations))))

(reg-event-db
  :editor/clear-filter-organizations-for-school-of-departure
  (fn [db]
    (assoc-in db [:editor :organizations :schools-of-departure-filtered] [])))

(reg-event-fx
  :editor/select-organization
  (fn [{db :db} [_ oid]]
    (let [rights-sans-opinto-ohjaaja (->> user-rights/right-names
                                          (remove #(= :opinto-ohjaaja %)))]
      {:http {:method              :post
              :path                (str "/lomake-editori/api/organization/user-organization/"
                                        oid
                                        "?rights="
                                        (string/join "&rights=" (map name rights-sans-opinto-ohjaaja)))
              :handler-or-dispatch :editor/update-selected-organization}
       :db   (assoc-in db [:editor :user-info :selected-organization :rights] rights-sans-opinto-ohjaaja)}
      )))

(reg-event-fx
  :editor/increase-organization-result-page
  (fn [{:keys [db]} _]
    {:db       (update-in db [:editor :organizations :results-page] inc)
     :dispatch [:editor/do-organization-query]}))

(reg-event-fx
  :editor/update-selected-organization
  (fn [{db :db} [_ selected-organization]]
    {:db (assoc-in db
                   [:editor :user-info :selected-organization]
                   (not-empty selected-organization))}
    (.reload js/location)))

(reg-event-fx
  :editor/remove-selected-organization
  (fn [_]
    {:http {:method              :delete
            :path                "/lomake-editori/api/organization/user-organization"
            :handler-or-dispatch :editor/update-selected-organization}}))

(reg-event-fx
  :editor/update-selected-organization-rights
  (fn [{db :db} [_ right selected?]]
    (let [db (if selected?
               (update-in db [:editor :user-info :selected-organization :rights] conj (name right))
               (update-in db [:editor :user-info :selected-organization :rights] (partial remove #{(name right)})))
          rights (->> db :editor :user-info :selected-organization :rights
                      not-empty)]
      {:db   db
       :http {:method              :post
              :path                (str "/lomake-editori/api/organization/user-organization/"
                                        (-> db :editor :user-info :selected-organization :oid)
                                        (when rights
                                          (str "?rights=" (string/join "&rights=" rights))))
              :handler-or-dispatch :editor/update-selected-organization}})))

(reg-event-db
  :editor/toggle-autosave
  (fn [db _]
    (let [form-key         (get-in db [:editor :selected-form-key])
          form             (get-in db [:editor :forms form-key])
          autosave-stop-fn (get-in db [:editor :autosave])]
      (if (fn? autosave-stop-fn)
        (when (autosave-stop-fn)
          (-> db
              (assoc-in [:editor :last-autosaved-form] form)
              (update :editor dissoc :autosave)))
        (let [new-autosave (when form
                             (create-autosave-loop form (get-in db [:editor :last-autosaved-form])))]
          (-> db
              (update :editor dissoc :last-autosaved-form)
              (assoc-in [:editor :autosave] new-autosave)))))))

(reg-event-db
  :editor/toggle-auto-expand-hakukohteet
  (fn [db _]
    (let [form-path (db/current-form-properties-path db [:auto-expand-hakukohteet])]
      (update-in db form-path not))))

(reg-event-db
 :editor/toggle-order-hakukohteet-by-opetuskieli
 (fn [db _]
   (let [form-path (db/current-form-properties-path db [:order-hakukohteet-by-opetuskieli])]
     (update-in db form-path not))))

(reg-event-db
  :editor/add-invalid-value-validator
  (fn [db [_ option-value parent-path]]
    (-> db
      (update-in
        (db/current-form-content-path db [parent-path :params :invalid-values])
        (fn [invalid-values]
          (-> invalid-values
            set
            (conj option-value)
            vec)))
      (add-validator [nil "invalid-values" parent-path]))))

(reg-event-db
  :editor/remove-invalid-value-validator
  (fn [db [_ option-value parent-path]]
    (let [invalid-values-path    (db/current-form-content-path db [parent-path :params :invalid-values])
          invalid-values         (get-in db invalid-values-path)
          updated-invalid-values (->> invalid-values
                                   (remove #(= option-value %))
                                   vec)
          updated-db             (assoc-in db invalid-values-path updated-invalid-values)]
      (if (empty? updated-invalid-values)
        (remove-validator updated-db [nil "invalid-values" parent-path])
        updated-db))))

(reg-event-db
  :editor/toggle-allow-hakeminen-tunnistautuneena
  (fn [db [_]]
    (let [path (db/current-form-properties-path db [:allow-hakeminen-tunnistautuneena])
          value (not (get-in db path))]
      (assoc-in db path value))))

(reg-event-db
  :editor/toggle-allow-only-yhteishaut
  (fn [db [_]]
    (let [path (db/current-form-properties-path db [:allow-only-yhteishaut])
          value (not (get-in db path))]
    (assoc-in db path value))))

(reg-event-db
  :editor/toggle-lomakkeeseen-liittyy-maksutoiminto
  (fn [db [_]]
    (let [path (db/current-form-properties-path db [:payment])
          value (get-in db path)]
      (if (not-empty value)
        (assoc-in db path {})
        (assoc-in
          db
          path
          {:type "payment-type-tutu"
           :decision-fee nil
           :processing-fee (config/get-public-config
                             [:tutu-default-processing-fee])})))))

(reg-event-db
  :editor/change-maksutyyppi
  (fn [db [_ maksutyyppi]]
    (let [path (db/current-form-properties-path db [:payment])]
      (assoc-in
        db
        path
        (case maksutyyppi
          "payment-type-tutu"
          {:type maksutyyppi
           :decision-fee nil
           :processing-fee (config/get-public-config
                             [:tutu-default-processing-fee])}
          "payment-type-astu"
          {:type maksutyyppi
           :decision-fee nil
           :processing-fee nil
           :vat "0"
           :order-id-prefix (first astu-order-id-prefixes)})))))

(reg-event-db
  :editor/change-processing-fee
  (fn [db [_ processing-fee]]
    (let [path (db/current-form-properties-path db [:payment :processing-fee])]
      (assoc-in db path processing-fee))))

(reg-event-db
  :editor/change-vat
  (fn [db [_ vat]]
    (let [path (db/current-form-properties-path db [:payment :vat])]
      (assoc-in db path vat))))

(reg-event-db
  :editor/change-order-id-prefix
  (fn [db [_ order-id-prefix]]
    (let [path (db/current-form-properties-path db [:payment :order-id-prefix])]
      (assoc-in db path order-id-prefix))))

(reg-event-db
  :editor/toggle-close-form
  (fn [db [_]]
    (let [path (db/current-form-properties-path db [:closed])
          value (not (get-in db path))]
      (assoc-in db path value))))

(reg-event-db
  :editor/update-selected-property-options
  (fn [db [_ category selected-option-ids]]
    (let [path (vec (db/current-form-properties-path db [(keyword category)]))]
      (assoc-in db (conj path :selected-option-ids) selected-option-ids))))

(reg-event-db
  :editor/set-property-value
  (fn [db [_ category property value]]
    (let [path (db/current-form-properties-path db [(keyword category)(keyword property)])]
      (assoc-in db path value))))

(reg-event-db
  :editor/toggle-property-value
  (fn [db [_ category property]]
    (let [path (db/current-form-properties-path db [(keyword category)(keyword property)])
          value (not (get-in db path))]
      (assoc-in db path value))))
