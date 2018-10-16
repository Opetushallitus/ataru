(ns ataru.virkailija.virkailija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.http.server :as server]
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
            [ataru.person-service.person-service :as person-service]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service]))

(defn new-system
  ([]
   (new-system
    (Integer/parseInt (get env :ataru-http-port "8350"))
    (Integer/parseInt (get env :ataru-repl-port "3333"))))
  ([http-port repl-port]
   (apply
    component/system-map

    :organization-service (organization-service/new-organization-service)

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

    :kayttooikeus-service (if (-> config :dev :fake-dependencies)
                            (kayttooikeus-service/->FakeKayttooikeusService)
                            (kayttooikeus-service/->HttpKayttooikeusService nil))

    :person-service (component/using
                     (person-service/new-person-service)
                     [:henkilo-cache])

    :handler (component/using
              (virkailija-routes/new-handler)
              (vec (concat [:organization-service
                            :virkailija-tarjonta-service
                            :tarjonta-service
                            :job-runner
                            :ohjausparametrit-service
                            :person-service
                            :kayttooikeus-service]
                           (map #(keyword (str (:name %) "-cache")) caches))))

    :server-setup {:port      http-port
                   :repl-port repl-port}

    :server (component/using
             (server/new-server)
             [:server-setup :handler])

    :job-runner (job/new-job-runner virkailija-jobs/job-definitions)

    :redis (redis/map->Redis {})

    (mapcat (fn [cache]
              [(keyword (str (:name cache) "-cache"))
               (component/using cache [:redis])])
            caches))))
