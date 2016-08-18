(ns ataru.hakija.application-validators
  (:require [clojure.string]
            #?(:clj  [clj-time.core :as c]
               :cljs [cljs-time.core :as c])
            #?(:clj  [clj-time.format :as f]
               :cljs [cljs-time.format :as f])
            #?(:clj  [clojure.core.match :refer [match]]
               :cljs [cljs.core.match :refer-macros [match]])))

(defn ^:private required?
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

(defn- ->int [thestr]
  #?(:clj  (Integer/parseInt thestr)
     :cljs (js/parseInt thestr 10)))

(defn valid-year? [year century]
  {:pre [(integer? year)]}
  (let [current-year (c/year (c/now))]
    (and
      ; not (given year between 2000 and current-year)
      (not
        (and
          (or (= "-" century)
            (= "+" century))
          (-> year (>= 2000))
          (-> year (<= current-year))))
      ; not (given century is A and year in future)
      (not
        (and
          (or (= "A" century) (= "a" century))
          (-> year (>= (+ current-year 1))))))))

(defn- ssn?
  [value]
  (when-not (nil? value)
    (when-let [[_ day month year century individual check] (re-matches ssn-pattern value)]
      (let [check-str  (str day month year individual)
            check-num  (->int check-str) 
            check-mod  (mod check-num 31)
            check-char (get check-chars check-mod)]
        (and
          (valid-year? (+ 2000 (->int year)) century)
          (= (clojure.string/upper-case check) check-char))))))

(def ^:private email-pattern #"^[^\s@]+@(([a-zA-Z\-0-9])+\.)+([a-zA-Z\-0-9]){2,}$")
(def ^:private invalid-email-pattern #".*([^\x00-\x7F]|%0[aA]).")

(defn ^:private email?
  [value]
  (and (not (nil? value))
       (not (nil? (re-matches email-pattern value)))
       (nil? (re-find invalid-email-pattern value))))

(def ^:private postal-code-pattern #"^\d{5}$")

(defn ^:private postal-code?
  [value]
  (and (not (nil? value))
       (not (nil? (re-matches postal-code-pattern value)))))

(def ^:private whitespace-pattern #"\s*")
(def ^:private phone-pattern #"^\+?\d{4,}$")

(defn ^:private phone?
  [value]
  (if-not (nil? value)
    (let [parsed (clojure.string/replace value whitespace-pattern "")]
      (not (nil? (re-matches phone-pattern parsed))))
    false))

#?(:clj
   (def parse-date
     (let [formatter (f/formatter (c/time-zone-for-id "Europe/Helsinki")
                                  "dd.MM.YYYY"
                                  "ddMMYYYY")]
       (fn [d]
         (try
           (f/parse formatter d)
           (catch Exception _ nil)))))
   :cljs
   (def parse-date
     (let [formatters (mapv f/formatter ["d.M.YYYY" "ddMMYYYY"])]
       (fn [d]
         (first
           (filter some? (map
                           #(try (f/parse % d)
                                 (catch :default _ nil))
                           formatters)))))))

(defn ^:private date?
  [value]
  (boolean
    (some->>
      value
      parse-date)))

(defn ^:private past-date? [value]
  (boolean
    (and (date? value)
         (some-> (parse-date value)
                 (c/before? (c/today-at-midnight))))))

(def validators {:required    required?
                 :ssn         ssn?
                 :email       email?
                 :postal-code postal-code?
                 :phone       phone?
                 :past-date   past-date?})

(defn validate
  [validator value]
  (boolean
    (when-let [validator-fn (get validators
                              (if (keyword? validator)
                                validator
                                (keyword validator)))]
      (validator-fn value))))
