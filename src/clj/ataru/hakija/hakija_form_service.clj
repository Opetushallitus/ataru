(ns ataru.hakija.hakija-form-service
  (:require [ataru.forms.form-store :as form-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [taoensso.timbre :refer [warn]]))

(defn fetch-form-by-key
  [key]
  (let [form (form-store/fetch-by-key key)]
    (when (and (some? form)
               (not (true? (:deleted form))))
      (-> form
          (koodisto/populate-form-koodisto-fields)))))

(defn fetch-form-by-hakukohde-oid
  [tarjonta-service hakukohde-oid]
  (let [hakukohde     (.get-hakukohde tarjonta-service hakukohde-oid)
        haku-oid      (:hakuOid hakukohde)
        haku          (when haku-oid (.get-haku tarjonta-service haku-oid))
        tarjonta-info (tarjonta-parser/parse-tarjonta-info tarjonta-service hakukohde-oid)
        form-key      (:ataruLomakeAvain hakukohde)
        form          (when form-key (fetch-form-by-key form-key))]
    (when (and hakukohde (not haku))
      (throw (Exception. (str "No haku found for hakukohde" hakukohde-oid))))
    (if form
      (merge form tarjonta-info)
      (warn "could not find local form for hakukohde" hakukohde-oid "with key" form-key))))
