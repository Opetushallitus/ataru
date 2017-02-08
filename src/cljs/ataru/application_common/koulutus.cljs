(ns ataru.application-common.koulutus
  (:require [clojure.string :as string :refer [join blank?]]))

(defn koulutukset->str
  [koulutukset]
  (join "; "
        (distinct
          (remove blank?
                  (map (fn [koulutus]
                         (join ", "
                               (distinct
                                 (remove blank?
                                         [(:koulutuskoodi-name koulutus)
                                          (:tutkintonimike-name koulutus)
                                          (:tarkenne koulutus)]))))
                       koulutukset)))))

