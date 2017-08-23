(ns ataru.hakija.application-hakukohde-handlers
  (:require
    [re-frame.core :refer [reg-event-db reg-fx reg-event-fx dispatch]]
    [ataru.hakija.application-validators :as validator]))

(defn- hakukohteet-field [db]
  (->> (get-in db [:form :content] [])
       (filter #(= "hakukohteet" (:id %)))
       first))

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
  :application/hakukohde-add-selection
  (fn [db [_ hakukohde-oid]]
    (let [selected-hakukohteet (get-in db [:application :answers :hakukohteet :values] [])
          new-hakukohde-values (conj selected-hakukohteet {:valid true :value hakukohde-oid})]
      (-> db
          (assoc-in [:application :answers :hakukohteet :values]
                    new-hakukohde-values)
          (assoc-in [:application :answers :hakukohteet :valid]
                    (validator/validate :hakukohteet new-hakukohde-values nil (hakukohteet-field db)))))))

(reg-event-db
  :application/hakukohde-remove-selection
  (fn [db [_ hakukohde-oid]]
    (let [selected-hakukohteet (get-in db [:application :answers :hakukohteet :values] [])
          new-hakukohde-values (remove #(= hakukohde-oid (:value %)) selected-hakukohteet)]
      (-> db
          (assoc-in [:application :answers :hakukohteet :values]
                    new-hakukohde-values)
          (assoc-in [:application :answers :hakukohteet :valid]
                    (validator/validate :hakukohteet new-hakukohde-values nil (hakukohteet-field db)))))))

