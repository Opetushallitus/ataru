(ns ataru.translations.translation-util
  (:require [ataru.translations.texts :refer [translation-mapping]]))

(defn get-translations [lang]
  (clojure.walk/prewalk (fn [x]
                          (cond-> x
                            (and (map? x)
                                 (contains? x lang))
                            (get lang)))
                        translation-mapping))

(defn get-translation [key lang]
  (-> translation-mapping key lang))
