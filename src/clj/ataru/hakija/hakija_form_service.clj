(ns ataru.hakija.hakija-form-service
  (:require [ataru.forms.form-store :as form-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [taoensso.timbre :refer [warn]]))

(defn fetch-form-by-key [key]
  (let [form (form-store/fetch-by-key key)]
    (when (and (some? form)
               (not (true? (:deleted form))))
      (-> form
          (koodisto/populate-form-koodisto-fields)))))

(defn fetch-form-by-hakukohde-oid [hakukohde-oid]
  (let [result         (tarjonta-client/get-hakukohde hakukohde-oid)
        form-key       (:ataruLomakeAvain result)
        form           (when form-key (fetch-form-by-key form-key))]
    (if form
      (merge form {:hakukohde-oid  hakukohde-oid
                   :hakukohde-name (-> result :hakukohteenNimet :kieli_fi)}))
      (warn "could not find local form for hakukohde" hakukohde-oid "with key" form-key)))