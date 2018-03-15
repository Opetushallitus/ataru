(ns ataru.virkailija.form-sorting
  (:require [cljs-time.core :as c]))

(defn sort-by-time-and-deletedness [m]
  (into (sorted-map-by
         (fn [k1 k2]
           (let [c1 (get-in m [k1 :created-time] (c/now))
                 c2 (get-in m [k2 :created-time] (c/now))]
             (cond (= k1 k2) 0
                   (nil? k1) -1
                   (nil? k2) 1
                   (c/equal? c1 c2) (compare (get-in m [k1 :id])
                                             (get-in m [k2 :id]))
                   (c/after? c1 c2) -1
                   :else 1))))
        m))
