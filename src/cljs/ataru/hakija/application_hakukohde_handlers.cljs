(ns ataru.hakija.application-hakukohde-handlers
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [subscribe reg-event-db reg-fx reg-event-fx dispatch]]
    [ataru.util :as util]
    [ataru.hakija.application-validators :as validator]
    [ataru.hakija.application-handlers :refer [set-field-visibilities]]))

(defn- hakukohteet-field [db]
  (->> (:flat-form-content db)
       (filter #(= "hakukohteet" (:id %)))
       first))

(defn- set-values-changed
  [db]
  (let [values          (map :value (get-in db [:application :answers :hakukohteet :values] []))
        original-values (get-in db [:application :answers :hakukohteet :original-value] [])]
    (update-in db [:application :values-changed?]
               (fnil (if (= original-values values) disj conj) #{})
               :hakukohteet)))

(defn- toggle-hakukohde-search
  [db]
  (update-in db [:application :show-hakukohde-search] not))

(reg-event-db
  :application/hakukohde-search-toggle
  (fn [db _] (toggle-hakukohde-search db)))

(reg-event-db
  :application/hakukohde-query-process
  (fn [db [_ hakukohde-query]]
    (if (= hakukohde-query (get-in db [:application :hakukohde-query]))
      (let [lang              (-> db :form :selected-language)
            order-by-hakuaika (fn [hk] (not @(subscribe [:application/hakukohde-editable? (:value hk)])))
            order-by-name     #(util/non-blank-val (:label %) [lang :fi :sv :en])
            hakukohde-options (->> (hakukohteet-field db)
                                   :options
                                   (sort-by (juxt order-by-hakuaika
                                                  order-by-name)))
            query-parts       (map string/lower-case (string/split hakukohde-query #"\s+"))
            results           (if (or (string/blank? hakukohde-query)
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
        (-> db
            (assoc-in [:application :remaining-hakukohde-search-results] rest-results)
            (assoc-in [:application :hakukohde-hits] hakukohde-hits)))
      db)))

(reg-event-fx
  :application/hakukohde-query-change
  (fn [_ [_ hakukohde-query timeout]]
    {:dispatch-debounced-n [{:timeout  (or timeout 500)
                             :dispatch [:application/hakukohde-query-set hakukohde-query]
                             :id       :hakukohde-query-set}
                            {:timeout  (or timeout 500)
                             :id       :hakukohde-query
                             :dispatch [:application/hakukohde-query-process hakukohde-query]}]}))

(reg-event-db
  :application/show-more-hakukohdes
  (fn [db _]
    (let [remaining-results (-> db :application :remaining-hakukohde-search-results)
          [more-hits rest-results] (split-at 15 remaining-results)]
      (-> db
          (assoc-in [:application :remaining-hakukohde-search-results] rest-results)
          (update-in [:application :hakukohde-hits] concat more-hits)))))

(reg-event-db
  :application/hakukohde-query-set
  (fn [db [_ query]]
    (assoc-in db [:application :hakukohde-query] query)))

(reg-event-fx
  :application/set-hakukohde-valid
  (fn [{:keys [db]} [_ valid?]]
    {:db         (assoc-in db [:application :answers :hakukohteet :valid] valid?)
     :dispatch-n [[:application/update-answers-validity]
                  [:application/set-validator-processed :hakukohteet]]}))

(reg-event-fx
  :application/hakukohde-add-selection
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
                                   set-values-changed
                                   set-field-visibilities)]
      {:db                 (cond-> db
                                   (and (some? max-hakukohteet)
                                        (<= max-hakukohteet (count new-hakukohde-values)))
                                   toggle-hakukohde-search)
       :validate-debounced {:value             new-hakukohde-values
                            :answers-by-key    (get-in db [:application :answers])
                            :field-descriptor  field-descriptor
                            :editing?          (get-in db [:application :editing?])
                            :virkailija?       (contains? (:application db) :virkailija-secret)
                            :before-validation #(dispatch [:application/set-validator-processing (keyword (:id field-descriptor))])
                            :on-validated      (fn [[valid? errors]]
                                                 (dispatch [:application/set-hakukohde-valid
                                                            valid?]))}})))

(defn- remove-hakukohde-from-deleting
  [hakukohteet hakukohde]
  (remove #(= hakukohde %) hakukohteet))

(reg-event-fx
  :application/hakukohde-remove
  (fn [{db :db} [_ hakukohde-oid]]
    (let [field-descriptor     (hakukohteet-field db)
          selected-hakukohteet (get-in db [:application :answers :hakukohteet :values] [])
          new-hakukohde-values (vec (remove #(= hakukohde-oid (:value %)) selected-hakukohteet))
          db                   (-> db
                                   (assoc-in [:application :answers :hakukohteet :values]
                                             new-hakukohde-values)
                                   (update-in [:application :ui :hakukohteet :deleting] remove-hakukohde-from-deleting hakukohde-oid)
                                   set-values-changed
                                   set-field-visibilities)]
      {:db                 db
       :validate-debounced {:value             new-hakukohde-values
                            :answers-by-key    (get-in db [:application :answers])
                            :field-descriptor  field-descriptor
                            :editing?          (get-in db [:application :editing?])
                            :before-validation #(dispatch [:application/set-validator-processing (keyword (:id field-descriptor))])
                            :on-validated      (fn [[valid? errors]]
                                                 (dispatch [:application/set-hakukohde-valid
                                                            valid?]))}})))

(reg-event-fx
  :application/hakukohde-remove-selection
  (fn [{db :db} [_ hakukohde-oid]]
    {:db             (update-in db [:application :ui :hakukohteet :deleting] (comp set conj) hakukohde-oid)
     :dispatch-later [{:ms       500
                       :dispatch [:application/hakukohde-remove hakukohde-oid]}]}))

(reg-event-db
  :application/change-hakukohde-priority
  (fn [db [_ hakukohde-oid index-change]]
    (let [hakukohteet     (-> db :application :answers :hakukohteet :values vec)
          current-index   (first (keep-indexed #(when (= hakukohde-oid (:value %2))
                                                  %1)
                                               hakukohteet))
          new-index       (+ current-index index-change)
          new-hakukohteet (assoc hakukohteet
                                 current-index (nth hakukohteet new-index)
                                 new-index (nth hakukohteet current-index))]
      (-> db
          (assoc-in [:application :answers :hakukohteet :values] new-hakukohteet)
          set-values-changed))))
