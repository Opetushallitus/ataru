(ns ataru.virkailija.soresu.person-info-module
  (:require [ataru.virkailija.soresu.component :as component]))

(def ^:private person-info-section
  {:label {:fi "Henkilötiedot"
           :sv "Personlig information"}
   :children []
   :focus? false})

(defn person-info-module
  []
  (merge (component/form-section) person-info-section))
