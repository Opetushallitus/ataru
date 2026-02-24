(ns ataru.util.deadline-util
  (:require [ataru.time :as time]
            [ataru.time.format :as f]
            ))

(defn custom-deadline [field]
  (get-in field [:params :deadline]))

(def deadline-format (f/formatter "dd.MM.yyyy HH:mm" (time/time-zone-for-id "Europe/Helsinki")))

(defn custom-deadline-passed? [now field]
  (some->> (custom-deadline field)
           (f/parse deadline-format)
           (time/after? now)))
