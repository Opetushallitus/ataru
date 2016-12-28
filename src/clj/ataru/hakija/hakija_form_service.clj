(ns ataru.hakija.hakija-form-service
  (:require [ataru.forms.form-store :as form-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [taoensso.timbre :refer [warn]]))

(defn fetch-form-by-key
  [key]
  (let [form (form-store/fetch-by-key key)]
    (when (and (some? form)
               (not (true? (:deleted form))))
      (-> form
          (koodisto/populate-form-koodisto-fields)))))

(defn fetch-form-by-hakukohde-oid
  [hakukohde-oid]
  (let [hakukohde (tarjonta-client/get-hakukohde hakukohde-oid)
        haku-oid  (:hakuOid hakukohde)
        haku      (when haku-oid (tarjonta-client/get-haku haku-oid))
        form-key  (:ataruLomakeAvain hakukohde)
        form      (when form-key (fetch-form-by-key form-key))]
    (when (and hakukohde
               (not haku))
      (throw (Exception. (str "No haku found for hakukohde" hakukohde-oid))))
    (if form
      (merge form {:hakukohde-oid      hakukohde-oid
                   :hakukohde-name     (-> hakukohde :hakukohteenNimet :kieli_fi)
                   :haku-tarjoaja-name (-> hakukohde :tarjoajaNimet :fi)
                   :haku-oid           haku-oid
                   :haku-name          (-> haku :nimi :kieli_fi)
                   :hakuaika-dates     {:start (:hakuaikaAlkuPvm hakukohde)
                                        :end   (:hakuaikaLoppuPvm hakukohde)}})
      (warn "could not find local form for hakukohde" hakukohde-oid "with key" form-key))))