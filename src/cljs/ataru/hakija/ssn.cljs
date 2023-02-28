(ns ataru.hakija.ssn)

(def last-century #{"-" "U" "V" "W" "X" "Y"})
(def this-century #{"A" "B" "C" "D" "E" "F"})

(defn- do-parse-birth-date-from-ssn
  [ssn]
  (let [century-sign (nth ssn 6)
        day          (subs ssn 0 2)
        month        (subs ssn 2 4)
        year         (subs ssn 4 6)
        century      (cond
                       (contains? this-century century-sign) "20"
                       (contains? last-century century-sign) "19"
                       (= century-sign "+") 18
                       :else (throw (js/Error. (str "Unknown century: " century-sign))))]
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
