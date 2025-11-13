(ns ataru.valinta-laskenta-service.valintalaskentaservice-service-spec
  (:require [speclj.core :refer [it describe tags should=]]
            [ataru.valinta-laskenta-service.valintalaskentaservice-service :as vls-service]
            [ataru.valinta-laskenta-service.valintalaskentaservice-protocol :as vls]
            [ataru.valinta-tulos-service.valintatulosservice-protocol :refer [ValintaTulosService]]
            [ataru.cas.client :as cas]
            [cheshire.core :as json])
  (:import [java.time ZonedDateTime ZoneId]))

(def mocked-vls-response
  {:hakukohteet
   [{:oid "1.2.246.562.20.00000000000000009278"
     :valinnanvaihe
     [{:valintatapajonot
       [{:jonosijat
         [{:funktioTulokset
           [{
             :tunniste "peruskaava"
             :arvo     19
             :nimiFi   "2. asteen peruskoulupohjainen peruskaava + Kielitaidon riittävyys - 2 aste, pk ja yo 2021"
             }
            {
             :tunniste "hakutoivejarjestys"
             :arvo     2
             :nimiFi   "Hakutoivejärjestyspisteytys, 2 aste, pk ja yo 2016",
             }
            {
             :tunniste "keskiarvo"
             :arvo     8.5556
             :nimiFi   "Kaikkien aineiden keskiarvo, PK 2019"
             },
            {
             :tunniste "painotettavat_ka"
             :arvo     8.6667
             :nimiFi   "Painotettavien arvosanojen keskiarvo 2016"
             }]
           }]
         }]
       }]
     }]
   })

(def mocked-vts-response {:hakutoiveet [{:hakukohdeOid       "1.2.246.562.20.00000000000000009278"
                                         :hakukohdeNimi      "Lukion yleislinja"
                                         :tarjoajaNimi       "Ressun lukio"
                                         :pisteet            123
                                         :valintatila        "HYVAKSYTTY"
                                         :vastaanottotila    "KESKEN"
                                         :ilmoittautumistila {:ilmoittautumistila "HYVAKSYTTY"}}]})

(def response-with-exam (assoc-in mocked-vls-response
                                  [:hakukohteet 0 :valinnanvaihe 0 :valintakokeet]
                                  [{:nimi                "Valintakoe"
                                    :osallistuminenTulos {:tila          "HYVAKSYTTY"
                                                          :laskentaTulos true}}]))

(defn- time-from-present [modifier]
       (-> (ZonedDateTime/now (ZoneId/of "Europe/Helsinki"))
           .toInstant
           .toEpochMilli
           (+ (* modifier 10000))))

(defrecord FakeValintaTulosService []
  ValintaTulosService

  (valinnan-tulos-hakemukselle [_ _ _]
    mocked-vts-response))

(def cas-client nil)

(describe "valintalaskentaservice spec"
          (tags :unit)
          (it "Return valintatulokset for application"
              (with-redefs [cas/cas-authenticated-get (constantly {:status 200 :body (json/generate-string mocked-vls-response)})]
                (let [mocked-vts-service (->FakeValintaTulosService)
                      vls-service-instance (vls-service/->RemoteValintaLaskentaService cas-client mocked-vts-service)
                      result (first (vls/hakemuksen-tulokset vls-service-instance "1.2.3.4" "1.2.3.4.5.6"))
                      first-piste (first (:pisteet result))]
                  (should= "1.2.246.562.20.00000000000000009278" (:oid result))
                  (should= "Lukion yleislinja - Ressun lukio" (:name result))
                  (should= 123 (:kokonaispisteet result))
                  (should= :accepted (:valintatila result))
                  (should= :incomplete (:vastaanottotila result))
                  (should= :accepted (:ilmoittautumistila result))
                  (should= 4 (count (:pisteet result)))
                  (should= "hakutoivejarjestys" (:tunniste first-piste))
                  (should= 2 (:arvo first-piste))
                  (should= "Hakutoivejärjestyspisteytys, 2 aste, pk ja yo 2016" (get-in first-piste [:nimi :fi])))))

          (it "Returns valintatulokset with exam result for application"
              (with-redefs [cas/cas-authenticated-get (constantly {:status 200 :body (json/generate-string response-with-exam)})]
                (let [mocked-vts-service (->FakeValintaTulosService)
                      vls-service-instance (vls-service/->RemoteValintaLaskentaService cas-client mocked-vts-service)
                      result (first (vls/hakemuksen-tulokset vls-service-instance "1.2.3.4" "1.2.3.4.5.6"))
                      first-piste (first (:pisteet result))
                      last-piste (last (:pisteet result))]
                  (should= "1.2.246.562.20.00000000000000009278" (:oid result))
                  (should= "Lukion yleislinja - Ressun lukio" (:name result))
                  (should= 123 (:kokonaispisteet result))
                  (should= :accepted (:valintatila result))
                  (should= :incomplete (:vastaanottotila result))
                  (should= :accepted (:ilmoittautumistila result))
                  (should= 5 (count (:pisteet result)))
                  (should= "hakutoivejarjestys" (:tunniste first-piste))
                  (should= 2 (:arvo first-piste))
                  (should= "Hakutoivejärjestyspisteytys, 2 aste, pk ja yo 2016" (get-in first-piste [:nimi :fi]))
                  (should= "Valintakoe" (:nimi last-piste))
                  (should= :accepted (:tila last-piste))
                  (should= :accepted (:arvo last-piste))
                  (should= true (:localize-arvo last-piste)))))

          (it "allows fetching valintatulokset when time window is null"
              (let [vls-service-instance (vls-service/->RemoteValintaLaskentaService nil nil)
                    result (vls/valinnan-tuloksien-hakeminen-sallittu? vls-service-instance false {})]
                (should= true result)))

          (it "disallows fetching valintatulokset when start is before present and end is not given"
              (let [vls-service-instance (vls-service/->RemoteValintaLaskentaService nil nil)
                    result (vls/valinnan-tuloksien-hakeminen-sallittu?
                             vls-service-instance
                             false
                             {:valinnat-estetty-time-window {:dateStart (time-from-present -10) :dateEnd nil}})]
                (should= false result)))

          (it "disallows fetching valintatulokset when end is after present and start is not given"
              (let [vls-service-instance (vls-service/->RemoteValintaLaskentaService nil nil)
                    result (vls/valinnan-tuloksien-hakeminen-sallittu?
                             vls-service-instance
                             false
                             {:valinnat-estetty-time-window {:dateStart nil :dateEnd (time-from-present 10)}})]
                (should= false result)))

          (it "allows fetching valintatulokset when start is after present and end is not given"
              (let [vls-service-instance (vls-service/->RemoteValintaLaskentaService nil nil)
                    result (vls/valinnan-tuloksien-hakeminen-sallittu?
                             vls-service-instance
                             false
                             {:valinnat-estetty-time-window {:dateStart (time-from-present 10) :dateEnd nil}})]
                (should= true result)))

          (it "allows fetching valintatulokset when end is before present and start is not given"
              (let [vls-service-instance (vls-service/->RemoteValintaLaskentaService nil nil)
                    result (vls/valinnan-tuloksien-hakeminen-sallittu?
                             vls-service-instance
                             false
                             {:valinnat-estetty-time-window {:dateStart nil :dateEnd (time-from-present -10)}})]
                (should= true result)))

          (it "allows fetching valintatulokset when present is ater time window"
              (let [vls-service-instance (vls-service/->RemoteValintaLaskentaService nil nil)
                    result (vls/valinnan-tuloksien-hakeminen-sallittu?
                             vls-service-instance
                             false
                             {:valinnat-estetty-time-window {:dateStart (time-from-present -20) :dateEnd (time-from-present -10)}})]
                (should= true result)))

          (it "allows fetching valintatulokset when present is before time window"
              (let [vls-service-instance (vls-service/->RemoteValintaLaskentaService nil nil)
                    result (vls/valinnan-tuloksien-hakeminen-sallittu?
                             vls-service-instance
                             false
                             {:valinnat-estetty-time-window {:dateStart (time-from-present 10) :dateEnd (time-from-present 20)}})]
                (should= true result)))

          (it "disallows fetching valintatulokset when present is inside time window"
              (let [vls-service-instance (vls-service/->RemoteValintaLaskentaService nil nil)
                    result (vls/valinnan-tuloksien-hakeminen-sallittu?
                             vls-service-instance
                             false
                             {:valinnat-estetty-time-window {:dateStart (time-from-present -20) :dateEnd (time-from-present 10)}})]
                (should= false result)))

          (it "allows fetching valintatulokset when user is superuser"
              (let [vls-service-instance (vls-service/->RemoteValintaLaskentaService nil nil)
                    result (vls/valinnan-tuloksien-hakeminen-sallittu?
                             vls-service-instance
                             true
                             {:valinnat-estetty-time-window {:dateStart (time-from-present -20) :dateEnd (time-from-present 10)}})]
                (should= true result))))
