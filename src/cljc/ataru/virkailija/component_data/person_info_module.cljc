(ns ataru.virkailija.component-data.person-info-module
  (:require [ataru.virkailija.component-data.component :as component]))

(defn ^:private first-name-component
  []
  (merge (component/text-field) {:label {:fi "Etunimet" :sv "Förnamn"} :required true}))

(defn ^:private referrer-name-component
  []
  (merge (component/text-field) {:label {:fi "Kutsumanimi" :sv "Smeknamn"} :required true}))

(defn ^:private first-name-section
  []
  (component/row-section [(first-name-component)
                          (referrer-name-component)]))

(defn ^:private last-name-component
  []
  (merge (component/text-field) {:label {:fi "Sukunimi" :sv "Efternamn"} :required true}))

(defn person-info-module
  []
  (clojure.walk/prewalk
    (fn [x]
      (if (map? x)
        (dissoc x :focus?)
        x))
    (merge (component/form-section) {:label {:fi "Henkilötiedot"
                                             :sv "Personlig information"}
                                     :children [(first-name-section)
                                                (last-name-component)]
                                     :focus? false
                                     :module :person-info})))
