(ns ataru.person-service.birth-date-converter)

(def finnish-date-regex #"(\d{1,2})\.(\d{1,2})\.(\d{4})")

(defn convert-birth-date [finnish-format-date]
  {:post [(not= % "--")]} ;; When no match for finnish date, this would result in "--"
  (let [[_ day month year] (re-find finnish-date-regex finnish-format-date)]
    (str year "-" month "-" day)))

(def yyyy-mm-dd-regex #"(\d{4})\-(\d{2})\-(\d{2})")

(defn convert-to-finnish-format [yyyy-mm-dd-date]
  {:post [(not= % "..")]}
  (let [[_ year month day] (re-find yyyy-mm-dd-regex yyyy-mm-dd-date)]
    (str day "." month "." year)))
