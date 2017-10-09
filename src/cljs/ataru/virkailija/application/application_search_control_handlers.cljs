(ns ataru.virkailija.application.application-search-control-handlers
  (:require
   [re-frame.core :refer [reg-event-fx reg-event-db]]
   [ataru.dob :as dob]
   [ataru.email :as email]
   [ataru.ssn :as ssn]
   [ataru.cljs-util :as cljs-util]))

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

(defn- set-search-term
  [db term]
  (cljs-util/set-query-param "term" term)
  (-> db
      (assoc-in [:application :search-control :search-term :value] term)
      (assoc-in [:application :search-control :search-term :show-error] false)))

(reg-event-fx
  :application/search-by-term
  (fn [{:keys [db]} [_ search-term]]
    (let [search-term-ucase   (-> search-term
                                  clojure.string/trim
                                  clojure.string/upper-case)
          term-type           (cond (ssn/ssn? search-term-ucase)
                                    [search-term-ucase :ssn]

                                    (dob/dob? search-term-ucase)
                                    [search-term-ucase :dob]

                                    (email/email? search-term)
                                    [search-term :email]

                                    (< 2 (count search-term))
                                    [search-term :name])]
      (if-let [[term type] term-type]
        {:db (set-search-term db search-term)
         :dispatch-debounced {:timeout 500
                              :id :application-search
                              :dispatch [:application/fetch-applications-by-term term type]}}
        {:db (-> db
                 (set-search-term search-term)
                 (assoc-in [:application :applications] nil))}))))
