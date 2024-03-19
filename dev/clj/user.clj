(ns user
  (:require [reloaded.repl :refer [set-init! system initializer init start stop go reset]]
            [clojure.tools.namespace.repl :refer [disable-reload! refresh refresh-all set-refresh-dirs clear]]
            [com.gfredericks.debug-repl.async :refer [wait-for-breaks]]
            [environ.core :refer [env]]))

; uudelleenladataan vain ataru-backend -luokkia
(set-refresh-dirs "./src/clj/ataru")

(defn get-init []
      (let [app (:app env)]
           (case app
                 "ataru-editori" (do
                                    (require 'ataru.virkailija.virkailija-system)
                                    (resolve 'ataru.virkailija.virkailija-system/new-system))
                 "ataru-hakija" (do
                                   (require 'ataru.hakija.hakija-system)
                                   (resolve 'ataru.hakija.hakija-system/new-system))
                 "default" #(throw (RuntimeException. (str "Init function not defined for app: " app))))))


(defn get-audit-logger []
  (require 'ataru.log.audit-log)
  (resolve 'ataru.log.audit-log/new-audit-logger))

; kerrotaan reloaded.repl -kirjastolle miten sovelluksen saa rakennettua
(set-init! (fn [] ((get-init) ((get-audit-logger) (:app env)))))
