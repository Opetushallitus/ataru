(ns ataru.translations.translation-util)

(defn get-translations [lang translation-map]
  (clojure.walk/prewalk (fn [x]
                          (cond-> x
                            (and (map? x)
                                 (contains? x lang))
                            (get lang)))
                        translation-map))
