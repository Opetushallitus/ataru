(ns ataru.virkailija.application.excel-download.excel-subs
  (:require [ataru.virkailija.application.excel-download.excel-utils :refer [get-in-excel]]
            [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :application/excel-request-filter-value
 (fn [db [_ id]]
   (get-in-excel db [:filters id :checked])))

(re-frame/reg-sub
 :application/excel-request-filters-selected-count-by-ids
 (fn [db [_ ids]]
   (as-> (get-in-excel db :filters) filters
     (select-keys filters ids)
     (vals filters)
     (filter :checked filters)
     (count filters))))


(re-frame/reg-sub
 :application/excel-download-mode
 (fn [db]
   (get-in-excel db :selected-mode)))

(re-frame/reg-sub
 :application/excel-request-filters-some-selected?
 (fn [db]
   (let [filter-vals (vals (get-in-excel db :filters))]
     (boolean (some :checked filter-vals)))))

(re-frame/reg-sub
 :application/excel-request-filter-indeterminate?
 (fn [db [_ id]]
   (let [top-filter (get-in-excel db [:filters id])
         children-checked (->> (:child-ids top-filter)
                               (map #(get-in-excel db [:filters % :checked])))]
     (and (not (:parent-id top-filter)) (some true? children-checked) (not (:checked top-filter))))))

(re-frame/reg-sub
 :application/excel-request-accordion-open?
 (fn [db [_ id]]
   (get-in-excel db [:filters id :open?])))

(re-frame/reg-sub
 :application/excel-request-filters
 (fn [db [_]]
   (get-in-excel db :filters)))

(re-frame/reg-sub
 :application/excel-request-filters-initialized?
 (fn [db [_]]
   (not (empty? (get-in-excel db :filters)))))

(re-frame/reg-sub
 :application/excel-request-filters-initializing?
 (fn [_ _]
   [(re-frame/subscribe [:application/excel-request-filters-initialized?])
    (re-frame/subscribe [:application/fetching-form-content?])
    (re-frame/subscribe [:state-query [:fetching-hakukohteet]])])
 (fn [[filters-initialized? fetching-form-content? fetching-hakukohteet]]
   (and (not filters-initialized?)
        (or fetching-form-content? (> fetching-hakukohteet 0)))))

(re-frame/reg-sub
 :application/excel-request-filters-need-initialization?
 (fn [_ _]
   [(re-frame/subscribe [:application/excel-request-filters-initializing?])
    (re-frame/subscribe [:application/excel-request-filters-initialized?])])
 (fn [[filters-initializing? filters-initialized?]]
   (and (not filters-initialized?) (not filters-initializing?))))