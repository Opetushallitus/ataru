(ns ataru.applications.suoritus-filter
  (:require [clj-time.core :as time]))

(defn year-for-suoritus-filter
  [now]
  (time/year now))

(defn luokkatasot-for-suoritus-filter
  []
  ["9" "10" "VALMA" "TELMA" "ML" "OPISTOVUOSI" "TUVA"])

(defn luokkatasot-for-suoritus-filter-opiskelija
  []
  ["7" "8" "9" "10" "VALMA" "TELMA" "ML" "OPISTOVUOSI" "valmistava"])

