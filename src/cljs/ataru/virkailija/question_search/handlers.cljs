(ns ataru.virkailija.question-search.handlers
  (:require [ataru.util :as util]
            [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
  :question-search/set-search-input
  (fn [{db :db} [_ form-key id filter-predicate value]]
    {:db                 (assoc-in db [:question-search form-key id :search-input] value)
     :dispatch-debounced {:timeout  500
                          :id       [:question-search form-key id]
                          :dispatch [:question-search/search form-key id filter-predicate]}}))

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
  (clojure.string/includes?
   (clojure.string/lower-case
    (or (util/non-blank-val (:label field) [lang :fi :sv :en])
        ""))
   search-term))

(defn- matches-hakukohde-or-tarjoaja-name?
  [db field lang search-term]
  (boolean
   (some (fn [hakukohde-oid]
           (let [hakukohde (get-in db [:hakukohteet hakukohde-oid])]
             (or (clojure.string/includes?
                  (clojure.string/lower-case
                   (or (util/non-blank-val (:name hakukohde) [lang :fi :sv :en])
                       ""))
                  search-term)
                 (clojure.string/includes?
                  (clojure.string/lower-case
                   (or (util/non-blank-val (:tarjoaja-name hakukohde) [lang :fi :sv :en])
                       ""))
                  search-term))))
         (:belongs-to-hakukohteet field))))

(defn- matches-hakukohderyhma-name?
  [db field lang search-term]
  (boolean
   (some (fn [hakukohderyhma-oid]
           (clojure.string/includes?
            (clojure.string/lower-case
             (or (util/non-blank-val
                  (get-in db [:hakukohderyhmat hakukohderyhma-oid :name])
                  [lang :fi :sv :en])
                 ""))
            search-term))
         (:belongs-to-hakukohderyhma field))))

(defn- belongs-to-hakukohde?
  [hakukohteet field]
  (some hakukohteet (:belongs-to-hakukohteet field)))

(defn- belongs-to-hakukohderyhma?
  [hakukohderyhmat field]
  (some hakukohderyhmat (:belongs-to-hakukohderyhma field)))

(defn- belongs-to-virkailijan-hakukohde?
  [db]
  (fn [field]
    (some #(get-in db [:hakukohteet % :user-organization?] false)
          (:belongs-to-hakukohteet field))))

(defn- belongs-to-virkailijan-hakukohderyhma?
  [db]
  (let [hakukohderyhmat (->> (:hakukohteet db)
                             (filter :user-organization?)
                             (mapcat :ryhmaliitokset)
                             set)]
    (fn [field]
      (some hakukohderyhmat (:belongs-to-hakukohderyhma field)))))

(defn field-type-filter-predicate
  [field-type]
  (fn [field]
    (= field-type (:fieldType field))))

(defn belongs-to-selected-filter-predicate
  [db]
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
        hakukohderyhma-oids     (set (mapcat :ryhmaliitokset hakukohteet))]
    (fn [field]
      (or (nil? hakukohteet)
          (and (empty? (:belongs-to-hakukohteet field))
               (empty? (:belongs-to-hakukohderyhma field)))
          (belongs-to-hakukohde? hakukohde-oids field)
          (belongs-to-hakukohderyhma? hakukohderyhma-oids field)))))

(re-frame/reg-event-db
  :question-search/search
  (fn [db [_ form-key id filter-predicate]]
    (let [lang                                   (or (-> db :editor :user-info :lang keyword) :fi)
          search-input                           (get-in db [:question-search form-key id :search-input] "")
          search-terms                           (clojure.string/split (clojure.string/lower-case search-input) #"\s+")
          filter-predicate                       (filter-predicate db)
          belongs-to-virkailijan-hakukohde?      (belongs-to-virkailijan-hakukohde? db)
          belongs-to-virkailijan-hakukohderyhma? (belongs-to-virkailijan-hakukohderyhma? db)]
      (assoc-in db [:question-search form-key id :search-result]
                (->> (get-in db [:forms form-key :flat-content])
                     (filter (fn [field]
                               (and (filter-predicate field)
                                    (or (matches-id? field search-input)
                                        (every? #(or (matches-label? field lang %)
                                                     (matches-hakukohde-or-tarjoaja-name? db field lang %)
                                                     (matches-hakukohderyhma-name? db field lang %))
                                                search-terms)))))
                     (sort (comparator #(or (and (belongs-to-virkailijan-hakukohde? %1)
                                                 (not (belongs-to-virkailijan-hakukohde? %2)))
                                            (and (belongs-to-virkailijan-hakukohderyhma? %1)
                                                 (not (belongs-to-virkailijan-hakukohde? %2))
                                                 (not (belongs-to-virkailijan-hakukohderyhma? %2))))))
                     (mapv (comp keyword :id)))))))
