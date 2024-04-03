(ns ataru.virkailija.application.excel-download.excel-subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :application/excel-request-filter-value
 (fn [db [_ id]]
   (get-in db [:application :excel-request :filters id :checked])))

(re-frame/reg-sub
 :application/excel-request-filters-selected-count-by-ids
 (fn [db [_ ids]]
   (as-> (get-in db [:application :excel-request :filters]) filters
     (select-keys filters ids)
     (vals filters)
     (filter :checked filters)
     (count filters))))

(re-frame/reg-sub
 :application/excel-request-filters-initialized?
 (fn [db [_]]
   (not (empty? (get-in db [:application :excel-request :filters])))))

(re-frame/reg-sub
 :application/excel-download-mode
 (fn [db]
   (get-in db [:application :excel-request :selected-mode])))

(re-frame/reg-sub
 :application/excel-request-filters-some-selected?
 (fn [db]
   (let [filter-vals (vals (get-in db [:application :excel-request :filters]))]
     (boolean (some :checked filter-vals)))))

(re-frame/reg-sub
 :application/excel-request-filter-indeterminate?
 (fn [db [_ id]]
   (let [top-filter (get-in db [:application :excel-request :filters id])
         children-checked (->> (:child-ids top-filter)
                               (map #(get-in db [:application :excel-request :filters % :checked])))]
     (and (not (:parent-id top-filter)) (some true? children-checked) (not (:checked top-filter))))))