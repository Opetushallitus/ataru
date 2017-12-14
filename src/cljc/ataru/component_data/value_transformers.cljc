(ns ataru.component-data.value-transformers
  #?(:cljs (:require [goog.string :as gstring])))

(def ^:private dob-pattern #"^(\d{1,2})\.(\d{1,2})\.(\d{4})$")

(defn birth-date [dob]
  (when-let [[_ day month year] (re-matches dob-pattern dob)]
    (let [f #?(:clj format :cljs gstring/format)]
      (f "%02d.%02d.%d" #?@(:clj  [(Integer/valueOf day)
                                   (Integer/valueOf month)
                                   (Integer/valueOf year)]
                            :cljs [day month year])))))
