(ns ataru.hakija.application-validators
  (:require [clojure.string]
            [ataru.email :as email]
            [ataru.ssn :as ssn]
            #?(:clj  [clj-time.core :as c]
               :cljs [cljs-time.core :as c])
            #?(:clj  [clj-time.format :as f]
               :cljs [cljs-time.format :as f])
            #?(:clj  [clojure.core.match :refer [match]]
               :cljs [cljs.core.match :refer-macros [match]])))

(defn ^:private required?
  [value _]
  (if (or (seq? value) (vector? value))
    (not (empty? value))
    (not (clojure.string/blank? value))))

(defn- ssn?
  [value _]
  (ssn/ssn? value))

(def ^:private postal-code-pattern #"^\d{5}$")

(defn ^:private postal-code?
  [value _]
  (and (not (nil? value))
       (not (nil? (re-matches postal-code-pattern value)))))

(def ^:private whitespace-pattern #"\s*")
(def ^:private phone-pattern #"^\+?\d{4,}$")
(def ^:private finnish-date-pattern #"^\d{1,2}\.\d{1,2}\.\d{4}$")

(defn ^:private phone?
  [value _]
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
  [value _]
  (boolean
    (some->>
      value
      parse-date)))

(defn ^:private past-date?
  [value _]
  (boolean
    (and (date? value _)
         (some-> (parse-date value)
                 (c/before? (c/today-at-midnight))))))

(defn- main-first-name?
  [value answers-by-key]
  (let [first-names     (clojure.string/split (-> answers-by-key :first-name :value) #"[\s-]+")
        num-first-names (count first-names)
        possible-names  (set
                          (for [sub-length (range 1 (inc num-first-names))
                                start-idx  (range 0 num-first-names)
                                :when (<= (+ sub-length start-idx) num-first-names)]
                            (clojure.string/join " " (subvec first-names start-idx (+ start-idx sub-length)))))]
    (contains? possible-names (clojure.string/replace value "-" " "))))

(def validators {:required        required?
                 :ssn             ssn?
                 :email           email/email?
                 :postal-code     postal-code?
                 :phone           phone?
                 :past-date       past-date?
                 :main-first-name main-first-name?})

(defn validate
  [validator value answers-by-key]
  (boolean
    (when-let [validator-fn ((keyword validator) validators)]
      (validator-fn value answers-by-key))))
