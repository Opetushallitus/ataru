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
 :application/ssn-search
 (fn [{:keys [db]} [_ potential-ssn]]
   (let [ucase-potential-ssn   (clojure.string/upper-case potential-ssn)
         type                  (cond (ssn/ssn? ucase-potential-ssn)
                                     :ssn

                                     (dob/dob? ucase-potential-ssn)
                                     :dob

                                     (email/email? potential-ssn)
                                     :email)
         show-error            false ; temporarily disabled for now, no sense in showing it if email is always default
         db-with-potential-ssn (-> db
                                   (assoc-in [:application :search-control :ssn :value] potential-ssn)
                                   (assoc-in [:application :search-control :ssn :show-error] show-error))]
     (case type
       :ssn
       {:db db-with-potential-ssn
        :dispatch [:application/fetch-applications-by-term ucase-potential-ssn :ssn]}

       :dob
       {:db       db-with-potential-ssn
        :dispatch [:application/fetch-applications-by-term ucase-potential-ssn :dob]}

       :email
       {:db       db-with-potential-ssn
        :dispatch [:application/fetch-applications-by-term potential-ssn :email]}

       {:db (assoc-in db-with-potential-ssn [:application :applications] nil)}))))
