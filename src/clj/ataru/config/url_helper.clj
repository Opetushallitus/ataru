(ns ataru.config.url-helper
  (:require [clojure.tools.logging :refer [spy]])
  (:import (fi.vm.sade.properties OphProperties)))

; TODO component if not too much work?
(def ^fi.vm.sade.properties.OphProperties url-config (atom nil))

(defn- load-config
  []
  (reset! url-config
          (doto (OphProperties. (into-array String ["/ataru-oph.properties"]))
            (.addDefault "host-virkailija" "itest-virkailija.oph.ware.fi")
            (.addDefault "host-hakija" "itest-hakija.oph.ware.fi"))))

(defn resolve-url
  [key & params]
  (when (nil? @url-config)
    (load-config))
  (.url @url-config (name key) (to-array (or params []))))
