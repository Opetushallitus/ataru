(ns ataru.person-service.birth-date-converter)

(def finnish-date-regex #"(\d{1,2})\.(\d{1,2})\.(\d{4})")

(defn convert-birth-date [finnish-format-date]
  {:post [(not= % "--")]} ;; When no match for finnish date, this would result in "--"
  (let [[_ day month year] (re-find finnish-date-regex finnish-format-date)]
    (str year "-" month "-" day)))
