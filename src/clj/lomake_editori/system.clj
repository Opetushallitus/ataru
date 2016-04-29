(ns lomake-editori.system
  (:require [com.stuartsierra.component :as component]
            [lomake-editori.core :as server]))

(defn new-system
  []
  (component/system-map
    :server (server/new-server)))

