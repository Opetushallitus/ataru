(ns ataru.hakija.application-validators)

(defn ^:private required
  [value]
  (not (clojure.string/blank? value)))

(def validators {"required" required})

(defn validate
  [validator value]
  (when-let [validate-fn (get validators validator)]
    (validate-fn value)))
