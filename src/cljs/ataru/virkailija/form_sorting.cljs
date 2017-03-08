(ns ataru.virkailija.form-sorting
  (:require [cljs-time.core :as c]))

(defn sort-by-time-and-deletedness [m]
  (into (sorted-map-by
          (fn [k1 k2]
            (let [v1 (get m k1)
                  v2 (get m k2)
                  v1-deleted? (:deleted v1)
                  v2-deleted? (:deleted v2)
                  v1-created (:created-time v1)
                  v2-created (:created-time v2)]
              (cond
                (and v1-deleted? (not v2-deleted?)) 1
                (and v2-deleted? (not v1-deleted?)) -1
                (and (nil? v1-created) (nil? v2-created)) 0
                (nil? v2-created) 1
                (nil? v1-created) -1
                :else (c/after? v1-created v2-created)))))
        m))
