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
 :application/show-search-term
 (fn [db]
   (assoc-in db show-path :search-term)))

(reg-event-db
 :application/close-search-control
 (fn [db]
   (assoc-in db show-path nil)))

(reg-event-fx
 :application/search-by-term
 (fn [{:keys [db]} [_ search-term]]
   (let [search-term-ucase   (clojure.string/upper-case search-term)
         term-type           (cond (ssn/ssn? search-term-ucase)
                                   [search-term-ucase :ssn]

                                   (dob/dob? search-term-ucase)
                                   [search-term-ucase :dob]

                                   (email/email? search-term nil)
                                   [search-term :email])
         show-error          false ; temporarily disabled for now, no sense in showing it if email is always default
         db-with-search-term (-> db
                                 (assoc-in [:application :search-control :search-term :value] search-term)
                                 (assoc-in [:application :search-control :search-term :show-error] show-error))]
     (if-let [[term type] term-type]
       {:db db-with-search-term
        :dispatch [:application/fetch-applications-by-term term type]}
       {:db (assoc-in db-with-search-term [:application :applications] nil)}))))
