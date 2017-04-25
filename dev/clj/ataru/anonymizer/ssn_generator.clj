(ns ataru.anonymizer.ssn-generator
  (:require [ataru.ssn :as ssn]))

(def days-of-month {1 31
                    2 28
                    3 31
                    4 30
                    5 31
                    6 30
                    7 31
                    8 31
                    9 30
                    10 31
                    11 30
                    12 31})

(def check-chars
  {0 "0"
   1 "1"
   2 "2"
   3 "3"
   4 "4"
   5 "5"
   6 "6"
   7 "7"
   8 "8"
   9 "9"
   10 "A"
   11 "B"
   12 "C"
   13 "D"
   14 "E"
   15 "F"
   16 "H"
   17 "J"
   18 "K"
   19 "L"
   20 "M"
   21 "N"
   22 "P"
   23 "R"
   24 "S"
   25 "T"
   26 "U"
   27 "V"
   28 "W"
   29 "X"
   30 "Y"})

(defn gen-day [month]
  (inc (rand-int (get days-of-month month))))

(defn gen-month []
  (inc (rand-int 12)))

(defn gen-year []
  (+ 1940 (rand-int 70)))

(defn ssn-digit->str [the-int]
  (let [str-converted (str the-int)]
    (if (= 1 (count str-converted))
      (str "0" str-converted)
      str-converted)))

(defn gen-synthetic-identifier
  "Between 900-999 the ssns are considered synthetic, can't clash with real ones"
  []
  (+ 900 (rand-int 100)))

(defn gen-divider-char [year]
  (if (< year 2000)
    "-"
    "A"))

(defn gen-check-char [shortened-ssn]
  (get check-chars (mod (Integer/parseInt shortened-ssn) 31)))

(defn generate-ssn []
  {:post [(ssn/ssn? %)]}
  (let [month      (gen-month)
        day        (gen-day month)
        year       (gen-year)
        divider    (gen-divider-char year)
        identifier (gen-synthetic-identifier)
        day-str    (ssn-digit->str day)
        month-str  (ssn-digit->str month)
        year-str   (subs (str year) 2)
        ident-str  (str identifier)
        shortened  (str day-str month-str year-str ident-str)
        check-char (gen-check-char shortened)
        final-ssn  (str day-str month-str year-str divider ident-str check-char)]
    final-ssn))
