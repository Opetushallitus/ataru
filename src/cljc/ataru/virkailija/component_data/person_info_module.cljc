(ns ataru.virkailija.component-data.person-info-module
  (:require [ataru.virkailija.component-data.component :as component]))

(def ^:private first-name-component
  (merge (component/text-field) {:label {:fi "Etunimet" :sv "Förnamn"} :required true}))

(def ^:private last-name-component
  (merge (component/text-field) {:label {:fi "Sukunimi" :sv "Efternamn"} :required true}))

(def ^:private person-info-section
  {:label {:fi "Henkilötiedot"
           :sv "Personlig information"}
   :children [first-name-component
              last-name-component]
   :focus? false})

(defn person-info-module
  []
  (merge (component/form-section) person-info-section))
