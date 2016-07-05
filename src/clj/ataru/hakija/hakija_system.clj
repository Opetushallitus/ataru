(ns ataru.hakija.hakija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.db.migrations :as migrations]
            [ataru.hakija.hakija-routes :as handler]
            [ataru.http.server :as server]))

(defn new-system
  []
  (component/system-map
    :handler      (handler/new-handler)

    :server-setup {:port 8351 :repl-port 3335}

    :migration    (migrations/new-migration)
    :server       (component/using
                    (server/new-server)
                    [:server-setup :handler])))
