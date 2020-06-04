(ns ataru.number)

(def numeric-matcher #"([+-]?)(0|[1-9][0-9]*)(([,.])([0-9]+))?")

(defn- minus? [sign]
  (= "-" sign))
(defn- plus? [sign]
  (not (minus? sign)))

(defn ->int [thestr]
  #?(:clj  (try (Integer/parseInt thestr) (catch Exception e Double/NaN))
     :cljs (js/parseInt thestr 10)))

(defn isNaN [thenumber]
  #?(:clj  (Double/isNaN thenumber)
     :cljs (js/isNaN thenumber)))

(defn- greater-than-or-equal [[value-int value-dec min-int min-dec]]
  (or (> value-int min-int)
      (and (= value-int min-int)
           (>= value-dec min-dec))))

(defn- normalize [value-int value-dec min-int min-dec]
  (let [value-dec    (or value-dec "0")
        min-dec      (or min-dec "0")
        max-trailing (max (count min-dec) (count value-dec))]
    [(->int (or value-int "0"))
     (->int (apply str value-dec (repeat (- max-trailing (count value-dec)) "0")))
     (->int (or min-int "0"))
     (->int (apply str min-dec (repeat (- max-trailing (count min-dec)) "0")))]))

(defn gte [value min-value]
  (let [[_ value-sign value-int _ _ value-dec] (re-matches numeric-matcher value)
        [_ min-sign min-int _ _ min-dec] (re-matches numeric-matcher min-value)]
    (and
     (not (and (minus? value-sign) (plus? min-sign)))
     (or
      (and (minus? min-sign) (plus? value-sign))
      (if (and (minus? min-sign) (minus? value-sign))
        (greater-than-or-equal (normalize min-int min-dec value-int value-dec))
        (greater-than-or-equal (normalize value-int value-dec min-int min-dec)))))))

(defn lte [value max-value]
  (gte max-value value))