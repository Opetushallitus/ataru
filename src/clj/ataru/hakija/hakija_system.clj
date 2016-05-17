(ns ataru.hakija.hakija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.db.migrations :as migrations]
            [ataru.hakija.hakija-routes :refer [hakija-routes]]
            [ataru.http.server :as server]))

(defn new-system
  []
  (component/system-map
    :server-setup {:routes hakija-routes :port 8351 :repl-port 3335}
    :migration (migrations/new-migration)
    :server (component/using (server/new-server) [:server-setup])))
