(ns ataru.hakija.hakija-form-service
  (:require [ataru.forms.form-store :as form-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [taoensso.timbre :refer [warn]]))

(defn fetch-form-by-key
  [key]
  (let [form (form-store/fetch-by-key key)]
    (when (and (some? form)
               (not (true? (:deleted form))))
      (-> form
          (koodisto/populate-form-koodisto-fields)))))

(defn- parse-koulutus
  [response]
  {:oid                  (:oid response)
   :koulutuskoodi-name   (-> response :koulutuskoodi :nimi)
   :tutkintonimike-name  (-> response :tutkintonimike :nimi)
   :koulutusohjelma-name (-> response :koulutusohjelma :nimi)
   :tarkenne             (:tarkenne response)})

(defn fetch-form-by-hakukohde-oid
  [tarjonta-service hakukohde-oid]
  (let [hakukohde     (.get-hakukohde tarjonta-service hakukohde-oid)
        haku-oid      (:hakuOid hakukohde)
        haku          (when haku-oid (.get-haku tarjonta-service haku-oid))
        koulutus-oids (map :oid (:koulutukset hakukohde))
        koulutukset   (when koulutus-oids
                        (->> koulutus-oids
                             (map #(.get-koulutus tarjonta-service %))
                             (map parse-koulutus)))
        form-key      (:ataruLomakeAvain hakukohde)
        form          (when form-key (fetch-form-by-key form-key))]
    (when (and hakukohde
               (not haku))
      (throw (Exception. (str "No haku found for hakukohde" hakukohde-oid))))
    (if form
      (merge form
             {:tarjonta
              {:hakukohde-oid      hakukohde-oid
               :hakukohde-name     (-> hakukohde :hakukohteenNimet :kieli_fi)
               :haku-tarjoaja-name (-> hakukohde :tarjoajaNimet :fi)
               :haku-oid           haku-oid
               :haku-name          (-> haku :nimi :kieli_fi)
               :hakuaika-dates     (hakuaika/get-hakuaika-info hakukohde haku)
               :koulutukset        koulutukset}})
      (warn "could not find local form for hakukohde" hakukohde-oid "with key" form-key))))
