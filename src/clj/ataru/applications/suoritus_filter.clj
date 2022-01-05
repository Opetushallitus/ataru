(ns ataru.applications.suoritus-filter
  (:require [clj-time.core :as time]))

(defn year-for-suoritus-filter
  [now]
  (time/year now))

(defn filter-applications-by-oppilaitos-and-luokat
  [applications get-oppilaitoksen-opiskelijat oppilaitos-oid luokat]
  (if oppilaitos-oid
    (let [oppilaitoksen-opiskelijat-ja-luokat (when oppilaitos-oid
                                                (get-oppilaitoksen-opiskelijat oppilaitos-oid))
          valitut-koulun-luokat               (set luokat)
          oppilaitoksen-opiskelijat           (when oppilaitos-oid
                                                (->> oppilaitoksen-opiskelijat-ja-luokat
                                                  (filter #(or
                                                             (empty? valitut-koulun-luokat)
                                                             (contains? valitut-koulun-luokat (:luokka %))))
                                                  (map :person-oid)
                                                  (set)))]
      (filter #(contains? oppilaitoksen-opiskelijat (get-in % [:person :oid])) applications))
    applications))
