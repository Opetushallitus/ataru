(ns ataru.virkailija.component-data.value-transformers
  (:require [goog.string :as gstring]))

(def ^:private dob-pattern #"^(\d{1,2})\.(\d{1,2})\.(\d{4})$")

(defn birth-date [dob]
  (when-let [[_ day month year] (re-matches dob-pattern dob)]
    (gstring/format "%02d.%02d.%d" day month year)))
