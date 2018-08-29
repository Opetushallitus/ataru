(ns ataru.tarjonta-service.hakukohde
  (:require [ataru.util :refer [koulutus->str non-blank-val]]
            [clojure.string :refer [join blank?]]))

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

(defn- as-hakukohde-name [name tarjoaja-name lang]
  (let [ks [lang :fi :en :sv]]
    [lang (str (non-blank-val name ks) " – " (non-blank-val tarjoaja-name ks))]))

(defn- hakukohde->option
  [{:keys [oid name koulutukset tarjoaja-name]}]
  (let [langs (distinct (concat [:fi] (keys name) (keys tarjoaja-name)))]
    {:value       oid
     :label       (into {} (map (partial as-hakukohde-name name tarjoaja-name) langs))
     :description (ensure-finnish (koulutukset->str koulutukset))}))

(defn- populate-hakukohteet-field
  [field tarjonta-info]
  (-> field
      (assoc :options
             (map hakukohde->option
                  (get-in tarjonta-info [:tarjonta :hakukohteet])))
      (assoc-in [:params :max-hakukohteet] (get-in tarjonta-info [:tarjonta :max-hakukohteet]))))

(defn populate-hakukohde-answer-options [form tarjonta-info]
  (update form :content
          (fn [content]
            (clojure.walk/prewalk
              (fn [field]
                (if (= (:fieldType field) "hakukohteet")
                  (populate-hakukohteet-field field tarjonta-info)
                  field))
              content))))
