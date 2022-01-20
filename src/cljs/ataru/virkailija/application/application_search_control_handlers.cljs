(ns ataru.virkailija.application.application-search-control-handlers
  (:require
   [re-frame.core :refer [reg-event-fx reg-event-db]]
   [ataru.dob :as dob]
   [clojure.string :as str]
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

(defn- person-oid?
  [maybe-oid]
  (re-matches #"^1\.2\.246\.562\.24\.\d+$" maybe-oid))

(defn- application-oid?
  [maybe-oid]
  (re-matches #"^1\.2\.246\.562\.11\.\d+$" maybe-oid))

(defn- application-oid-suffix?
  [maybe-oid-suffix]
  (re-matches #"^\d{2,20}$" maybe-oid-suffix))

(defn- complete-application-oid
  [oid-suffix]
  (str/join "" (concat "1.2.246.562.11."
                       (repeat (- 20 (count oid-suffix)) "0")
                       oid-suffix)))

(defn- parse-search-term
  [search-term]
  (let [search-term-ucase (-> search-term
                              str/trim
                              str/upper-case)]
    (cond (application-oid? search-term-ucase)
          {:application-oid search-term-ucase}

          (person-oid? search-term-ucase)
          {:person-oid search-term-ucase}

          (ssn/ssn? search-term-ucase)
          {:ssn search-term-ucase}

          (dob/dob? search-term-ucase)
          {:dob search-term-ucase}

          (email/email? search-term)
          {:email search-term}

          (application-oid-suffix? search-term)
          {:application-oid (complete-application-oid search-term-ucase)}

          (< 2 (count search-term))
          {:name search-term})))

(defn set-search
  [db search-term]
  (-> db
      (assoc-in [:application :search-control :search-term :value] search-term)
      (assoc-in [:application :search-control :search-term :parsed] (parse-search-term search-term))
      (assoc-in [:application :search-control :search-term :show-error] false)))

(reg-event-fx
  :application/search-by-term
  (fn [{:keys [db]} [_ search-term]]
    (cljs-util/set-query-param "term" search-term)
    (let [db (set-search db search-term)]
      (cond-> {:db db}
              (or (some? (get-in db [:application :search-control :search-term :parsed]))
                  (str/blank? search-term))
              (assoc :dispatch-debounced {:timeout  (if (str/blank? search-term) 0 500)
                                          :id       :application-search
                                          :dispatch [:application/reload-applications true]})))))
