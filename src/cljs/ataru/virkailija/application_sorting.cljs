(ns ataru.virkailija.application-sorting
  (:require
   [ataru.virkailija.temporal :as t]))

(def initial-sort {:column :created-time :order :descending})

(defn- compare-by-state
  [x y]
  (if (not= (:state x) (:state y))
    (if (= "inactivated" (:state x)) 1 -1)
    false))

(defn- date-sort [compare-fn sort-key x y]
  (or
    (compare-by-state x y)
    (compare-fn (t/time->long (sort-key x))
                (t/time->long (sort-key y)))))

(defn- applicant-sort [order-fn x y]
  (or
    (compare-by-state x y)
    (order-fn
      (compare
        (clojure.string/lower-case (-> x :person :last-name))
        (clojure.string/lower-case (-> y :person :last-name))))))

(def application-sort-column-fns
  {:applicant-name
   {:ascending (partial applicant-sort +)
    :descending (partial applicant-sort -)}
   :created-time
   {:ascending (partial date-sort < :created-time)
    :descending (partial date-sort > :created-time)}
   :original-created-time
   {:ascending (partial date-sort < :original-created-time)
    :descending (partial date-sort > :original-created-time)}})

(defn sort-by-column [applications column-id order]
  (sort (get-in application-sort-column-fns [column-id order]) applications))
