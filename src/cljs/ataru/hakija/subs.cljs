(ns ataru.hakija.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]
            [ataru.hakija.application :refer [answers->valid-status
                                              wrapper-sections-with-validity
                                              applying-possible?]]))

(re-frame/reg-sub
  :state-query
  (fn [db [_ path]]
    (get-in db path)))

(re-frame/reg-sub
  :application/valid-status
  (fn [db]
    (answers->valid-status (-> db :application :answers) (-> db :application :ui))))

(re-frame/reg-sub
  :application/wrapper-sections
  (fn [db]
    (wrapper-sections-with-validity
      (:wrapper-sections db)
      (-> db :application :answers))))

(re-frame/reg-sub
 :application/can-apply?
 (fn [db]
   (applying-possible? (:form db) (:application db))))

(re-frame/reg-sub
  :application/hakukohde-count
  (fn [db]
    (count (-> db :tarjonta :hakukohteet))))

(re-frame/reg-sub
  :application/form-language
  (fn [db]
    (or
      (get-in db [:form :selected-language])
      :fi))) ; When user lands on the page, there isn't any language set until the form is loaded)

(re-frame/reg-sub
  :application/default-language
  (fn [db]
    (-> db
        (get-in [:form :languages])
        first)))

(re-frame/reg-sub
  :application/adjacent-field-row-amount
  (fn [db [_ field-descriptor]]
    (let [child-id   (-> (:children field-descriptor) first :id keyword)
          row-amount (-> (get-in db [:application :answers child-id :values] [])
                         count)]
      (if (= row-amount 0)
        1
        row-amount))))

(re-frame/reg-sub
  :application/multiple-choice-option-checked?
  (fn [db [_ parent-id option-value]]
    (let [options (get-in db [:application :answers parent-id :options])]
      (true? (get options option-value)))))

(re-frame/reg-sub
  :application/single-choice-option-checked?
  (fn [db [_ parent-id option-value]]
    (let [value (get-in db [:application :answers parent-id :value])]
      (= option-value value))))

(defn- hakukohde-query [db]
  (get-in db [:application :hakukohde-query]))

(defn- selected-hakukohde-oids [db]
  (map :value (get-in db [:application :answers :hakukohteet :values] [])))

(defn- hakukohteet-field [db]
  (->> (get-in db [:form :content] [])
       (filter #(= "hakukohteet" (:id %)))
       first))

(defn- max-hakukohteet [db]
  (get-in (hakukohteet-field db)
          [:params :max-hakukohteet]
          nil))

(re-frame/reg-sub
  :application/selected-hakukohteet
  (fn [db _]
    (selected-hakukohde-oids db)))

(re-frame/reg-sub
  :application/hakukohteet-editable?
  (fn [db _] (< 1 (count (:options (hakukohteet-field db))))))

(re-frame/reg-sub
  :application/hakukohde-query
  (fn [db _] (hakukohde-query db)))

(re-frame/reg-sub
  :application/hakukohde-hits
  (fn [db _]
    (let [hakukohde-query (hakukohde-query db)
          query-pattern (re-pattern (str "(?i)" hakukohde-query))
          hakukohde-options (:options (hakukohteet-field db))]
      (if (< 1 (count hakukohde-query))
                                        ; TODO support other languages
        (->> hakukohde-options
             (filter #(re-find query-pattern (get-in % [:label :fi] "")))
             (map :value))
        []))))

(re-frame/reg-sub
  :application/hakukohde-selected?
  (fn [db [_ hakukohde-oid]]
    (some #(= % hakukohde-oid) (selected-hakukohde-oids db))))

(re-frame/reg-sub
  :application/max-hakukohteet
  (fn [db _] (max-hakukohteet db)))

(re-frame/reg-sub
  :application/hakukohteet-full?
  (fn [db _] (<= (max-hakukohteet db) (count (selected-hakukohde-oids db)))))

(re-frame/reg-sub
  :application/hakukohde-label
  (fn [db [_ hakukohde-oid]]
    (let [lang :fi ;; FIXME
          hakukohteet-by-oid (into {} (map (juxt :value identity)
                                           (:options (hakukohteet-field db))))]
      (get-in hakukohteet-by-oid [hakukohde-oid :label lang]))))

(re-frame/reg-sub
  :application/hakukohde-description
  (fn [db [_ hakukohde-oid]]
    (let [lang :fi ;; FIXME
          hakukohteet-by-oid (into {} (map (juxt :value identity)
                                           (:options (hakukohteet-field db))))]
      (get-in hakukohteet-by-oid [hakukohde-oid :description lang]))))
