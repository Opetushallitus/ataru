(ns ataru.application-common.koulutus
  (:require [clojure.string :refer [join blank?]]))

(defn koulutukset->str
  "Produces a condensed string to better identify a hakukohde by its koulutukset"
  [koulutukset]
  (->> koulutukset
       (map (fn [koulutus]
              (->> [(:koulutuskoodi-name koulutus)
                    (:tutkintonimike-name koulutus)
                    (:tarkenne koulutus)]
                   (remove blank?)
                   (distinct)
                   (join ", "))))
       (remove blank?)
       (distinct)
       (join "; ")))

