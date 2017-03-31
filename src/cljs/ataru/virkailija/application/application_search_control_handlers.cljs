(ns ataru.virkailija.application.application-search-control-handlers
  (:require
    [re-frame.core :refer [reg-event-fx reg-event-db]]))

(def show-path [:application :search-control :show])

(defn toggle-show [db list-kw]
  (if (= (get-in db show-path) list-kw)
    (assoc-in db show-path nil)
    (assoc-in db show-path list-kw)))

(reg-event-db
 :application/show-incomplete-haut-list
 (fn [db [_ _]]
   (toggle-show db :incomplete)))

(reg-event-db
 :application/show-complete-haut-list
 (fn [db [_ _]]
   (toggle-show db :complete)))

(reg-event-db
 :application/close-search-control
 (fn [db [_ _]]
   (assoc-in db show-path nil)))
