(ns ataru.virkailija.virkailija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.aws.auth :as aws-auth]
            [ataru.aws.sns :as sns]
            [ataru.aws.sqs :as sqs]
            [ataru.cas.client :as cas]
            [ataru.http.server :as server]
            [ataru.hakija.hakija-form-service :as hakija-form-service]
            [ataru.kayttooikeus-service.kayttooikeus-service :as kayttooikeus-service]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.virkailija.virkailija-routes :as virkailija-routes]
            [ataru.cache.caches :refer [caches]]
            [ataru.redis :as redis]
            [environ.core :refer [env]]
            [ataru.config.core :refer [config]]
            [ataru.background-job.job :as job]
            [ataru.virkailija.background-jobs.virkailija-jobs :as virkailija-jobs]
            [ataru.person-service.person-client :as person-client]
            [ataru.person-service.person-service :as person-service]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service])
  (:import java.time.Duration))

(defn new-system
  ([]
   (new-system
    (Integer/parseInt (get env :ataru-http-port "8350"))
    (Integer/parseInt (get env :ataru-repl-port "3333"))))
  ([http-port repl-port]
   (apply
    component/system-map

    :organization-service (component/using
                           (organization-service/new-organization-service)
                           [:all-organization-groups-cache])

    :virkailija-tarjonta-service (component/using
                                  (tarjonta-service/new-virkailija-tarjonta-service)
                                  [:forms-in-use-cache :organization-service])

    :tarjonta-service (component/using
                       (tarjonta-service/new-tarjonta-service)
                       [:koulutus-cache
                        :hakukohde-cache
                        :haku-cache
                        :hakukohde-search-cache])

    :ohjausparametrit-service (component/using
                               (ohjausparametrit-service/new-ohjausparametrit-service)
                               [:ohjausparametrit-cache])

    :kayttooikeus-cas-client (cas/new-client "/kayttooikeus-service" "j_spring_cas_security_check" "JSESSIONID")

    :kayttooikeus-service (if (-> config :dev :fake-dependencies)
                            (kayttooikeus-service/->FakeKayttooikeusService)
                            (component/using
                             (kayttooikeus-service/->HttpKayttooikeusService nil)
                             [:kayttooikeus-cas-client]))

    :oppijanumerorekisteri-cas-client (cas/new-client "/oppijanumerorekisteri-service" "j_spring_cas_security_check" "JSESSIONID")

    :henkilo-cache-loader (component/using
                           (person-client/map->PersonCacheLoader {})
                           [:oppijanumerorekisteri-cas-client])

    :form-by-haku-oid-and-id-cache-loader (component/using
                                           (hakija-form-service/map->FormByHakuOidAndIdCacheLoader {})
                                           [:tarjonta-service
                                            :koodisto-cache
                                            :organization-service
                                            :ohjausparametrit-service])

    :form-by-haku-oid-str-cache-loader (component/using
                                        (hakija-form-service/map->FormByHakuOidStrCacheLoader {})
                                        [:tarjonta-service
                                         :form-by-haku-oid-and-id-cache])

    :person-service (component/using
                     (person-service/new-person-service)
                     [:henkilo-cache :oppijanumerorekisteri-cas-client])

    :login-cas-client (cas/new-cas-client)

    :handler (component/using
              (virkailija-routes/new-handler)
              (vec (concat [:login-cas-client
                            :organization-service
                            :virkailija-tarjonta-service
                            :tarjonta-service
                            :job-runner
                            :ohjausparametrit-service
                            :person-service
                            :kayttooikeus-service]
                           (map first caches))))

    :server-setup {:port      http-port
                   :repl-port repl-port}

    :server (component/using
             (server/new-server)
             [:server-setup :handler])

    :suoritusrekisteri-cas-client (cas/new-client "/suoritusrekisteri" "j_spring_cas_security_check" "JSESSIONID")

    :suoritus-service (component/using
                       (suoritus-service/new-suoritus-service)
                       [:suoritusrekisteri-cas-client])

    :job-runner (component/using
                 (job/new-job-runner virkailija-jobs/job-definitions)
                 [:ohjausparametrit-service
                  :henkilo-cache
                  :koodisto-cache
                  :person-service
                  :tarjonta-service
                  :suoritus-service])

    :credentials-provider (aws-auth/map->CredentialsProvider {})

    :amazon-sqs (component/using
                 (sqs/map->AmazonSQS {})
                 [:credentials-provider])

    :sns-message-manager (sns/map->SNSMessageManager {})

    :update-person-info-worker (component/using
                                (person-integration/map->UpdatePersonInfoWorker
                                 {:enabled?      (:enabled? (:henkilo-modified-queue (:aws config)))
                                  :drain-failed? (:drain-failed? (:henkilo-modified-queue (:aws config)))
                                  :queue-url     (:queue-url (:henkilo-modified-queue (:aws config)))
                                  :receive-wait  (Duration/ofSeconds 20)})
                                [:amazon-sqs
                                 :job-runner
                                 :sns-message-manager])

    :redis (redis/map->Redis {})

    (mapcat identity caches))))
