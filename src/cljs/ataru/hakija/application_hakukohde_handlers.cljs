(ns ataru.hakija.application-hakukohde-handlers
  (:require
    [re-frame.core :refer [reg-event-db reg-fx reg-event-fx dispatch]]
    [ataru.util :as util]
    [ataru.hakija.application-validators :as validator]))

(defn- hakukohteet-field [db]
  (->> (get-in db [:form :content] [])
       util/flatten-form-fields
       (filter #(= "hakukohteet" (:id %)))
       first))

(defn- set-visibility-of-belongs-to-hakukohteet-questions
  [db]
  (let [selected-hakukohteet (set (map :value (get-in db [:application :answers :hakukohteet :values] [])))]
    (util/reduce-form-fields
     (fn [db field]
       (if (empty? (:belongs-to-hakukohteet field))
         db
         (assoc-in db [:application :ui (keyword (:id field)) :visible?]
                   (not (empty? (clojure.set/intersection
                                 (set (:belongs-to-hakukohteet field))
                                 selected-hakukohteet))))))
     db
     (get-in db [:form :content]))))

(defn- set-values-changed
  [db]
  (let [values (map :value (get-in db [:application :answers :hakukohteet :values] []))
        original-values (get-in db [:application :answers :hakukohteet :original-value] [])]
    (update-in db [:application :values-changed?]
               (fnil (if (= original-values values) disj conj) #{})
               :hakukohteet)))

(reg-event-db
  :application/hakukohde-search-toggle
  (fn [db _]
    (update-in db [:application :show-hakukohde-search] not)))

(reg-event-db
  :application/hakukohde-query-process
  (fn [db [_ hakukohde-query]]
    (if (= hakukohde-query (get-in db [:application :hakukohde-query]))
      (let [hakukohde-options (:options (hakukohteet-field db))
            lang              (-> db :form :selected-language)
            query-pattern     (re-pattern (str "(?i)" hakukohde-query))
            results           (if (clojure.string/blank? hakukohde-query)
                                (map :value hakukohde-options)
                                (->> hakukohde-options
                                     (filter #(re-find query-pattern
                                                       (str
                                                         (get-in % [:label lang] "")
                                                         (get-in % [:description lang] ""))))
                                     (map :value)))]
        (assoc-in db [:application :hakukohde-hits] results))
      db)))

(reg-event-fx
  :application/hakukohde-query-change
  (fn [{db :db} [_ hakukohde-query timeout]]
    {:db                 (assoc-in db [:application :hakukohde-query] hakukohde-query)
     :dispatch-debounced {:timeout  (or timeout 500)
                          :id       :hakukohde-query
                          :dispatch [:application/hakukohde-query-process hakukohde-query]}}))

(reg-event-db
  :application/set-hakukohde-valid
  (fn [db [_ valid?]]
    (assoc-in db [:application :answers :hakukohteet :valid] valid?)))

(reg-event-fx
  :application/hakukohde-add-selection
  (fn [{db :db} [_ hakukohde-oid]]
    (let [selected-hakukohteet (get-in db [:application :answers :hakukohteet :values] [])
          not-yet-selected? (every? #(not= hakukohde-oid (:value %))
                                    selected-hakukohteet)
          new-hakukohde-values (cond-> selected-hakukohteet
                                 not-yet-selected?
                                 (conj {:valid true :value hakukohde-oid}))
          db (-> db
                 (assoc-in [:application :answers :hakukohteet :values]
                           new-hakukohde-values)
                 set-values-changed
                 set-visibility-of-belongs-to-hakukohteet-questions)]
      {:db db
       :validate {:value new-hakukohde-values
                  :answers (get-in db [:application :answers])
                  :field-descriptor (hakukohteet-field db)
                  :editing? (get-in db [:application :editing?])
                  :on-validated (fn [[valid? errors]]
                                  (dispatch [:application/set-hakukohde-valid
                                             valid?]))}})))

(defn- remove-hakukohde-from-deleting
  [hakukohteet hakukohde]
  (remove #(= hakukohde %) hakukohteet))

(reg-event-fx
 :application/hakukohde-remove
 (fn [{db :db} [_ hakukohde-oid]]
   (let [selected-hakukohteet (get-in db [:application :answers :hakukohteet :values] [])
         new-hakukohde-values (vec (remove #(= hakukohde-oid (:value %)) selected-hakukohteet))
         db (-> db
                (assoc-in [:application :answers :hakukohteet :values]
                          new-hakukohde-values)
                (update-in [:application :ui :hakukohteet :deleting] remove-hakukohde-from-deleting hakukohde-oid)
                set-values-changed
                set-visibility-of-belongs-to-hakukohteet-questions)]
     {:db db
      :validate {:value new-hakukohde-values
                 :answers (get-in db [:application :answers])
                 :field-descriptor (hakukohteet-field db)
                 :editing? (get-in db [:application :editing?])
                 :on-validated (fn [[valid? errors]]
                                 (dispatch [:application/set-hakukohde-valid
                                            valid?]))}})))

(reg-event-fx
  :application/hakukohde-remove-selection
  (fn [{db :db} [_ hakukohde-oid]]
    {:db             (update-in db [:application :ui :hakukohteet :deleting] (comp set conj) hakukohde-oid)
     :dispatch-later [{:ms       500
                       :dispatch [:application/hakukohde-remove hakukohde-oid]}]}))

(reg-event-db
  :application/show-answers-belonging-to-hakukohteet
  (fn [db _]
    (set-visibility-of-belongs-to-hakukohteet-questions db)))

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
