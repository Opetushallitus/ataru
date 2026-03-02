(ns ataru.dob
  (:require #?(:clj [ataru.time :as t]
               :cljs [cljs-time.core :as t])))

(defonce date-pattern #"^(\d{1,2})\.(\d{1,2})\.(\d{4})$")

(defn str->dob [dob-str]
  (try
    (when-let [[_ day month year] (re-find date-pattern dob-str)]
      (t/date-time #?@(:clj  [(Integer/valueOf year)
                              (Integer/valueOf month)
                              (Integer/valueOf day)]
                       :cljs [(int year)
                              (int month)
                              (int day)])))
    (catch #?(:clj Exception :cljs :default) _)))

(defn dob? [dob-str]
  (if (str->dob dob-str)
    true
    false))
