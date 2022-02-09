(ns ataru.applications.suoritus-filter
  (:require [clj-time.core :as time]))

(defn year-for-suoritus-filter
  [now]
  (time/year now))

(defn luokkatasot-for-suoritus-filter
  []
  ["9" "10" "VALMA" "TELMA"])

(defn filter-applications-by-oppilaitos-and-luokat
  [applications get-haku get-oppilaitoksen-opiskelijat oppilaitos-oid luokat]
  (if oppilaitos-oid
    (let [hakujen-vuodet                           (->> applications
                                                        (map :haku)
                                                        (distinct)
                                                        (map get-haku)
                                                        (mapcat :hakuajat)
                                                        (map #(year-for-suoritus-filter (:end %)))
                                                        (distinct))
          oppilaitoksen-opiskelijat-ja-luokat (mapcat #(get-oppilaitoksen-opiskelijat oppilaitos-oid %) hakujen-vuodet)
          valitut-koulun-luokat               (set luokat)
          oppilaitoksen-opiskelijat           (->> oppilaitoksen-opiskelijat-ja-luokat
                                                  (filter #(or
                                                             (empty? valitut-koulun-luokat)
                                                             (contains? valitut-koulun-luokat (:luokka %))))
                                                  (map :person-oid)
                                                  (set))]
      (filter #(contains? oppilaitoksen-opiskelijat (get-in % [:person :oid])) applications))
    applications))
