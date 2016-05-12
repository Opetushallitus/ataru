(ns ataru.system
  (:require [com.stuartsierra.component :as component]
            [ataru.virkailija.virkailija-server :as virkailija-server]
            [ataru.db.migrations :as migrations]
            [taoensso.timbre :refer [info]])
  (:gen-class))

(defn new-system
  []
  (component/system-map
    :migration (migrations/new-migration)
    :server (virkailija-server/new-server)))

(defn ^:private wait-forever
  []
  @(promise))

(defn -main [& _]
  (let [system (new-system)]
    (info "Starting lomake-editori system")
    (let [_ (component/start-system system)]
      (wait-forever))))
