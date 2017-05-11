(ns ataru.dob
  (:require #?(:clj [clj-time.core :as t]
               :cljs [cljs-time.core :as t])))

(defonce date-pattern #"^(\d{1,2})\.(\d{1,2})\.(\d{4})$")

(defn dob? [dob]
  (try
    (if-let [[_ day month year] (re-find date-pattern dob)]
      (let [date (t/date-time #?@(:clj  [(Integer/valueOf year)
                                         (Integer/valueOf month)
                                         (Integer/valueOf day)]
                                  :cljs [(int year)
                                         (int month)
                                         (int day)]))
            now  (t/now)]
        (t/before? date now))
      false)
    (catch #?(:clj Exception :cljs :default) _
      false)))
