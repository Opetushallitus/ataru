(ns ataru.hakija.hakija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.hakija.hakija-routes :as handler]
            [ataru.background-job.job :as job]
            [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
            [ataru.http.server :as server]
            [ataru.person-service.person-service :as person-service]
            [environ.core :refer [env]]
            [ataru.cache.caches :refer [caches]]
            [ataru.redis :as redis]
            [ataru.config.core :refer [config]]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [ataru.temp-file-storage.s3-client :as s3-client]
            [ataru.temp-file-storage.filesystem-temp-file-store :as filesystem-temp-file-store]
            [ataru.temp-file-storage.s3-temp-file-store :as s3-temp-file-store]))

(defn new-system
  ([]
   (new-system
     (Integer/parseInt (get env :ataru-http-port "8351"))
     (Integer/parseInt (get env :ataru-repl-port "3335"))))
  ([http-port repl-port]
   (apply
     component/system-map
     :cache-service (component/using {} (mapv (comp keyword :name) caches))

     :tarjonta-service (component/using
                         (tarjonta-service/new-tarjonta-service)
                         [:cache-service])

     :organization-service (component/using
                             (organization-service/new-organization-service)
                             [:cache-service])

     :ohjausparametrit-service (component/using
                                 (ohjausparametrit-service/new-ohjausparametrit-service)
                                 [:cache-service])

     :person-service (component/using
                       (person-service/new-person-service)
                       [:cache-service])

     :suoritus-service (suoritus-service/new-suoritus-service)

     :temp-file-store (filesystem-temp-file-store/new-store)

     :handler (component/using
                (handler/new-handler)
                [:tarjonta-service
                 :job-runner
                 :organization-service
                 :ohjausparametrit-service
                 :person-service
                 :temp-file-store])

     :server-setup {:port      http-port
                    :repl-port repl-port}

     :server (component/using
               (server/new-server)
               [:server-setup :handler])

     :job-runner (component/using
                   (job/new-job-runner hakija-jobs/job-definitions)
                   [:ohjausparametrit-service
                    :person-service
                    :tarjonta-service
                    :suoritus-service])

     :redis (redis/map->Redis {})

     ;(if (get-in config [:temp-files :s3])
     ;  [:s3-client (s3-client/new-client)
     ;   :temp-file-store (component/using
     ;                      (s3-temp-file-store/new-store)
     ;                      [:s3-client])]
     ;  [:temp-file-store (filesystem-temp-file-store/new-store)])



     (mapcat (fn [cache]
               [(keyword (:name cache)) (component/using cache [:redis])])
             caches)

)))
