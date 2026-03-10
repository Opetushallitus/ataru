(ns ataru.valinta-laskenta-service.valintalaskentaservice-service
  (:require [ataru.valinta-laskenta-service.valintalaskentaservice-protocol :refer [ValintaLaskentaService]]
            [ataru.valinta-tulos-service.valintatulosservice-protocol :as vts]
            [ataru.valinta-laskenta-service.valintalaskentaservice-client :as client]
            [ataru.time :as time])
  (:import [java.time ZonedDateTime Instant]))

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
    (= "VARALLA" state)
    :reserve
    (= "PERUUNTUNUT" state)
    :cancelled
    (= "PERUUTETTU" state)
    :cancelled-by-someone
    (= "PERUNUT" state)
    :cancelled-by-applicant
    (= "VARASIJALTA_HYVAKSYTTY" state)
    :accepted-from-reserve
    (= "VASTAANOTTANUT_SITOVASTI" state)
    :bindingly-received
    (= "LASNA_KOKO_LUKUVUOSI" state)
    :present-whole-academic-year
    (= "POISSA_KOKO_LUKUVUOSI" state)
    :away-whole-acedemic-year
    (= "LASNA_SYKSY" state)
    :present-autumn
    (= "POISSA_SYKSY" state)
    :away-autumn
    (= "LASNA" state)
    :present-spring
    (= "AWAY" state)
    :away-spring
    (= "HARKINNANVARAISESTI_HYVAKSYTTY" state)
    :accepted-harkinnanvaraisesti
    (= "EI_ILMOITTAUTUNUT" state)
    :not-enrolled
    (= "EI_VASTAANOTETTU_MAARA_AIKANA" state)
    :not-received-during-period
    (= "OTTANUT_VASTAAN_TOISEN_PAIKAN" state)
    :received-another
    (= "EHDOLLISESTI_VASTAANOTTANUT" state)
    :conditionally-received
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

(defn- is-fetching-valinnat-allowed?
  [time-window]
  (let [start  (when (:dateStart time-window)
                 (-> (:dateStart time-window)
                     (Instant/ofEpochMilli)
                     (ZonedDateTime/ofInstant (time/time-zone-for-id "Europe/Helsinki"))))
        end    (when (:dateEnd time-window)
                 (-> (:dateEnd time-window)
                     (Instant/ofEpochMilli)
                     (ZonedDateTime/ofInstant (time/time-zone-for-id "Europe/Helsinki"))))
        present (ZonedDateTime/now (time/time-zone-for-id "Europe/Helsinki"))]
  (cond
    (and (nil? start) (nil? end))
    true

    (nil? end)
    (.isBefore present start)

    (nil? start)
    (.isAfter present end)

    :else
    (or (.isBefore present start)
         (.isAfter present end)))))

(defrecord RemoteValintaLaskentaService [cas-client valinta-tulos-service]
  ValintaLaskentaService

  (hakemuksen-tulokset [_ hakukohde-oid haku-oid]
    (get-valinnan-ja-laskennan-tulos-hakemukselle cas-client valinta-tulos-service hakukohde-oid haku-oid))

  (valinnan-tuloksien-hakeminen-sallittu? [_ superuser? haku]
    (if superuser?
      true
      (is-fetching-valinnat-allowed? (:valinnat-estetty-time-window haku)))))
