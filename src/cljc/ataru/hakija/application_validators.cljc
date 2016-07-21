(ns ataru.hakija.application-validators)

(defn ^:private required
  [value]
  (not (clojure.string/blank? value)))

(def ^:private ssn-pattern #"^(\d{2})(\d{2})(\d{2})([-|\+|A])(\d{3})([0-9a-zA-Z])$")

(def ^:private check-chars {0  "0"
                            1  "1"
                            2  "2"
                            3  "3"
                            4  "4"
                            5  "5"
                            6  "6"
                            7  "7"
                            8  "8"
                            9  "9"
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

(defn ^:private ssn
  [value]
  (when-let [[_ day month year _ individual check] (re-matches ssn-pattern value)]
    (let [check-str  (str day month year individual)
          check-num  #?(:clj (Integer/parseInt check-str)
                        :cljs (js/parseInt check-str))
          check-mod  (mod check-num 31)
          check-char (get check-chars check-mod)]
      (= (clojure.string/upper-case check) check-char))))

(def validators {"required" required
                 "ssn"      ssn})

(defn validate
  [validator value]
  (when-let [validate-fn (get validators validator)]
    (validate-fn value)))
