(ns ataru.hakija.hakija-system
  (:require [com.stuartsierra.component :as component]
            [ataru.hakija.hakija-routes :as handler]
            [ataru.background-job.job :as job]
            [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
            [ataru.http.server :as server]
            [ataru.hakija.legacy-application-email.email :as email]
            [ataru.person-service.person-service :as person-service]
            [environ.core :refer [env]]))

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

     ;; TODO remove this and the code behind it
     ;; after all the legacy emails have been sent
     ;; Now background jobs handle this
     :email                (email/new-emailer)

     :server               (component/using
                           (server/new-server)
                            [:server-setup :handler])

     :person-service       (person-service/new-person-service)

     :job-definitions      hakija-jobs/job-definitions

     :job-runner           (component/using
                             (job/new-job-runner)
                             [:job-definitions :person-service]))))
