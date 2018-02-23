(ns ataru.hakija.application-hakukohde-handlers
  (:require
    [clojure.string :as string]
    [re-frame.core :refer [subscribe reg-event-db reg-fx reg-event-fx dispatch]]
    [ataru.util :as util]
    [ataru.hakija.application-validators :as validator]))

(defn- hakukohteet-field [db]
  (->> (:flat-form-content db)
       (filter #(= "hakukohteet" (:id %)))
       first))

(defn- selected-hakukohteet [db]
  (map :value (get-in db [:application :answers :hakukohteet :values] [])))

(defn- selected-hakukohteet-and-ryhmat [db]
  (let [selected-hakukohteet     (set (selected-hakukohteet db))
        selected-hakukohderyhmat (->> (get-in db [:form :tarjonta :hakukohteet])
                                      (filter #(contains? selected-hakukohteet (:oid %)))
                                      (mapcat :hakukohderyhmat))]
    (set (concat selected-hakukohteet selected-hakukohderyhmat))))

(defn- field-intersection-with-selected-hakukohteet-and-ryhmat [db field]
  (if-let [ids (seq (concat (get field :belongs-to-hakukohderyhma [])
                            (get field :belongs-to-hakukohteet [])))]
    (if-let [selected-hakukohteet-and-ryhmat (selected-hakukohteet-and-ryhmat db)]
      (clojure.set/intersection
        (set ids)
        selected-hakukohteet-and-ryhmat))))

(defn- set-visibility-of-belongs-to-hakukohteet-questions
  [db]
  (let [selected-hakukohteet-and-ryhmat (selected-hakukohteet-and-ryhmat db)]
    (util/reduce-form-fields
      (fn [db field]
        (if-let [intersection (field-intersection-with-selected-hakukohteet-and-ryhmat db field)]
          (assoc-in db [:application :ui (keyword (:id field)) :visible?] (not (empty? intersection)))
          db)
        )
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
      (let [lang              (-> db :form :selected-language)
            hakukohde-options (->> (hakukohteet-field db)
                                   :options
                                   (sort-by #(get-in % [:label lang])))
            query-parts       (map string/lower-case (string/split hakukohde-query #"\s+"))
            results           (if (or (string/blank? hakukohde-query)
                                      (< (count hakukohde-query) 2))
                                (map :value hakukohde-options)
                                (->> hakukohde-options
                                     (filter
                                       (fn [option]
                                         (let [haystack (string/lower-case
                                                          (str (get-in option [:label lang] "")
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
  (fn [{db :db} [_ hakukohde-query timeout]]
    {:db                 (assoc-in db [:application :hakukohde-query] hakukohde-query)
     :dispatch-debounced {:timeout  (or timeout 500)
                          :id       :hakukohde-query
                          :dispatch [:application/hakukohde-query-process hakukohde-query]}}))

(reg-event-db
  :application/show-more-hakukohdes
  (fn [db _]
    (let [remaining-results (-> db :application :remaining-hakukohde-search-results)
          [more-hits rest-results] (split-at 15 remaining-results)]
      (-> db
          (assoc-in [:application :remaining-hakukohde-search-results] rest-results)
          (update-in [:application :hakukohde-hits] concat more-hits)))))

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
