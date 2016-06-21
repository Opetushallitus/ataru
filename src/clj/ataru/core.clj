(ns ataru.core
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh]]
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

(def system nil)

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system (fn [s] (when s (component/stop s)))))

(defn- get-app-id [[app-id & _]]
  (keyword
    (or app-id
        (:app env))))

(defn restart []
  (stop)
  (start))

(defn -main [& args]
  (let [app-id (get-app-id args)
        system ((get app-systems app-id (constantly nil)))]
    (info "Starting application" app-id (if (:dev? env) "dev" ""))
    (when-not system
      (println "ERROR: No system map found for application" app-id "exiting. Valid keys: "
               (apply str (interpose ", " (map name (keys app-systems)))))
      (System/exit 1))
    (do
      (alter-var-root #'system
                      (constantly system))
      (start)
      (wait-forever))))
