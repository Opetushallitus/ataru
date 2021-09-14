(ns ataru.hakija.ssn)

(defn- do-parse-birth-date-from-ssn
  [ssn]
  (let [century-sign (nth ssn 6)
        day          (subs ssn 0 2)
        month        (subs ssn 2 4)
        year         (subs ssn 4 6)
        century      (case century-sign
                       "+" "18"
                       "-" "19"
                       "A" "20")]
    (str day "." month "." century year)))

(defn parse-birth-date-from-ssn
  [demo? ssn]
  (if demo?
    (try
      (do-parse-birth-date-from-ssn ssn)
      (catch :default _e
        ""))
    (do-parse-birth-date-from-ssn ssn)))

(defn- do-parse-gender-from-ssn
  [ssn]
  (if (zero? (mod (js/parseInt (nth ssn 9)) 2))
    "2"                                                     ;; based on koodisto-values
    "1"))

(defn parse-gender-from-ssn
  [demo? ssn]
  (if demo?
    (try
      (do-parse-gender-from-ssn ssn)
      (catch :default _e
        ""))
    (do-parse-gender-from-ssn ssn)))
