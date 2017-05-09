(ns ataru.virkailija.application.application-search-control-handlers
  (:require
   [re-frame.core :refer [reg-event-fx reg-event-db]]
   [ataru.ssn :as ssn]))

(def show-path [:application :search-control :show])

(reg-event-db
 :application/show-incomplete-haut-list
 (fn [db]
   (assoc-in db show-path :incomplete)))

(reg-event-db
 :application/show-complete-haut-list
 (fn [db]
   (assoc-in db show-path :complete)))

(reg-event-db
 :application/show-search-ssn
 (fn [db]
   (assoc-in db show-path :search-ssn)))

(reg-event-db
 :application/close-search-control
 (fn [db]
   (assoc-in db show-path nil)))

(reg-event-db
 :application/clear-ssn
 (fn [db]
   (assoc-in db [:application :search-control :ssn] nil)))

(reg-event-fx
 :application/ssn-search
 (fn [{:keys [db]} [_ potential-ssn]]
   (let [ucase-potential-ssn   (clojure.string/upper-case potential-ssn)
         previous-ssn-value    (get-in db [:application :search-control :ssn :value])
         show-error            (if (= 11 (count ucase-potential-ssn)) (not (ssn/ssn? ucase-potential-ssn)) false)
         db-with-potential-ssn (-> db
                                   (assoc-in [:application :search-control :ssn :value] potential-ssn)
                                   (assoc-in [:application :search-control :ssn :show-error] show-error))]
     (cond
       (ssn/ssn? ucase-potential-ssn)
       {:db db-with-potential-ssn
        :dispatch [:application/fetch-applications-by-ssn ucase-potential-ssn]}

       :else
       {:db (assoc-in db-with-potential-ssn [:application :applications] nil)}))))
