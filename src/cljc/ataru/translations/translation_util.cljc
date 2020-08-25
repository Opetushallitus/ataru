(ns ataru.translations.translation-util
  (:require [ataru.translations.texts :refer [translation-mapping]]
            [clojure.walk :as walk]
            #?@(:cljs [[goog.string :as gstring]
                       [goog.string.format]])))

(defn get-translations [lang]
  (walk/prewalk (fn [x]
                  (cond-> x
                          (and (map? x)
                               (contains? x lang))
                          (get lang)))
                translation-mapping))

(def not-found-translations {:fi "Käännöstä ei ole saatavilla. Ole hyvä ja ota yhteyttä ylläpitoon."
                             :sv "Översättning inte tillgänglig. Var vänlig och kontakta administrationen."
                             :en "Translation not available. Please contact an administrator."})

(defn- format-string [text params]
  #?(:cljs (apply gstring/format text params)
     :clj  (apply format text params)))

(defn get-translation [key lang texts & params]
  (let [text (-> texts
                 (get key not-found-translations)
                 (get lang))]
    (cond-> text
            (some? params)
            (format-string params))))

(defn get-hakija-translation [key lang & params]
  (apply get-translation
         key
         lang
         translation-mapping
         params))
