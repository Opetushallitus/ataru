(ns ataru.virkailija.application-sorting
  (:require
   [ataru.virkailija.temporal :as t]))

(def initial-sort {:column :created-time :order :descending})

(defn- score-sort [compare-fn x y]
  (compare-fn (:score x) (:score y)))

(def application-sort-column-fns
  {:score
   {:ascending (partial score-sort <)
    :descending (partial score-sort >)}
   :applicant-name
   {:ascending (fn [x y] (compare (clojure.string/lower-case (:applicant-name x)) (clojure.string/lower-case (:applicant-name y))))
    :descending (fn [x y] (- (compare (clojure.string/lower-case (:applicant-name x)) (clojure.string/lower-case (:applicant-name y)))))}
   :created-time
   {:ascending (fn [x y]
                 (< (t/time->long (:created-time x)) (t/time->long (:created-time y))))
    :descending (fn [x y]
                  (> (t/time->long (:created-time x)) (t/time->long (:created-time y))))}})

(defn sort-by-column [applications column-id order]
  (sort (get-in application-sort-column-fns [column-id order]) applications))
