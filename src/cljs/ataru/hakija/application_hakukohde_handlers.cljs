(ns ataru.hakija.application-hakukohde-handlers
  (:require
    [clojure.string :as string]
    [clojure.set :as s]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
    [ataru.util :as util]
    [ataru.hakija.handlers-util :as handlers-util]
    [ataru.hakija.application-handlers :refer [set-field-visibilities
                                               set-validator-processing
                                               check-schema-interceptor]]))

(defn hakukohteet-field [db]
  (->> (:flat-form-content db)
       (filter #(= "hakukohteet" (:id %)))
       first))

(defn- toggle-hakukohde-search
  [db]
  (update-in db [:application :show-hakukohde-search] not))

(defn query-hakukohteet [hakukohde-query lang virkailija? tarjonta-hakukohteet hakukohteet-field]
  (let [non-archived-hakukohteet (filter #(not (:archived %)) tarjonta-hakukohteet)
        non-archived-hakukohteet-oids (set (map :oid non-archived-hakukohteet))
        order-by-hakuaika (if virkailija?
                            #{}
                            (->> non-archived-hakukohteet
                                 (remove #(:on (:hakuaika %)))
                                 (map :oid)
                                 set))
        order-by-name #(util/non-blank-val (:label %) [lang :fi :sv :en])
        hakukohde-options (->> hakukohteet-field
                               :options
                               (filter #(contains? non-archived-hakukohteet-oids (:value %)))
                               (sort-by (juxt (comp order-by-hakuaika :value)
                                              order-by-name)))
        query-parts (map string/lower-case (string/split hakukohde-query #"\s+"))
        results (if (or (string/blank? hakukohde-query)
                        (< (count hakukohde-query) 2))
                  (map :value hakukohde-options)
                  (->> hakukohde-options
                       (filter
                         (fn [option]
                           (let [haystack (string/lower-case
                                            (str (get-in option [:label lang] (get-in option [:label :fi] ""))
                                                 (get-in option [:description lang] "")))]
                             (every? #(string/includes? haystack %) query-parts))))
                       (map :value)))
        [hakukohde-hits rest-results] (split-at 15 results)]
    {:hakukohde-hits  hakukohde-hits
     :rest-results    rest-results
     :hakukohde-query hakukohde-query}))

(reg-event-db
  :application/hakukohde-search-toggle
  [check-schema-interceptor]
  (fn [db _] (toggle-hakukohde-search db)))

(reg-event-db
  :application/hakukohde-query-process
  [check-schema-interceptor]
  (fn hakukohde-query-process [db [_ hakukohde-query-atom _]]
    (let [hakukohde-query @hakukohde-query-atom
          lang (-> db :form :selected-language)
          virkailija? (some? (get-in db [:application :virkailija-secret]))
          hakukohteet-field (hakukohteet-field db)
          tarjonta-hakukohteet (get-in db [:form :tarjonta :hakukohteet])
          {:keys [hakukohde-query
                  hakukohde-hits
                  rest-results]} (query-hakukohteet hakukohde-query lang virkailija? tarjonta-hakukohteet hakukohteet-field)]
      (-> db
          (assoc-in [:application :hakukohde-query] hakukohde-query)
          (assoc-in [:application :remaining-hakukohde-search-results] rest-results)
          (assoc-in [:application :hakukohde-hits] hakukohde-hits)))))

(reg-event-fx
  :application/hakukohde-query-change
  [check-schema-interceptor]
  (fn [{_db :db} [_ hakukohde-query-atom idx]]
    {:dispatch-debounced {:timeout  500
                          :id       :hakukohde-query
                          :dispatch [:application/hakukohde-query-process
                                     hakukohde-query-atom
                                     idx]}}))

(reg-event-db
  :application/show-more-hakukohdes
  [check-schema-interceptor]
  (fn [db _]
    (let [remaining-results (-> db :application :remaining-hakukohde-search-results)
          [more-hits rest-results] (split-at 15 remaining-results)]
      (-> db
          (assoc-in [:application :remaining-hakukohde-search-results] rest-results)
          (update-in [:application :hakukohde-hits] concat more-hits)))))

(reg-event-fx
  :application/set-hakukohde-valid
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ valid?]]
    {:db       (assoc-in db [:application :answers :hakukohteet :valid] valid?)
     :dispatch [:application/set-validator-processed :hakukohteet]}))

(reg-event-fx
  :application/validate-hakukohteet
  [check-schema-interceptor]
  (fn [{db :db} _]
    {:db                 (set-validator-processing db :hakukohteet)
     :validate-debounced {:value                        (get-in db [:application :answers :hakukohteet :value])
                          :tarjonta-hakukohteet         (get-in db [:form :tarjonta :hakukohteet])
                          :rajaavat-hakukohderyhmat     (get-in db [:form :rajaavat-hakukohderyhmat])
                          :priorisoivat-hakukohderyhmat (get-in db [:form :priorisoivat-hakukohderyhmat])
                          :answers-by-key               (get-in db [:application :answers])
                          :field-descriptor             (hakukohteet-field db)
                          :editing?                     (get-in db [:application :editing?])
                          :virkailija?                  (contains? (:application db) :virkailija-secret)
                          :on-validated                 (fn [[valid? _errors]]
                                                          (dispatch [:application/set-hakukohde-valid
                                                                     valid?]))}}))

(reg-event-fx
  :application/create-questions-per-hakukohde
  (fn [{db :db} [_ hakukohde-oid]]
    (let [questions (get-in db [:form :content])
          selected-hakukohteet (get-in db [:application :answers :hakukohteet :value])
          tarjonta-hakukohteet (get-in db [:form :tarjonta :hakukohteet])
          update-questions (handlers-util/sort-questions-and-first-level-children selected-hakukohteet
                            (reduce (partial handlers-util/duplicate-questions-for-hakukohde tarjonta-hakukohteet hakukohde-oid) [] questions))
          updated-answers (handlers-util/fill-missing-answer-for-hakukohde (get-in db [:application :answers]) update-questions)
          flat-form-content (util/flatten-form-fields update-questions)]
      {:db (-> db
               (assoc-in [:form :content] update-questions)
               (assoc-in [:application :answers] updated-answers)
               (assoc :flat-form-content flat-form-content)
               set-field-visibilities)})))

(reg-event-fx
  :application/hakukohde-clear-selection
  (fn [{db :db} [_ idx]]
    (let [selected-hakukohteet (vec (get-in db [:application :answers :hakukohteet :values]))
          hakukohde-oid (get-in selected-hakukohteet [idx :value])
          updated-hakukohteet (assoc selected-hakukohteet idx {:valid false :value ""})
          updated-db (-> db
                         (assoc-in [:application :answers :hakukohteet :values]
                                   updated-hakukohteet)
                         (assoc-in [:application :answers :hakukohteet :value]
                                   (mapv :value updated-hakukohteet))
                         set-field-visibilities)]
      {:db         updated-db
       :dispatch-n [[:application/validate-hakukohteet]
                    [:application/remove-questions-per-hakukohde hakukohde-oid]]})))

(reg-event-db
  :application/set-active-hakukohde-search
  (fn [db [_ active-search-idx]]
    (assoc-in db [:application :active-hakukohde-search] active-search-idx)))

(reg-event-fx
  :application/hakukohde-add-selection-2nd
  [check-schema-interceptor]
  (fn [{db :db} [_ hakukohde-oid idx]]
    (let [field-descriptor     (hakukohteet-field db)
          selected-hakukohteet (vec (get-in db [:application :answers :hakukohteet :values]))
          not-yet-selected?    (every? #(not= hakukohde-oid (:value %))
                                 selected-hakukohteet)
          add-hakukohde-fn     (fn [hakukohteet]
                                 (let [hakukohde {:valid true :value hakukohde-oid}
                                       default {:valid false :value ""}]
                                   (->> (range (max (inc idx) (count hakukohteet)))
                                        (mapv (fn [cur-idx]
                                                (let [cur (nth hakukohteet cur-idx nil)]
                                                  (if (= idx cur-idx)
                                                    hakukohde
                                                    (or cur default))))))))
          new-hakukohde-values (cond-> selected-hakukohteet
                                       not-yet-selected?
                                       add-hakukohde-fn)
          max-hakukohteet      (get-in field-descriptor [:params :max-hakukohteet] nil)
          db                   (-> db
                                   (assoc-in [:application :answers :hakukohteet :values]
                                             new-hakukohde-values)
                                   (assoc-in [:application :answers :hakukohteet :value]
                                             (mapv :value new-hakukohde-values))
                                   (update-in [:application :ui :hakukohteet :deleting] (comp set disj) hakukohde-oid)
                                   set-field-visibilities)]
      {:db         (cond-> db
                     (and (some? max-hakukohteet)
                       (<= max-hakukohteet (count new-hakukohde-values)))
                     (assoc-in [:application :show-hakukohde-search] false))
       :dispatch-n [[:application/validate-hakukohteet]
                    [:application/create-questions-per-hakukohde hakukohde-oid]]})))

(reg-event-fx
  :application/hakukohde-add-selection
  [check-schema-interceptor]
  (fn [{db :db} [_ hakukohde-oid]]
    (let [field-descriptor     (hakukohteet-field db)
          selected-hakukohteet (vec (get-in db [:application :answers :hakukohteet :values]))
          not-yet-selected?    (every? #(not= hakukohde-oid (:value %))
                                       selected-hakukohteet)
          new-hakukohde-values (cond-> selected-hakukohteet
                                       not-yet-selected?
                                       (conj {:valid true :value hakukohde-oid}))
          max-hakukohteet      (get-in field-descriptor [:params :max-hakukohteet] nil)
          db                   (-> db
                                   (assoc-in [:application :answers :hakukohteet :values]
                                             new-hakukohde-values)
                                   (assoc-in [:application :answers :hakukohteet :value]
                                             (mapv :value new-hakukohde-values))
                                   (update-in [:application :ui :hakukohteet :deleting] (comp set disj) hakukohde-oid)
                                   set-field-visibilities)]
      {:db                 (cond-> db
                                   (and (some? max-hakukohteet)
                                        (<= max-hakukohteet (count new-hakukohde-values)))
                                   (assoc-in [:application :show-hakukohde-search] false))
       :dispatch-n [[:application/validate-hakukohteet] [:application/create-questions-per-hakukohde hakukohde-oid]]})))

(defn- remove-question-duplicates-with-hakukohde
  [hakukohde-oid questions]
  (let [filterfn (partial filter #(not= (:duplikoitu-kysymys-hakukohde-oid %) hakukohde-oid))
        remove-duplicate-children (fn [question]
                                    (if (seq (:children question))
                                      (assoc question :children (filterfn (:children question)))
                                      question))]
    (->> questions
        (filterfn)
        (map remove-duplicate-children))))

(defn- remove-duplicates-with-hakukohde
  [m questions hakukohde-oid]
  (let [duplicate-question-ids (->> questions
                                    (util/flatten-form-fields)
                                    (filter #(or
                                               (= (:duplikoitu-kysymys-hakukohde-oid %) hakukohde-oid)
                                               (= (:duplikoitu-followup-hakukohde-oid %) hakukohde-oid)))
                                    (map #(keyword (:id %))))]
    (apply dissoc m duplicate-question-ids )))

(reg-event-fx
  :application/remove-questions-per-hakukohde
  (fn [{db :db} [_ hakukohde-oid]]
    (let [questions (get-in db [:form :content])
          answers (get-in db [:application :answers])
          ui (get-in db [:application :ui])
          answers-without-duplicates (remove-duplicates-with-hakukohde answers questions hakukohde-oid)
          ui-without-duplicates (remove-duplicates-with-hakukohde ui questions hakukohde-oid)
          update-questions (remove-question-duplicates-with-hakukohde hakukohde-oid questions)
          flat-form-content (util/flatten-form-fields update-questions)]
      {:db (-> db
               (assoc-in [:form :content] update-questions)
               (assoc-in [:application :answers] answers-without-duplicates)
               (assoc-in [:application :ui] ui-without-duplicates)
               (assoc :flat-form-content flat-form-content))})))

(reg-event-fx
  :application/add-empty-hakukohde-selection
  [check-schema-interceptor]
  (fn [{db :db} _]
    (let [current-hakukohteet (get-in db [:application :answers :hakukohteet :values])
          hakukohteet-count (count current-hakukohteet)
          repeat-times (if (zero? hakukohteet-count) 2 1)
          empty-hakukohde (->> {:valid false :value ""}
                               (repeat repeat-times)
                               vec)
          new-hakukohteet (vec (concat current-hakukohteet empty-hakukohde))]
      {:db (-> db
               (assoc-in [:application :answers :hakukohteet :values] new-hakukohteet)
               (assoc-in [:application :answers :hakukohteet :value] (mapv :value new-hakukohteet)))
       :dispatch [:application/validate-hakukohteet]})))

(reg-event-fx
  :application/hakukohde-remove
  [check-schema-interceptor]
  (fn [{db :db} [_ hakukohde-oid]]
    (let [selected-hakukohteet (get-in db [:application :answers :hakukohteet :values] [])
          new-hakukohde-values (vec (remove #(= hakukohde-oid (:value %)) selected-hakukohteet))
          db                   (-> db
                                   (assoc-in [:application :answers :hakukohteet :values]
                                             new-hakukohde-values)
                                   (assoc-in [:application :answers :hakukohteet :value]
                                             (mapv :value new-hakukohde-values))
                                   set-field-visibilities)]
      {:db                 db
       :dispatch-n [[:application/validate-hakukohteet] [:application/remove-questions-per-hakukohde hakukohde-oid]]})))

(reg-event-fx
  :application/hakukohde-remove-by-idx
  [check-schema-interceptor]
  (fn [{db :db} [_ idx]]
    (let [selected-hakukohteet (-> db
                                   (get-in [:application :answers :hakukohteet :values] [])
                                   vec) ;; Need to be vec because of subvec
          hakukohde-oid        (get-in selected-hakukohteet [idx :value])
          new-hakukohde-values (vec (concat
                                      (subvec selected-hakukohteet 0 idx)
                                      (subvec selected-hakukohteet (inc idx))))
          db                   (-> db
                                   (assoc-in [:application :answers :hakukohteet :values]
                                             new-hakukohde-values)
                                   (assoc-in [:application :answers :hakukohteet :value]
                                             (mapv :value new-hakukohde-values))
                                   set-field-visibilities)]
      {:db       db
       :dispatch-n [[:application/validate-hakukohteet]
                    [:application/remove-koulutustyyppi-filter idx]
                    [:application/remove-questions-per-hakukohde hakukohde-oid]]})))

(reg-event-fx
  :application/hakukohde-remove-selection
  [check-schema-interceptor]
  (fn [{db :db} [_ hakukohde-oid]]
    {:db             (update-in db [:application :ui :hakukohteet :deleting] (comp set conj) hakukohde-oid)
     :dispatch-later [{:ms       500
                       :dispatch [:application/hakukohde-remove hakukohde-oid]}]}))

(reg-event-fx
  :application/change-hakukohde-priority
  [check-schema-interceptor]
  (fn [{db :db} [_ hakukohde-oid index-change original-index]]
    (let [hakukohteet          (-> db :application :answers :hakukohteet :values vec)
          current-index        (if hakukohde-oid
                                 (first (keep-indexed #(when (= hakukohde-oid (:value %2))
                                                         %1)
                                          hakukohteet))
                                 original-index)
          new-index            (+ current-index index-change)
          new-hakukohteet      (assoc hakukohteet
                                 current-index (nth hakukohteet new-index)
                                 new-index (nth hakukohteet current-index))
          new-hakukohteet-oids (mapv :value new-hakukohteet)
          questions            (get-in db [:form :content])
          sorted-questions     (handlers-util/sort-questions-and-first-level-children new-hakukohteet-oids questions)
          db                   (-> db
                                 (assoc-in [:application :answers :hakukohteet :values] new-hakukohteet)
                                 (assoc-in [:application :answers :hakukohteet :value] new-hakukohteet-oids)
                                 (assoc-in [:form :content] sorted-questions))]
      {:db         db
       :dispatch-n [[:application/validate-hakukohteet]
                    [:application/change-koulutustyyppi-filter-priority current-index index-change]]})))

(reg-event-db
  :application/handle-fetch-koulutustyypit
  [check-schema-interceptor]
  (fn [db [_ {koulutustyypit-response-body :body}]]
    (let [relevant-koulutustyyyppi-ids #{"26" "2" "4" "5" "10" "40" "41"}
          koulutustyypit (filter #(relevant-koulutustyyyppi-ids (:value %))
                                 koulutustyypit-response-body)]
      (assoc-in db [:application :koulutustyypit] koulutustyypit))))

(reg-event-fx
  :application/fetch-koulutustyypit
  [check-schema-interceptor]
  (fn [_]
    {:http {:method    :get
            :url       "/hakemus/api/koulutustyypit"
            :handler   [:application/handle-fetch-koulutustyypit]}}))

(reg-event-db
  :application/toggle-koulutustyyppi-filter
  [check-schema-interceptor]
  (fn [db [_ idx koulutustyyppi-value]]
    (update-in db [:application :hakukohde-koulutustyyppi-filters idx koulutustyyppi-value] not)))

(reg-event-db
  :application/remove-koulutustyyppi-filter
  [check-schema-interceptor]
  (fn [db [_ idx]]
    (let [filters (get-in db [:application :hakukohde-koulutustyyppi-filters])
          indices-to-change (->> (keys filters)
                                 (filter #(< idx %)))
          new-indices (map dec indices-to-change)
          filters' (-> filters
                       (dissoc idx)
                       (s/rename-keys (zipmap indices-to-change new-indices)))]
      (assoc-in db [:application :hakukohde-koulutustyyppi-filters] filters'))))

(reg-event-db
  :application/change-koulutustyyppi-filter-priority
  [check-schema-interceptor]
  (fn [db [_ idx index-change]]
    (let [filters (get-in db [:application :hakukohde-koulutustyyppi-filters])
          new-idx (+ idx index-change)
          changes {idx new-idx
                   new-idx idx}
          filters' (s/rename-keys filters changes)]
      (assoc-in db [:application :hakukohde-koulutustyyppi-filters] filters'))))

(reg-event-db
  :application/show-hakukohde-toast
 [check-schema-interceptor]
 (fn[db [_ message]]
   (assoc-in db [:hakukohde-lisatty-toast] {:visible true :message message})))

(reg-event-db
 :application/hide-hakukohde-toast
 [check-schema-interceptor]
 (fn[db [_]]
   (assoc-in db [:hakukohde-lisatty-toast] {:visible false :message ""})))
