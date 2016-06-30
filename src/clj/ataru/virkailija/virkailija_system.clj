(ns ataru.virkailija.virkailija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.db.migrations :as migrations]
            [ataru.http.server :as server]
            [ataru.person-service.client :as person-service]
            [ataru.virkailija.virkailija-routes :refer [clerk-routes]]))

(defn new-system
  ([http-port]
   (component/system-map
     :server-setup {:routes clerk-routes :port http-port :repl-port 3333}
     :migration (migrations/new-migration)
     :person-service (person-service/new-client)
     :server (component/using (server/new-server) [:server-setup])))
  ([]
   (new-system 8350)))
