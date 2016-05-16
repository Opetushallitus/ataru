(ns ataru.core
  (:require [com.stuartsierra.component :as component]
            [ataru.virkailija.virkailija-system :as virkailija-system]
            [environ.core :refer [env]]
            [taoensso.timbre :refer [info]])
  (:gen-class))

(defn ^:private wait-forever
  []
  @(promise))

(defn -main [& args]
  (let [system (virkailija-system/new-system)]
    (let [_ (component/start-system system)]
      (wait-forever))))
