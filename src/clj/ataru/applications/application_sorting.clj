(ns ataru.applications.application-sorting)

(defn- date-sort [compare-fn sort-key x y]
  (compare-fn (.getMillis (sort-key x)) (.getMillis (sort-key y))))

(defn- applicant-sort [order-fn x y]
  (order-fn
    (compare
      (clojure.string/lower-case (str (-> x :person :last-name) (-> x :person :first-name)))
      (clojure.string/lower-case (str (-> y :person :last-name) (-> y :person :first-name))))))

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

(defn person-info-needed-to-sort?
  [column-id]
  (= (keyword column-id) :applicant-name))
