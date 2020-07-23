(ns ataru.virkailija.question-search.handlers
  (:require [ataru.util :as util]
            [clojure.string :as clj-string]
            [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
  :question-search/set-search-input
  (fn [{db :db} [_ form-key id filter-predicate value]]
    {:db                 (assoc-in db [:question-search form-key id :search-input] value)
     :dispatch-debounced {:timeout  500
                          :id       [:question-search form-key id]
                          :dispatch [:question-search/start-search form-key id filter-predicate]}}))

(re-frame/reg-event-db
  :question-search/clear-search-input
  (fn [db [_ form-key id]]
    (-> db
        (assoc-in [:question-search form-key id :search-input] "")
        (update-in [:question-search form-key id] dissoc :search-result))))

(defn- matches-id?
  [field search-input]
  (= search-input (:id field)))

(defn- matches-label?
  [field lang search-term]
  (clj-string/includes?
   (clj-string/lower-case
    (or (util/non-blank-val (:label field) [lang :fi :sv :en])
        ""))
   search-term))

(defn- matches-hakukohde-or-tarjoaja-name?
  [db field lang search-term]
  (boolean
   (some (fn [hakukohde-oid]
           (let [hakukohde (get-in db [:hakukohteet hakukohde-oid])]
             (or (clj-string/includes?
                  (clj-string/lower-case
                   (or (util/non-blank-val (:name hakukohde) [lang :fi :sv :en])
                       ""))
                  search-term)
                 (clj-string/includes?
                  (clj-string/lower-case
                   (or (util/non-blank-val (:tarjoaja-name hakukohde) [lang :fi :sv :en])
                       ""))
                  search-term))))
         (:belongs-to-hakukohteet field))))

(defn- matches-hakukohderyhma-name?
  [db field lang search-term]
  (boolean
   (some (fn [hakukohderyhma-oid]
           (clj-string/includes?
            (clj-string/lower-case
             (or (util/non-blank-val
                  (get-in db [:hakukohderyhmat hakukohderyhma-oid :name])
                  [lang :fi :sv :en])
                 ""))
            search-term))
         (:belongs-to-hakukohderyhma field))))

(defn- belongs-to-hakukohde-or-hakukohderyhma?
  [hakukohteet hakukohderyhmat fields-by-id default field]
  (cond (and (or (seq (:belongs-to-hakukohteet field))
                 (seq (:belongs-to-hakukohderyhma field)))
             (not (some hakukohteet (:belongs-to-hakukohteet field)))
             (not (some hakukohderyhmat (:belongs-to-hakukohderyhma field))))
        false
        (or (:children-of field)
            (:followup-of field))
        (belongs-to-hakukohde-or-hakukohderyhma?
         hakukohteet
         hakukohderyhmat
         fields-by-id
         default
         (get fields-by-id (keyword (or (:children-of field)
                                        (:followup-of field)))))
        :else
        default))

(defn- belongs-to-virkailijan-hakukohde-or-hakukohderyhma?
  [db form-key]
  (let [fields-by-id        (get-in db [:forms form-key :form-fields-by-id])
        hakukohteet         (filter :user-organization? (:hakukohteet db))
        hakukohde-oids      (set (map :oid hakukohteet))
        hakukohderyhma-oids (set (mapcat :ryhmaliitokset hakukohteet))]
    (fn [field]
      (belongs-to-hakukohde-or-hakukohderyhma?
       hakukohde-oids
       hakukohderyhma-oids
       fields-by-id
       false
       field))))

(defn field-type-filter-predicate
  [field-type]
  (fn [field]
    (= field-type (:fieldType field))))

(defn belongs-to-selected-filter-predicate
  [db form-key]
  (let [selected-hakukohde      (get-in db [:application :selected-hakukohde])
        selected-hakukohderyhma (get-in db [:application :selected-hakukohderyhma])
        hakukohteet             (cond (some? selected-hakukohde)
                                      [(get-in db [:hakukohteet selected-hakukohde])]
                                      (some? selected-hakukohderyhma)
                                      (->> (get-in db [:haut (first selected-hakukohderyhma) :hakukohteet])
                                           (map #(get-in db [:hakukohteet %]))
                                           (filter #(some (partial = (second selected-hakukohderyhma)) (:ryhmaliitokset %))))
                                      :else
                                      nil)
        hakukohde-oids          (set (map :oid hakukohteet))
        hakukohderyhma-oids     (set (mapcat :ryhmaliitokset hakukohteet))
        fields-by-id            (get-in db [:forms form-key :form-fields-by-id])]
    (fn [field]
      (or (nil? hakukohteet)
          (belongs-to-hakukohde-or-hakukohderyhma?
           hakukohde-oids
           hakukohderyhma-oids
           fields-by-id
           true
           field)))))

(re-frame/reg-event-fx
  :question-search/start-search
  (fn [{db :db} [_ form-key id filter-predicate]]
    (let [lang             (or (-> db :editor :user-info :lang keyword) :fi)
          search-input     (get-in db [:question-search form-key id :search-input] "")
          search-terms     (-> search-input
                               clj-string/trim
                               clj-string/lower-case
                               (clj-string/split #"\s+"))
          filter-predicate (filter-predicate db form-key)
          belongs-to?      (belongs-to-virkailijan-hakukohde-or-hakukohderyhma? db form-key)
          searching?       (and (seq search-terms)
                                (not (clj-string/blank? (first search-terms))))]
      {:db       (-> db
                     (assoc-in [:question-search form-key id :search-result]
                               cljs.core/PersistentQueue.EMPTY)
                     (assoc-in [:question-search form-key id :searching?]
                               searching?)
                     (assoc-in [:question-search form-key id :filter-predicate]
                               (fn [field]
                                 (and (filter-predicate field)
                                      (or (matches-id? field search-input)
                                          (every? #(or (matches-label? field lang %)
                                                       (matches-hakukohde-or-tarjoaja-name? db field lang %)
                                                       (matches-hakukohderyhma-name? db field lang %))
                                                  search-terms)))))
                     (assoc-in [:question-search form-key id :less-than-predicate]
                               #(and (belongs-to? %1)
                                     (not (belongs-to? %2))))
                     (assoc-in [:question-search form-key id :fields-to-search]
                               (when searching?
                                 (get-in db [:forms form-key :flat-form-fields]))))
       :dispatch [:question-search/search form-key id]})))

(defn- add-to-result
  [result less-than-predicate f]
  (loop [less-than cljs.core/PersistentQueue.EMPTY
         result    result]
    (if (or (empty? result)
            (less-than-predicate f (peek result)))
      (into (conj less-than f) result)
      (recur (conj less-than (peek result))
             (pop result)))))

(re-frame/reg-event-fx
  :question-search/search
  (fn search [{db :db} [_ form-key id]]
    (let [filter-predicate    (get-in db [:question-search form-key id :filter-predicate])
          less-than-predicate (get-in db [:question-search form-key id :less-than-predicate])
          search-result       (get-in db [:question-search form-key id :search-result])]
      (if-let [fields (seq (get-in db [:question-search form-key id :fields-to-search]))]
        {:db       (-> db
                       (assoc-in [:question-search form-key id :fields-to-search]
                                 (rest fields))
                       (assoc-in [:question-search form-key id :search-result]
                                 (if (filter-predicate (first fields))
                                   (add-to-result search-result less-than-predicate (first fields))
                                   search-result)))
         :dispatch [:question-search/search form-key id]}
        {:db (assoc-in db [:question-search form-key id :searching?] false)}))))
