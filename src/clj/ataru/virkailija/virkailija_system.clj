(ns ataru.virkailija.virkailija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.db.migrations :as migrations]
            [ataru.http.server :as server]
            [ataru.virkailija.virkailija-routes :refer [clerk-routes]]))

(defn new-system
  ([http-port]
   (component/system-map
     :server-setup {:routes clerk-routes :port http-port :repl-port 3333}
     :migration (migrations/new-migration)
     :server (component/using (server/new-server) [:server-setup])))
  ([]
   (new-system 8350)))
