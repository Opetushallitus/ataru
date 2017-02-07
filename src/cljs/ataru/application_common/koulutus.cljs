(ns ataru.application-common.koulutus
  (:require [clojure.string :as string :refer [join blank?]]))

(defn koulutukset->str
  [koulutukset]
  (join "; "
        (remove blank?
                (map (fn [koulutus]
                       (join ", "
                             (remove blank?
                                     [(:koulutuskoodi-name koulutus)
                                      (:tarkenne koulutus)])))
                     koulutukset))))

