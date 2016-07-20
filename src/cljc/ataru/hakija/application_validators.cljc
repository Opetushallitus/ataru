(ns ataru.hakija.application-validators)

(defn ^:private required
  [value]
  (not (clojure.string/blank? value)))

(def validators {:required required})

(defn validate
  [validator-kwd value]
  (when-let [validate-fn (validator-kwd validators)]
    (validate-fn value)))
