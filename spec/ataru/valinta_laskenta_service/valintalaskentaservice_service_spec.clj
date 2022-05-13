(ns ataru.valinta-laskenta-service.valintalaskentaservice-service-spec
  (:require [speclj.core :refer [it describe tags should=]]
            [ataru.valinta-laskenta-service.valintalaskentaservice-service :as vls-service]
            [ataru.valinta-laskenta-service.valintalaskentaservice-protocol :as vls]
            [clj-http.client :as http]
            [ataru.cas.client :as cas]
            [cheshire.core :as json]))

(def mocked-vls-response {
                          :hakukohteet [{
                                         :oid "1.2.246.562.20.00000000000000009278"
                                         :valinnanvaihe [{
                                                          :valintatapajonot [{
                                                                              :jonosijat [{
                                                                                           :jarjestyskriteerit [
                                                                                                                {
                                                                                                                 :arvo 19
                                                                                                                 :tila "HYVAKSYTTAVISSA"
                                                                                                                 :kuvaus {}
                                                                                                                 :prioriteetti 0
                                                                                                                 :nimi "2. asteen peruskoulupohjainen peruskaava + Kielitaidon riittävyys - 2 aste, pk ja yo 2021"
                                                                                                                 }
                                                                                                                {
                                                                                                                 :arvo 0
                                                                                                                 :tila "HYVAKSYTTAVISSA"
                                                                                                                 :kuvaus {}
                                                                                                                 :prioriteetti 1
                                                                                                                 :nimi "Hakutoivejärjestyspisteytys, 2 aste, pk ja yo 2016"
                                                                                                                 }
                                                                                                                {
                                                                                                                 :arvo 8.5556
                                                                                                                 :tila "HYVAKSYTTAVISSA"
                                                                                                                 :kuvaus {}
                                                                                                                 :prioriteetti 2
                                                                                                                 :nimi "Kaikkien aineiden keskiarvo, PK 2019"
                                                                                                                 },
                                                                                                                {
                                                                                                                 :arvo 8.6667
                                                                                                                 :tila "HYVAKSYTTAVISSA"
                                                                                                                 :kuvaus {}
                                                                                                                 :prioriteetti 3
                                                                                                                 :nimi "Painotettavien arvosanojen keskiarvo 2016"
                                                                                                                 }]
                                                                                           }]
                                                                              }]
                                                          }]
                                         }]
                          })

(def mocked-vts-response {})

(def mocked-vts-service {:valinnan-tulos-hakemukselle (constantly mocked-vts-response)})

(describe "valintalaskentaservice spec"
          (tags :unit)
          (it "Return valinnat for application"
              (with-redefs [http/request (constantly (json/generate-string mocked-vls-response))]
                (let [cas-client (cas/new-cas-client "ataru-test")
                      cas (cas/map->CasClientState {:client              cas-client
                                                    :params              nil
                                                    :session-cookie-name "ring-session"
                                                    :session-id          (atom "fake-session")})
                      vls-service-instance (vls-service/->RemoteValintaLaskentaService cas mocked-vts-service)
                      result (vls/hakemuksen-tulokset vls-service-instance "1.2.3.4" "1.2.3.4.5.6")]
                  (should= nil result)))))