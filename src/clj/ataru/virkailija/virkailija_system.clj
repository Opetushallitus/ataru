(ns ataru.virkailija.virkailija-system
  (:require [ataru.siirtotiedosto-service :as siirtotiedosto-service]
            [ataru.config.url-helper :refer [resolve-url]]
            [com.stuartsierra.component :as component]
            [ataru.aws.auth :as aws-auth]
            [ataru.aws.sqs :as sqs]
            [ataru.aws.cloudwatch :as cloudwatch]
            [ataru.cas.client :as cas]
            [ataru.cache.redis-cache :as redis-cache]
            [ataru.cache.two-layer-cache :as two-layer-cache]
            [ataru.http.server :as server]
            [ataru.hakija.hakija-form-service :as hakija-form-service]
            [ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu-service :as hakukohderyhma-service]
            [ataru.kayttooikeus-service.kayttooikeus-service :as kayttooikeus-service]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-service :as koostepalvelu-service]
            [ataru.valintaperusteet.client :as valintaperusteet-client]
            [ataru.valintaperusteet.service :as valintaperusteet-service]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.valinta-tulos-service.valintatulosservice-service :as valinta-tulos-service]
            [ataru.valinta-laskenta-service.valintalaskentaservice-service :as valinta-laskenta-service]
            [ataru.virkailija.virkailija-routes :as virkailija-routes]
            [ataru.cache.caches :refer [caches]]
            [ataru.redis :as redis]
            [environ.core :refer [env]]
            [ataru.config.core :refer [config]]
            [ataru.background-job.job :as job]
            [ataru.temp-file-storage.s3-temp-file-store :as s3-temp-file-store]
            [ataru.temp-file-storage.filesystem-temp-file-store :as filesystem-temp-file-store]
            [ataru.virkailija.background-jobs.virkailija-jobs :as virkailija-jobs]
            [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
            [ataru.maksut.maksut-service :as maksut-service]
            [ataru.background-job.maksut-poller :as maksut-poller]
            [ataru.person-service.person-client :as person-client]
            [ataru.person-service.person-service :as person-service]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service]
            [ataru.applications.application-service :as application-service]
            [ataru.koski.koski-service :as koski-service]
            [clj-ring-db-session.session.session-store :refer [create-session-store]]
            [ataru.db.db :as db]
            [ataru.temp-file-storage.s3-client :as s3-client]
            [ataru.attachment-deadline.attachment-deadline-service :as attachment-deadline-service]
            [taoensso.timbre :as log])
  (:import java.time.Duration
           [fi.oph.viestinvalitys ClientBuilder]
           [fi.vm.sade.valinta.dokumenttipalvelu SiirtotiedostoPalvelu]
           [java.util.concurrent TimeUnit]))

(defn new-system
  ([audit-logger]
   (new-system
     audit-logger
    (Integer/parseInt (get env :ataru-http-port "8350"))
    (Integer/parseInt (get env :ataru-repl-port "3333"))))
  ([audit-logger http-port repl-port]
   (apply
    component/system-map

    :audit-logger audit-logger

    :viestinvalityspalvelu-client (-> (ClientBuilder/viestinvalitysClientBuilder)
                                      (.withEndpoint (resolve-url :viestinvalityspalvelu-endpoint))
                                      (.withUsername (-> config :cas :username))
                                      (.withPassword (-> config :cas :password))
                                      (.withCasEndpoint (resolve-url :cas-client))
                                      (.withCallerId (-> config :public-config :virkailija-caller-id))
                                      (.build))

    :hakukohderyhmapalvelu-cas-client (cas/new-client "/hakukohderyhmapalvelu"
                                                      "/auth/cas"
                                                      "ring-session"
                                                      (-> config :public-config :virkailija-caller-id))

    :hakukohderyhmapalvelu-service (component/using
                                     (hakukohderyhma-service/new-hakukohderyhmapalvelu-service)
                                     [:hakukohderyhmapalvelu-cas-client])

    :organization-service (component/using
                           (organization-service/new-organization-service)
                           [:organizations-hierarchy-cache
                            :all-organization-groups-cache])

    :tarjonta-service (component/using
                       (tarjonta-service/new-tarjonta-service)
                       [:organization-service
                        :forms-in-use-cache
                        :koulutus-cache
                        :kouta-hakus-by-form-key-cache
                        :hakukohde-cache
                        :haku-cache
                        :hakukohde-search-cache
                        :kouta-internal-cas-client])

    :valintalaskentakoostepalvelu-hakukohde-valintalaskenta-redis-cache
    (component/using
      (redis-cache/map->Cache
        {:name          "valintalaskentakoostepalvelu-hakukohde-valintalaskenta"
         :ttl           [(get-in config [:cache :ttl-amounts :valintalaskentakoostepalvelu-hakukohde-valintalaskenta] 3) TimeUnit/DAYS]
         :refresh-after [1 TimeUnit/DAYS]
         :lock-timeout  [10000 TimeUnit/MILLISECONDS]})
      {:redis  :redis
       :loader :valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache-loader})

    :valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache
    (component/using
      (two-layer-cache/map->Cache
        {:name                "in-memory-valintalaskentakoostepalvelu-hakukohde-valintalaskenta"
         :size                200000
         :expire-after-access [(get-in config [:cache :ttl-amounts :in-memory-valintalaskentakoostepalvelu-hakukohde-valintalaskenta] 3) TimeUnit/DAYS]
         :refresh-after       [1 TimeUnit/SECONDS]})
      {:redis-cache :valintalaskentakoostepalvelu-hakukohde-valintalaskenta-redis-cache})

    :valintalaskentakoostepalvelu-hakukohde-harkinnanvaraisuus-cache
    (component/using
      (redis-cache/map->Cache
        {:name          "valintalaskentakoostepalvelu-hakukohde-harkinnanvaraisuus"
         :ttl           [(get-in config [:cache :ttl-amounts :valintalaskentakoostepalvelu-hakukohde-harkinnanvaraisuus] 2) TimeUnit/DAYS]
         :refresh-after [2 TimeUnit/HOURS]
         :lock-timeout  [1 TimeUnit/MINUTES]})
      {:redis  :redis
       :loader :valintalaskentakoostepalvelu-hakukohde-harkinnanvaraisuus-cache-loader})

    :valinta-tulos-service-cas-client (cas/new-client "/valinta-tulos-service" "/auth/login"
                                                      "session" (-> config :public-config :virkailija-caller-id))

    :valinta-tulos-service (component/using
                            (valinta-tulos-service/map->RemoteValintaTulosService {})
                            {:cas-client :valinta-tulos-service-cas-client})

    :valinta-laskenta-service-cas-client (cas/new-client "/valintalaskenta-laskenta-service"
                                                         "/j_spring_cas_security_check"
                                                         "JSESSIONID"
                                                         (-> config :public-config :virkailija-caller-id))

    :valinta-laskenta-service (component/using
                                (valinta-laskenta-service/map->RemoteValintaLaskentaService {})
                                {:cas-client :valinta-laskenta-service-cas-client
                                 :valinta-tulos-service :valinta-tulos-service})

    :valintalaskentakoostepalvelu-cas-client (cas/new-client "/valintalaskentakoostepalvelu"
                                                             "/j_spring_cas_security_check"
                                                             "JSESSIONID"
                                                             (-> config :public-config :virkailija-caller-id))

    :valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache-loader (component/using
                                                                           (koostepalvelu-service/map->HakukohdeValintalaskentaCacheLoader {})
                                                                           [:valintalaskentakoostepalvelu-cas-client])

    :valintalaskentakoostepalvelu-hakukohde-harkinnanvaraisuus-cache-loader (component/using
                                                                              (koostepalvelu-service/map->HakukohdeHarkinnanvaraisuusCacheLoader {})
                                                                              [:valintalaskentakoostepalvelu-cas-client
                                                                               :hakukohde-cache])

    :valintalaskentakoostepalvelu-service (component/using
                                            (koostepalvelu-service/new-valintalaskentakoostepalvelu-service)
                                            [:valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache
                                             :valintalaskentakoostepalvelu-hakukohde-harkinnanvaraisuus-cache
                                             :valintalaskentakoostepalvelu-cas-client
                                             :hakukohde-cache])

    :valintaperusteet-cas-client (cas/new-client "/valintaperusteet-service" "/j_spring_cas_security_check"
                                                 "JSESSIONID" (-> config :public-config :virkailija-caller-id))

    :valintatapajono-cache-loader (component/using
                                   (valintaperusteet-client/map->ValintatapajonoCacheLoader {})
                                   [:valintaperusteet-cas-client])

    :valintatapajono-redis-cache (component/using
                                  (redis-cache/map->Cache
                                   {:name          "valintatapajono"
                                    :ttl           [(get-in config [:cache :ttl-amounts :valintatapajono] 3) TimeUnit/DAYS]
                                    :refresh-after [1 TimeUnit/DAYS]
                                    :lock-timeout  [1 TimeUnit/SECONDS]})
                                  {:redis  :redis
                                   :loader :valintatapajono-cache-loader})

    :valintatapajono-cache (component/using
                            (two-layer-cache/map->Cache
                             {:name                "in-memory-valintatapajono"
                              :size                10000
                              :expire-after-access [(get-in config [:cache :ttl-amounts :in-memory-valintatapajono] 3) TimeUnit/DAYS]
                              :refresh-after       [12 TimeUnit/HOURS]})
                            {:redis-cache :valintatapajono-redis-cache})

    :valintaperusteet-service (component/using
                               (valintaperusteet-service/map->CachedValintaperusteetService {})
                               [:valintatapajono-cache])

    :ohjausparametrit-service (component/using
                               (ohjausparametrit-service/new-ohjausparametrit-service)
                               [:ohjausparametrit-cache])

    :kayttooikeus-cas-client (cas/new-client "/kayttooikeus-service" "/j_spring_cas_security_check"
                                             "JSESSIONID" (-> config :public-config :virkailija-caller-id))

    :kayttooikeus-service (if (-> config :dev :fake-dependencies)
                            (kayttooikeus-service/->FakeKayttooikeusService)
                            (component/using
                             (kayttooikeus-service/->HttpKayttooikeusService nil)
                             [:kayttooikeus-cas-client]))

    :maksut-cas-client (cas/new-client "/maksut"
                        "/auth/cas"
                        "ring-session"
                        (-> config :public-config :virkailija-caller-id))

    :maksut-service (component/using
                     (maksut-service/new-maksut-service)
                     [:maksut-cas-client])

    :maksut-poller (component/using
                    (maksut-poller/map->MaksutPollWorker
                      {:enabled? (-> config :tutkintojen-tunnustaminen :maksut :enabled? boolean)})
                    [:job-runner
                     :application-service
                     :maksut-service])

    :attachment-deadline-service (component/using
                                  (attachment-deadline-service/map->AttachmentDeadlineService {})
                                  [:ohjausparametrit-service])

    :oppijanumerorekisteri-cas-client (cas/new-client "/oppijanumerorekisteri-service" "/j_spring_cas_security_check"
                                                      "JSESSIONID" (-> config :public-config :virkailija-caller-id))

    :henkilo-cache-loader (component/using
                           (person-client/map->PersonCacheLoader {})
                           [:oppijanumerorekisteri-cas-client])

    :form-by-haku-oid-str-cache-loader (component/using
                                        (hakija-form-service/map->FormByHakuOidStrCacheLoader {})
                                        [:form-by-id-cache
                                         :koodisto-cache
                                         :ohjausparametrit-service
                                         :organization-service
                                         :tarjonta-service
                                         :hakukohderyhma-settings-cache
                                         :attachment-deadline-service])

    :person-service (component/using
                     (person-service/new-person-service)
                     [:henkilo-cache :oppijanumerorekisteri-cas-client])

    :login-cas-client (let [client (cas/new-client "/lomake-editori" "/auth/cas" "ring-session" (-> config :public-config :virkailija-caller-id))]
                        (log/debug "Initialized login-cas-client:" client)
                        client)

    :liiteri-cas-client (cas/new-client "/liiteri" "/auth/cas"
                                        "ring-session" (-> config :public-config :virkailija-caller-id))

    :tutu-cas-client (cas/new-client "/tutu-backend/api" "/j_spring_cas_security_check" "JSESSIONID"
                                     (-> config :public-config :virkailija-caller-id))

    :siirtotiedosto-client (new SiirtotiedostoPalvelu
                                (-> config :siirtotiedostot :aws-region)
                                (-> config :siirtotiedostot :s3-bucket)
                                (-> config :siirtotiedostot :transferFileTargetRoleArn))

    :koski-client (cas/new-client "/koski" "/cas/virkailija" "koskiUser"
                                  "1.2.246.562.10.00000000001.ataru-hakija.frontend")
    :koski-service (component/using
                     (koski-service/map->IntegratedKoskiTutkintoService {})
                     {:koski-cas-client :koski-client})

    :application-service (component/using
                           (application-service/new-application-service)
                           [:liiteri-cas-client
                            :organization-service
                            :tarjonta-service
                            :ohjausparametrit-service
                            :audit-logger
                            :person-service
                            :valinta-tulos-service
                            :koodisto-cache
                            :job-runner
                            :suoritus-service
                            :form-by-id-cache
                            :valintalaskentakoostepalvelu-service
                            :koski-service])

    :siirtotiedosto-service (component/using
                              (siirtotiedosto-service/new-siirtotiedosto-service)
                              [:siirtotiedosto-client])

    :session-store (create-session-store (db/get-datasource :db))

    :handler (component/using
              (virkailija-routes/new-handler)
              (vec (concat [:login-cas-client
                            :organization-service
                            :tarjonta-service
                            :liiteri-cas-client
                            :valintalaskentakoostepalvelu-service
                            :valintaperusteet-service
                            :valinta-tulos-service
                            :valinta-laskenta-service
                            :job-runner
                            :maksut-service
                            :ohjausparametrit-service
                            :person-service
                            :kayttooikeus-service
                            :temp-file-store
                            :audit-logger
                            :application-service
                            :siirtotiedosto-service
                            :session-store
                            :suoritus-service
                            :attachment-deadline-service]
                           (map first caches))))

    :server-setup {:port      http-port
                   :repl-port repl-port}

    :server (component/using
             (server/new-server)
             [:server-setup :handler])

    :suoritus-service (component/using
                       (suoritus-service/new-suoritus-service)
                       [:suoritusrekisteri-cas-client
                        :oppilaitoksen-opiskelijat-cache
                        :oppilaitoksen-luokat-cache
                        :lahtokoulut-cache
                        :ohjausparametrit-service
                        :tarjonta-service])

    :job-runner (component/using
                 (job/new-job-runner (merge virkailija-jobs/job-definitions
                                            hakija-jobs/job-definitions)
                                     (db/get-datasource :db)
                                     (boolean (get-in config [:jobs :enabled] true)))
                 [:form-by-id-cache
                  :ohjausparametrit-service
                  :organization-service
                  :henkilo-cache
                  :koodisto-cache
                  :haku-cache
                  :get-haut-cache
                  :person-service
                  :tarjonta-service
                  :suoritus-service
                  :hakukohderyhmapalvelu-service
                  :hakukohderyhma-settings-cache
                  :valintalaskentakoostepalvelu-service
                  :audit-logger
                  :liiteri-cas-client
                  :amazon-cloudwatch
                  :maksut-service
                  :viestinvalityspalvelu-client
                  :tutu-cas-client
                  :attachment-deadline-service])

    :credentials-provider (aws-auth/map->CredentialsProvider {})


    :s3-client (component/using
                 (s3-client/new-client)
                 [:credentials-provider])

    :temp-file-store (if (get-in config [:aws :liiteri-files])
                       (component/using
                         (s3-temp-file-store/new-store)
                         [:s3-client])
                       (filesystem-temp-file-store/new-store))

    :amazon-cloudwatch (component/using
                  (cloudwatch/map->AmazonCloudwatch {:namespace (str (-> config :public-config :environment-name) "-ataru")})
                  [:credentials-provider])

    :amazon-sqs (component/using
                 (sqs/map->AmazonSQS {})
                 [:credentials-provider])

    :update-person-info-worker (component/using
                                (person-integration/map->UpdatePersonInfoWorker
                                 {:enabled?      (:enabled? (:henkilo-modified-queue (:aws config)))
                                  :drain-failed? (:drain-failed? (:henkilo-modified-queue (:aws config)))
                                  :queue-url     (:queue-url (:henkilo-modified-queue (:aws config)))
                                  :receive-wait  (Duration/ofSeconds 20)})
                                [:amazon-sqs
                                 :job-runner])

    :redis (redis/map->Redis {})

    (mapcat identity caches))))
