(ns ataru.hakija.hakija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.db.migrations :as migrations]
            [ataru.hakija.hakija-routes :as handler]
            [ataru.http.server :as server]
            [ataru.hakija.email :as email]
            [environ.core :refer [env]]
            [ataru.codes-service.postal-code-client :as postal-code-client]))

(defn new-system
  ([]
   (new-system
     (Integer/parseInt (get env :ataru-http-port "8351"))
     (Integer/parseInt (get env :ataru-repl-port "3335"))))
  ([http-port repl-port]
   (component/system-map
     :handler (component/using
                (handler/new-handler)
                [:postal-code-client])

     :server-setup {:port http-port
                    :repl-port repl-port}

     :migration    (migrations/new-migration)

     :email        (email/new-emailer)

     :postal-code-client (postal-code-client/new-postal-code-client)

     :server       (component/using
                     (server/new-server)
                     [:server-setup :handler]))))
