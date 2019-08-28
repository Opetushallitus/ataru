(ns ataru.kayttooikeus-service.kayttooikeus-service
  (:require [ataru.cas.client :as cas]
            [ataru.config.url-helper :as url]
            [cheshire.core :as json]
            [com.stuartsierra.component :as component]))

(defprotocol KayttooikeusService
  (virkailija-by-username [this username])
  (virkailija-by-oid [this oid]))

(defrecord HttpKayttooikeusService [kayttooikeus-cas-client]
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  KayttooikeusService
  (virkailija-by-username [_ username]
    (let [url                   (url/resolve-url :kayttooikeus-service.kayttooikeus.kayttaja
                                                 {"username" username})
          {:keys [status body]} (cas/cas-authenticated-get kayttooikeus-cas-client url)]
      (if (= 200 status)
        (if-let [virkailija (first (json/parse-string body true))]
          virkailija
          (throw (new RuntimeException
                      (str "No virkailija found by username " username))))
        (throw (new RuntimeException
                    (str "Could not get virkailija by username " username
                         ", status: " status
                         ", body: " body))))))
  (virkailija-by-oid [_ oid]
    (let [url                   (url/resolve-url :kayttooikeus-service.kayttooikeus.kayttaja
                                                 {"oidHenkilo" oid})
          {:keys [status body]} (cas/cas-authenticated-get kayttooikeus-cas-client url)]
      (if (= 200 status)
        (if-let [virkailija (first (json/parse-string body true))]
          virkailija
          (throw (new RuntimeException
                      (str "No virkailija found by oid " oid))))
        (throw (new RuntimeException
                    (str "Could not get virkailija by oid " oid
                         ", status: " status
                         ", body: " body)))))))

(def fake-virkailija-value
  {"1.2.246.562.11.11111111111"
   {:oidHenkilo    "1.2.246.562.11.11111111012"
    :organisaatiot [{:organisaatioOid "1.2.246.562.10.0439845"
                     :kayttooikeudet  [{:palvelu "ATARU_EDITORI"
                                        :oikeus  "CRUD"}
                                       {:palvelu "ATARU_HAKEMUS"
                                        :oikeus  "CRUD"}]}
                    {:organisaatioOid "1.2.246.562.28.1"
                     :kayttooikeudet  [{:palvelu "ATARU_EDITORI"
                                        :oikeus  "CRUD"}
                                       {:palvelu "ATARU_HAKEMUS"
                                        :oikeus  "CRUD"}]}]}
   "1.2.246.562.11.22222222222"
   {:oidHenkilo    "1.2.246.562.11.11111111000"
    :organisaatiot [{:organisaatioOid "1.2.246.562.10.0439846"
                     :kayttooikeudet  [{:palvelu "ATARU_EDITORI"
                                        :oikeus  "CRUD"}
                                       {:palvelu "ATARU_HAKEMUS"
                                        :oikeus  "CRUD"}]}
                    {:organisaatioOid "1.2.246.562.28.2"
                     :kayttooikeudet  [{:palvelu "ATARU_EDITORI"
                                        :oikeus  "CRUD"}
                                       {:palvelu "ATARU_HAKEMUS"
                                        :oikeus  "CRUD"}]}]}
   "1.2.246.562.11.11111111013"
   {:oidHenkilo    "1.2.246.562.11.11111111013"
    :organisaatiot [{:organisaatioOid "1.2.246.562.10.0439846"
                     :kayttooikeudet  [{:palvelu "ATARU_EDITORI"
                                        :oikeus  "CRUD"}
                                       {:palvelu "ATARU_HAKEMUS"
                                        :oikeus  "CRUD"}
                                       {:palvelu "ATARU_HAKEMUS"
                                        :oikeus  "ARKALUONTOINEN_READ"}
                                       {:palvelu "ATARU_HAKEMUS"
                                        :oikeus  "ARKALUONTOINEN_UPDATE"}]}]}})

(defrecord FakeKayttooikeusService []
  KayttooikeusService
  (virkailija-by-username [_ username]
    (get fake-virkailija-value username (get fake-virkailija-value "1.2.246.562.11.11111111111")))
  (virkailija-by-oid [_ oid]
    (get fake-virkailija-value oid)))
