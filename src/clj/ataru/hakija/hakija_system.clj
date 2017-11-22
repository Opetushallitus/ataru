(ns ataru.hakija.hakija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.hakija.hakija-routes :as handler]
            [ataru.background-job.job :as job]
            [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
            [ataru.http.server :as server]
            [ataru.person-service.person-service :as person-service]
            [environ.core :refer [env]]
            [ataru.cache.caches :refer [hazelcast-caches redis-caches]]
            [ataru.redis :as redis]
            [ataru.config.core :refer [config]]
            [ataru.cache.hazelcast :refer [map->HazelcastInstance]]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]))

(defn new-system
  ([]
   (new-system
     (Integer/parseInt (get env :ataru-http-port "8351"))
     (Integer/parseInt (get env :ataru-repl-port "3335"))))
  ([http-port repl-port]
   (apply component/system-map
     :cache-service (component/using
                     {}
                     (mapv (comp keyword :name)
                           (if (= :redis (-> config :cache :type))
                             redis-caches
                             hazelcast-caches)))

     :tarjonta-service (component/using
                         (tarjonta-service/new-tarjonta-service)
                         [:cache-service])

     :handler              (component/using
                             (handler/new-handler)
                             [:tarjonta-service])

     :server-setup         {:port      http-port
                            :repl-port repl-port}

     :server               (component/using
                             (server/new-server)
                             [:server-setup :handler])

     :person-service       (person-service/new-person-service)

     :job-runner           (component/using
                             (job/new-job-runner hakija-jobs/job-definitions)
                             [:person-service])

     (if (= :redis (-> config :cache :type))
       (concat [:redis (redis/map->Redis {})]
               (mapcat (fn [cache]
                         [(keyword (:name cache)) (component/using cache [:redis])])
                       redis-caches))
       (concat [:hazelcast (map->HazelcastInstance {:configurators hazelcast-caches})]
               (mapcat (fn [cache]
                         [(keyword (:name cache)) (component/using cache [:hazelcast])])
                       hazelcast-caches))))))
