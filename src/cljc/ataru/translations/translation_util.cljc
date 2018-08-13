(ns ataru.translations.translation-util
  (:require [ataru.translations.texts :refer [translation-mapping virkailija-texts]]))

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

(defn get-virkailija-translation [key lang params]
  (if-let [text-value (get virkailija-texts key)]
    (if (fn? text-value)
      (-> text-value
          (apply params)
          (get lang))
      (get text-value lang))
    (println "No key found in translations:" key)))
