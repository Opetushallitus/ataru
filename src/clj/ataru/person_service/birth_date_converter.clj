(ns ataru.person-service.birth-date-converter
  (:require [ataru.time.format :as f]))

(def finnish-parse-format (f/formatter "d.M.yyyy"))
(def finnish-format (f/formatter "dd.MM.yyyy"))
(def default-format (f/formatters :year-month-day))

(defn convert-birth-date [finnish-format-date]
  (->> finnish-format-date
       (f/parse finnish-parse-format)
       (f/unparse default-format)))

(defn convert-to-finnish-format [yyyy-mm-dd-date]
  (->> yyyy-mm-dd-date
       (f/parse default-format)
       (f/unparse finnish-format)))
