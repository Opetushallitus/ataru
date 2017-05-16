(ns ataru.virkailija.application.application-search-control-handlers
  (:require
   [re-frame.core :refer [reg-event-fx reg-event-db]]
   [ataru.dob :as dob]
   [ataru.email :as email]
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

(reg-event-fx
 :application/search-by-term
 (fn [{:keys [db]} [_ search-term]]
   (let [search-term-ucase     (clojure.string/upper-case search-term)
         type                  (cond (ssn/ssn? search-term-ucase)
                                     :ssn

                                     (dob/dob? search-term-ucase)
                                     :dob

                                     (email/email? search-term)
                                     :email)
         show-error            false ; temporarily disabled for now, no sense in showing it if email is always default
         db-with-potential-ssn (-> db
                                   (assoc-in [:application :search-control :ssn :value] search-term)
                                   (assoc-in [:application :search-control :ssn :show-error] show-error))]
     (case type
       :ssn
       {:db db-with-potential-ssn
        :dispatch [:application/fetch-applications-by-term search-term-ucase :ssn]}

       :dob
       {:db       db-with-potential-ssn
        :dispatch [:application/fetch-applications-by-term search-term-ucase :dob]}

       :email
       {:db       db-with-potential-ssn
        :dispatch [:application/fetch-applications-by-term search-term :email]}

       {:db (assoc-in db-with-potential-ssn [:application :applications] nil)}))))
