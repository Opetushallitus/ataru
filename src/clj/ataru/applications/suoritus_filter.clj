(ns ataru.applications.suoritus-filter
  (:require [ataru.time :as time]))

(defn year-for-suoritus-filter
  [now]
  (when now
    (time/year now)))

(defn luokkatasot-for-suoritus-filter
  []
  ["7" "8" "9" "10" "VALMA" "TELMA" "TUVA" "ML" "OPISTOVUOSI" "valmistava"])

