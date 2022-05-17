(ns ataru.valinta-laskenta-service.valintalaskentaservice-service
  (:require [ataru.valinta-laskenta-service.valintalaskentaservice-protocol :refer [ValintaLaskentaService]]
            [ataru.valinta-tulos-service.valintatulosservice-protocol :as vts]
            [ataru.valinta-laskenta-service.valintalaskentaservice-client :as client]))

(defn- localize-state
  [state]
  (cond
    (= "HYVAKSYTTY" state)
    :accepted

    (= "HYLATTY" state)
    :rejected

    (= "KESKEN" state)
    :incomplete

    (= "EI_TEHTY" state)
    :not-done

    :else
    state))

(defn- parse-exam
  [koe]
  (let [name (:nimi koe)
        tila (localize-state (get-in koe [:osallistuminenTulos :tila]))
        tulos (get-in koe [:osallistuminenTulos :laskentaTulos])
        arvo (cond
               (true? tulos)
               :accepted

               (false? tulos)
               :rejected

               :else
               :not-done)]
    {:nimi name
     :arvo arvo
     :tila tila
     :localize-arvo true}))

(defn- parse-pisteet
  [pisteet hakukohde-oid]
  (let [parse-tulos (fn [tulos]
                      {:tunniste (:tunniste tulos)
                       :arvo (:arvo tulos)
                       :nimi {:fi (:nimiFi tulos) :sv (:nimiSv tulos) :en (:nimiEn tulos)}})
        valinnanvaiheet (->> (:hakukohteet pisteet)
                             (filter #(= hakukohde-oid (:oid %)))
                             first
                             :valinnanvaihe)
        funktio-tulokset (->> valinnanvaiheet
                              (mapcat :valintatapajonot)
                              (mapcat :jonosijat)
                              (mapcat :funktioTulokset)
                              (map parse-tulos)
                              (sort-by :tunniste))
        exams (->> valinnanvaiheet
                    (mapcat :valintakokeet)
                    (map parse-exam))]
    (concat funktio-tulokset exams)))

(defn- parse-tulos
  [tulos pisteet]
  (let [hakutoiveet (->> (:hakutoiveet tulos)
                         (map (fn [toive]
                                {:oid (:hakukohdeOid toive)
                                 :name (str (:hakukohdeNimi toive) " - " (:tarjoajaNimi toive))
                                 :kokonaispisteet (:pisteet toive)
                                 :valintatila (localize-state (:valintatila toive))
                                 :vastaanottotila (localize-state (:vastaanottotila toive))
                                 :ilmoittautumistila (localize-state (get-in toive [:ilmoittautumistila :ilmoittautumistila]))
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