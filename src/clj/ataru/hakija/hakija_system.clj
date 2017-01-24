(ns ataru.hakija.hakija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.hakija.hakija-routes :as handler]
            [ataru.background-job.job :as job]
            [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
            [ataru.http.server :as server]
            [ataru.person-service.person-service :as person-service]
            [environ.core :refer [env]]
            [ataru.cache.cache-service :as cache-service]))

(defn new-system
  ([]
   (new-system
     (Integer/parseInt (get env :ataru-http-port "8351"))
     (Integer/parseInt (get env :ataru-repl-port "3335"))))
  ([http-port repl-port]
   (component/system-map
     :handler              (handler/new-handler)

     :server-setup         {:port      http-port
                            :repl-port repl-port}

     :server               (component/using
                           (server/new-server)
                            [:server-setup :handler])

     :person-service       (person-service/new-person-service)

     :job-runner           (component/using
                             (job/new-job-runner hakija-jobs/job-definitions)
                             [:person-service])

     :cache                (cache-service/new-cache-service))))
