(ns ataru.tarjonta-service.tarjonta-parser
  (:require [ataru.tarjonta-service.hakuaika :as hakuaika]))

(defn- parse-koulutus
  [response]
  {:oid                  (:oid response)
   :koulutuskoodi-name   (-> response :koulutuskoodi :nimi)
   :tutkintonimike-name  (-> response :tutkintonimike :nimi)
   :koulutusohjelma-name (-> response :koulutusohjelma :nimi)
   :tarkenne             (:tarkenne response)})

(defn parse-tarjonta-info
  [tarjonta-service hakukohde-oid]
  (when hakukohde-oid
    (let [hakukohde     (.get-hakukohde tarjonta-service hakukohde-oid)
          haku-oid      (:hakuOid hakukohde)
          haku          (when haku-oid (.get-haku tarjonta-service haku-oid))
          koulutus-oids (map :oid (:koulutukset hakukohde))
          koulutukset   (when koulutus-oids
                          (->> koulutus-oids
                               (map #(.get-koulutus tarjonta-service %))
                               (map parse-koulutus)))]
      (when hakukohde ;; If tarjonta doesn't return hakukohde, let's not return a crippled map here
        {:tarjonta
         {:hakukohde-oid      hakukohde-oid
          :hakukohde-name     (-> hakukohde :hakukohteenNimet :kieli_fi)
          :haku-tarjoaja-name (-> hakukohde :tarjoajaNimet :fi)
          :haku-oid           haku-oid
          :haku-name          (-> haku :nimi :kieli_fi)
          :hakuaika-dates     (hakuaika/get-hakuaika-info hakukohde haku)
          :koulutukset        koulutukset}}))))
