(ns ataru.virkailija.virkailija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.http.server :as server]
            [ataru.virkailija.user.organization-service :as organization-service]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.virkailija.virkailija-routes :as virkailija-routes]
            [ataru.cache.caches :refer [hazelcast-caches caches]]
            [ataru.cache.hazelcast :refer [map->HazelcastInstance]]
            [environ.core :refer [env]]))

(defn new-system
  ([]
   (new-system
     (Integer/parseInt (get env :ataru-http-port "8350"))
     (Integer/parseInt (get env :ataru-repl-port "3333"))))
  ([http-port repl-port]
   (apply component/system-map

     :organization-service (organization-service/new-organization-service)

     :hazelcast (map->HazelcastInstance {:configurators hazelcast-caches})

     :cache-service (component/using
                     {}
                     (mapv (comp keyword :name) caches))

     :virkailija-tarjonta-service (component/using
                                    (tarjonta-service/new-virkailija-tarjonta-service)
                                    [:organization-service])

     :tarjonta-service (component/using
                         (tarjonta-service/new-tarjonta-service)
                         [:cache-service])

     :handler (component/using
                (virkailija-routes/new-handler)
                [:organization-service :virkailija-tarjonta-service :tarjonta-service :cache-service])

     :server-setup {:port      http-port
                    :repl-port repl-port}

     :server (component/using
               (server/new-server)
               [:server-setup :handler])

     (mapcat (fn [cache]
               [(keyword (:name cache)) (component/using cache [:hazelcast])])
             hazelcast-caches))))
