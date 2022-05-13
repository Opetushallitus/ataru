(ns ataru.valinta-laskenta-service.valintalaskentaservice-service
  (:require [ataru.valinta-laskenta-service.valintalaskentaservice-protocol :refer [ValintaLaskentaService]]
            [ataru.valinta-tulos-service.valintatulosservice-protocol :as vts]
            [ataru.valinta-laskenta-service.valintalaskentaservice-client :as client]))

; :valinnanvaihe -> :valintakokeet -> [:nimi] -> osallistuminenTulos [laskentaTila -> :tila laskentatulos -> :arvo :kuvaus
(defn- parse-pisteet
  [pisteet hakukohde-oid]
  (let [parse-tulos (fn [tulos]
                      {:tunniste (:tunniste tulos)
                       :arvo (:arvo tulos)
                       :nimi {:fi (:nimiFi tulos) :sv (:nimiSv tulos) :en (:nimiEn tulos)}})
        funktio-tulokset (->> (:hakukohteet pisteet)
                                (filter #(= hakukohde-oid (:oid %)))
                                first
                                :valinnanvaihe
                                (mapcat :valintatapajonot)
                                (mapcat :jonosijat)
                                (mapcat :funktioTulokset)
                                (map parse-tulos)
                                (sort-by :tunniste))]
    funktio-tulokset))

(defn- parse-tulos
  [tulos pisteet]
  (let [hakutoiveet (->> (:hakutoiveet tulos)
                         (map (fn [toive]
                                {:oid (:hakukohdeOid toive)
                                 :name (str (:hakukohdeNimi toive) " - " (:tarjoajaNimi toive))
                                 :kokonaispisteet (:pisteet toive)
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