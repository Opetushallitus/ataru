(ns ataru.hakija.hakija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.db.migrations :as migrations]
            [ataru.hakija.hakija-routes :as handler]
            [ataru.http.server :as server]
            [environ.core :refer [env]]))

(defn new-system
  ([]
   (new-system
     (Integer/parseInt (get env :ataru-http-port "8351"))
     (Integer/parseInt (get env :ataru-repl-port "3335"))))
  ([http-port repl-port]
   (component/system-map
     :handler      (handler/new-handler)

     :server-setup {:port http-port
                    :repl-port repl-port}

     :migration    (migrations/new-migration)
     :server       (component/using
                     (server/new-server)
                     [:server-setup :handler]))))
