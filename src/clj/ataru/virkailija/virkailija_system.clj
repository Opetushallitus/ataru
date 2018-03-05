(ns ataru.virkailija.virkailija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.http.server :as server]
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
   (apply component/system-map

     :organization-service (component/using
                             (organization-service/new-organization-service)
                             [:cache-service])

     :cache-service (component/using {} (mapv (comp keyword :name) caches))

     :virkailija-tarjonta-service (component/using
                                    (tarjonta-service/new-virkailija-tarjonta-service)
                                    [:organization-service :cache-service])

     :tarjonta-service (component/using
                         (tarjonta-service/new-tarjonta-service)
                         [:cache-service])

     :ohjausparametrit-service (component/using
                                 (ohjausparametrit-service/new-ohjausparametrit-service)
                                 [:cache-service])

     :person-service (person-service/new-person-service)

     :handler (component/using
                (virkailija-routes/new-handler)
                [:organization-service
                 :virkailija-tarjonta-service
                 :tarjonta-service
                 :ohjausparametrit-service
                 :cache-service
                 :person-service])

     :server-setup {:port      http-port
                    :repl-port repl-port}

     :server (component/using
               (server/new-server)
               [:server-setup :handler])

     :job-runner (job/new-job-runner virkailija-jobs/job-definitions)

     :redis (redis/map->Redis {})

     (mapcat (fn [cache]
               [(keyword (:name cache)) (component/using cache [:redis])])
             caches))))
