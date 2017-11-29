(ns ataru.person-service.birth-date-converter
  (:require [clj-time.format :as f]))

(def finnish-format (f/formatter "dd.MM.yyyy"))
(def default-format (f/formatters :year-month-day))

(defn convert-birth-date [finnish-format-date]
  (->> finnish-format-date
       (f/parse finnish-format)
       (f/unparse default-format)))

(defn convert-to-finnish-format [yyyy-mm-dd-date]
  (->> yyyy-mm-dd-date
       (f/parse default-format)
       (f/unparse finnish-format)))
