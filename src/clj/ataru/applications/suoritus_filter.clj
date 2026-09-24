(ns ataru.applications.suoritus-filter
  (:require [ataru.time :as time]))

(defn year-for-suoritus-filter
  [now]
  (when now
    (time/year now)))



