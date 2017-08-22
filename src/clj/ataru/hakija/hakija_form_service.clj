(ns ataru.hakija.hakija-form-service
  (:require [ataru.forms.form-store :as form-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.tarjonta-service.hakukohde :refer [populate-hakukohde-answer-options]]
            [taoensso.timbre :refer [warn]]))

(defn fetch-form-by-key
  [key]
  (let [form (form-store/fetch-by-key key)]
    (when (and (some? form)
               (not (true? (:deleted form))))
      (-> form
          (koodisto/populate-form-koodisto-fields)))))

(defn fetch-form-by-haku-oid
  [tarjonta-service haku-oid]
  (let [tarjonta-info (tarjonta-parser/parse-tarjonta-info-by-haku tarjonta-service haku-oid)
        form-keys     (->> (-> tarjonta-info :tarjonta :hakukohteet)
                           (map :form-key)
                           (distinct)
                           (remove nil?))
        form          (when (= 1 (count form-keys))
                        (fetch-form-by-key (first form-keys)))]
    (when (not tarjonta-info)
      (throw (Exception. (str "No haku found for haku " haku-oid " and keys " (pr-str form-keys)))))
    (if form
      (populate-hakukohde-answer-options
        ; remove hakukohteet from form tarjonta for deduplication
        (merge form (assoc-in tarjonta-info [:tarjonta :hakukohteet] []))
        tarjonta-info)
      (warn "could not find local form for haku" haku-oid "with keys" (pr-str form-keys)))))

(defn fetch-form-by-hakukohde-oid
  [tarjonta-service hakukohde-oid]
  (let [hakukohde (.get-hakukohde tarjonta-service hakukohde-oid)
        form      (fetch-form-by-haku-oid tarjonta-service (:hakuOid hakukohde))]
    (when form
      (assoc-in
        form
        [:tarjonta :default-hakukohde]
        (tarjonta-parser/parse-hakukohde tarjonta-service hakukohde)))))
