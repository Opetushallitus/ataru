(ns ataru.virkailija.application.application-search-control-handlers
  (:require
    [re-frame.core :refer [reg-event-fx reg-event-db]]))

(def show-path [:application :search-control :show])

(reg-event-db
 :application/show-incomplete-haut-list
 (fn [db [_ _]]
   (assoc-in db show-path :incomplete)))

(reg-event-db
 :application/show-complete-haut-list
 (fn [db [_ _]]
   (assoc-in db show-path :complete)))

(reg-event-db
 :application/show-search-ssn
 (fn [db [_ _]]
   (assoc-in db show-path :search-ssn)))

(reg-event-db
 :application/close-search-control
 (fn [db [_ _]]
   (assoc-in db show-path nil)))
