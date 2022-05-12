(ns ataru.valinta-laskenta-service.valintalaskentaservice-service
  (:require [ataru.valinta-laskenta-service.valintalaskentaservice-protocol :refer [ValintaLaskentaService]]
            [ataru.valinta-tulos-service.valintatulosservice-protocol :as vts]
            [ataru.valinta-laskenta-service.valintalaskentaservice-client :as client]))

(defn- parse-pisteet
  [pisteet hakukohde-oid]
  (let [jarjestyskriteerit (->> (:hakukohteet pisteet)
                                (filter #(= hakukohde-oid (:oid %)))
                                first
                                :valinnanvaihe
                                (mapcat :valintatapajonot)
                                (mapcat :jonosijat)
                                (mapcat :jarjestyskriteerit))]
    jarjestyskriteerit))

(defn- parse-tulos
  [tulos pisteet]
  (let [hakutoiveet (->> (:hakutoiveet tulos)
                         (map (fn [toive]
                                {:oid (:hakukohdeOid toive)
                                 :name (str (:hakukohdeNimi toive) " - " (:tarjoajaNimi toive))
                                 :valintatila (:valintatila toive)
                                 :vastaanottotila (:vastaanottotila toive)
                                 :ilmoittautumistila (get-in toive [:ilmoittautumistila :ilmoittautumistila])
                                 :pisteet (parse-pisteet pisteet (:hakukohdeOid toive))})))]
    hakutoiveet))

(defn- get-valinnan-ja-laskennan-tulos-hakemukselle
  [cas-client valinta-tulos-service haku-oid hakemus-oid]
  (let [tulos (future (vts/valinnan-tulos-hakemukselle valinta-tulos-service haku-oid hakemus-oid))
        pisteet (client/hakemuksen-laskennan-tiedot cas-client haku-oid hakemus-oid)
        parsed-tulos (parse-tulos @tulos pisteet)]
    parsed-tulos))

(defrecord RemoteValintaLaskentaService [cas-client valinta-tulos-service]
  ValintaLaskentaService

  (hakemuksen-tulokset [_ hakukohde-oid haku-oid]
    (get-valinnan-ja-laskennan-tulos-hakemukselle cas-client valinta-tulos-service hakukohde-oid haku-oid)))