(ns ataru.virkailija.virkailija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.db.migrations :as migrations]
            [ataru.http.server :as server]
            [ataru.virkailija.user.organization-service :as organization-service]
            [ataru.virkailija.virkailija-routes :as virkailija-routes]
            [ataru.cache.cache-service :as cache-service]
            [environ.core :refer [env]]))

(defn new-system
  ([]
   (new-system
     (Integer/parseInt (get env :ataru-http-port "8350"))
     (Integer/parseInt (get env :ataru-repl-port "3333"))))
  ([http-port repl-port]
   (component/system-map

    :handler              (component/using
                           (virkailija-routes/new-handler)
                           [:organization-service])

    :server-setup         {:port      http-port
                           :repl-port repl-port}

    :organization-service (organization-service/new-organization-service)

    :server               (component/using
                           (server/new-server)
                           [:server-setup :handler])
    :cache                (cache-service/new-cache-service))))
