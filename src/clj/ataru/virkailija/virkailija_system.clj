(ns ataru.virkailija.virkailija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.cas.client :as cas]
            [ataru.codes-service.postal-code-client :as postal-code-client]
            [ataru.db.migrations :as migrations]
            [ataru.http.server :as server]
            [ataru.virkailija.user.organization-service :as organization-service]
            [ataru.virkailija.virkailija-routes :as handler]
            [environ.core :refer [env]]))

(defn new-system
  ([]
   (new-system
     (Integer/parseInt (get env :ataru-http-port "8350"))
     (Integer/parseInt (get env :ataru-repl-port "3333"))))
  ([http-port repl-port]
   (component/system-map
     :handler        (component/using
                       (handler/new-handler)
                       [:postal-code-client])

     :server-setup   {:port http-port
                      :repl-port repl-port}

     :migration      (migrations/new-migration)

     :postal-code-client (postal-code-client/new-postal-code-client)

     :organization-service (organization-service/new-organization-service)

     :server         (component/using
                       (server/new-server)
                       [:server-setup :handler]))))
