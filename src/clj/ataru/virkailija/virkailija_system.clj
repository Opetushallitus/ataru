(ns ataru.virkailija.virkailija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.db.migrations :as migrations]
            [ataru.http.server :as server]
            [ataru.virkailija.virkailija-routes :refer [virkailija-routes]]))

(defn new-system
  []
  (component/system-map
    :server-setup {:routes virkailija-routes :port 8350 :repl-port 3333}
    :migration (migrations/new-migration)
    :server (component/using (server/new-server) [:server-setup])))
