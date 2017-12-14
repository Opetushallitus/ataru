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
  (-> translation-mapping
      (get key {:fi "Käännöstä ei ole saatavilla. Ole hyvä ja ota yhteyttä ylläpitoon."
                :sv "Översättning inte tillgänglig. Var vänlig och kontakta administrationen."
                :en "Translation not available. Please contact an administrator."})
      (get lang)))
