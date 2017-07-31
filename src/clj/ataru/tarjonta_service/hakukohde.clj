(ns ataru.tarjonta-service.hakukohde
  (:require [clojure.string :refer [join blank?]]))

(defn- koulutus->str
  [koulutus lang]
  (->> [(-> koulutus :koulutuskoodi-name lang)
        (-> koulutus :tutkintonimike-name lang)
        (:tarkenne koulutus)]
       (remove #(or (nil? %) (blank? %)))
       (distinct)
       (join ", ")))

(defn- koulutus->str-map
  [koulutus]
  (->> [:fi :sv :en]
       (map (juxt identity (partial koulutus->str koulutus)))
       (remove (comp blank? second))
       (into {})))

(defn- koulutukset->str
  "Produces a condensed string to better identify a hakukohde by its koulutukset"
  [koulutukset]
  (->> koulutukset
       (map koulutus->str-map)
       (apply merge-with conj {:fi [] :sv [] :en []})
       (reduce-kv #(assoc %1 %2 (join "; " %3)) {})
       (remove (comp blank? second))
       (into {})))

(defn- ensure-finnish
  [m]
  (assoc m :fi (or (:fi m) (:en m) (:sv m))))

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
                                     :label (ensure-finnish name)
                                     :description (ensure-finnish (koulutukset->str koulutukset))})
                                  (get-in tarjonta-info [:tarjonta :hakukohteet])))
                      (assoc-in [:params :max-hakukohteet] (get-in tarjonta-info [:tarjonta :max-hakukohteet])))
                  field))
              content))))
