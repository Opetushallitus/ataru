(ns ataru.preferred-name)

(defn main-first-name?
  [{:keys [value answers-by-key]}]
  (let [first-names (-> (-> answers-by-key :first-name :value)
                        clojure.string/trim
                        (clojure.string/split #"\s+")
                        set)]
    (contains? first-names value)))
