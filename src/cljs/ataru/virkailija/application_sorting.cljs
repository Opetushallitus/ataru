(ns ataru.virkailija.application-sorting
  (:require
   [ataru.virkailija.temporal :as t]))

(def initial-sort {:column :created-time :order :descending})

(defn- score-sort [compare-fn x y]
  (compare-fn (:score x) (:score y)))

(defn- date-sort [compare-fn x y]
  (compare-fn (t/time->long (:created-time x))
              (t/time->long (:created-time y))))

(defn- applicant-sort [order-fn x y]
  (order-fn
   (compare
    (clojure.string/lower-case (:applicant-name x))
    (clojure.string/lower-case (:applicant-name y)))))

(def application-sort-column-fns
  {:score
   {:ascending (partial score-sort <)
    :descending (partial score-sort >)}
   :applicant-name
   {:ascending (partial applicant-sort +)
    :descending (partial applicant-sort -)}
   :created-time
   {:ascending (partial date-sort <)
    :descending (partial date-sort >)}})

(defn sort-by-column [applications column-id order]
  (sort (get-in application-sort-column-fns [column-id order]) applications))
