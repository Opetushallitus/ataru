(ns ataru.hakija.application-validators
  (:require [clojure.string]
            [ataru.ssn :as ssn]
            #?(:clj  [clj-time.core :as c]
               :cljs [cljs-time.core :as c])
            #?(:clj  [clj-time.format :as f]
               :cljs [cljs-time.format :as f])
            #?(:clj  [clojure.core.match :refer [match]]
               :cljs [cljs.core.match :refer-macros [match]])))

(defn ^:private required?
  [value]
  (if (or (seq? value) (vector? value))
    (not (empty? value))
    (not (clojure.string/blank? value))))

(defn- ssn?
  [value]
  (ssn/ssn? value))

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
(def ^:private finnish-date-pattern #"^\d{1,2}\.\d{1,2}\.\d{4}$")

(defn ^:private phone?
  [value]
  (if-not (nil? value)
    (let [parsed (clojure.string/replace value whitespace-pattern "")]
      (not (nil? (re-matches phone-pattern parsed))))
    false))

#?(:clj
   (def parse-date
     (let [formatter (f/formatter "dd.MM.YYYY" (c/time-zone-for-id "Europe/Helsinki"))]
       (fn [d]
         (try
           (f/parse formatter d)
           (catch Exception _ nil)))))
   :cljs
   (def parse-date
     (let [formatter (f/formatter "d.M.YYYY")]
       (fn [d]
         ;; Unfortunately cljs-time.format allows
         ;; garbage data (e.g. XXXXX11Y11Z1997BLAH),
         ;; so we have to protect against that with a regexp
         (if (re-matches finnish-date-pattern d)
           (try (f/parse formatter d)
                (catch :default _ nil))
           nil)))))

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
