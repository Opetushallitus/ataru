(ns ataru.tarjonta-service.hakukohde
  (:require [clojure.string :refer [join blank?]]))

(defn- koulutukset->str
  "Produces a condensed string to better identify a hakukohde by its koulutukset"
  [koulutukset]
  (->> koulutukset
       (map (fn [koulutus]
              (->> [(-> koulutus :koulutuskoodi-name :fi)
                    (-> koulutus :tutkintonimike-name :fi)
                    (:tarkenne koulutus)]
                   (remove blank?)
                   (distinct)
                   (join ", "))))
       (remove blank?)
       (distinct)
       (join "; ")))

(defn populate-hakukohde-answer-options [form tarjonta-info]
  (update form :content
          (fn [content]
            (clojure.walk/prewalk
              (fn [field]
                (if (= (:fieldType field) "hakukohteet")
                  (-> field
                      (assoc :options
                             (map (fn [{:keys [oid name koulutukset]}]
                                    {:value oid
                                     :label {:fi (or (:fi name) "")
                                             :sv (or (:sv name) "")
                                             :en (or (:en name) "")}
                                     ; TODO support other languages
                                     :description {:fi (koulutukset->str koulutukset)
                                                   :sv ""
                                                   :en ""}})
                                  (get-in tarjonta-info [:tarjonta :hakukohteet])))
                      (assoc-in [:params :max-hakukohteet] (get-in tarjonta-info [:tarjonta :max-hakukohteet])))
                  field))
              content))))
