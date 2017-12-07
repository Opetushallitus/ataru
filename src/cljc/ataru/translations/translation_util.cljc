(ns ataru.translations.translation-util
  (:require [ataru.translations.application-view :refer [application-view-translations]]))

(defn get-translations [lang translation-map]
  (clojure.walk/prewalk (fn [x]
                          (cond-> x
                            (and (map? x)
                                 (contains? x lang))
                            (get lang)))
                        translation-map))

(defn get-translation [key lang]
  (-> application-view-translations key lang))
