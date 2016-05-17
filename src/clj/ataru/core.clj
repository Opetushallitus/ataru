(ns ataru.core
  (:require [com.stuartsierra.component :as component]
            [ataru.virkailija.virkailija-system :as virkailija-system]
            [ataru.hakija.hakija-system :as hakija-system]
            [environ.core :refer [env]]
            [taoensso.timbre :refer [info]])
  (:gen-class))

(defn- wait-forever
  []
  @(promise))

(def ^:private app-systems {:virkailija virkailija-system/new-system
                            :hakija hakija-system/new-system})

(defn- get-app-id [args]
  (if (> (count args) 0)
    (keyword (first args))
    (keyword (env :app))))

(defn -main [& args]
  (let [app-id (get-app-id args)
        system (get app-systems app-id)]
    (info "Starting application" app-id)
    (when-not system
      (println "ERROR: No system map found for application" app-id "exiting")
      (System/exit 1))
    (let [_ (component/start-system (system))]
      (wait-forever))))
