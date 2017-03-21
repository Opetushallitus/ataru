(ns ataru.config.url-helper
  (:require [oph.soresu.common.config :refer [config]])
  (:import (fi.vm.sade.properties OphProperties)))

; TODO component if not too much work?
(def ^fi.vm.sade.properties.OphProperties url-config (atom nil))

(defn- load-config
  [{:keys [virkailija-host hakija-host liiteri-url] :as url-config}]
  (reset! url-config
          (doto (OphProperties. (into-array String ["/ataru-oph.properties"]))
            (.addDefault "host-virkailija" (or virkailija-host "itest-virkailija.oph.ware.fi"))
            (.addDefault "host-hakija" (or hakija-host "itest-hakija.oph.ware.fi"))
            (.addDefault "url-liiteri" liiteri-url))))

(defn resolve-url
  [key & params]
  (when (nil? @url-config)
    (load-config (:urls config)))
  (.url @url-config (name key) (to-array (or params []))))
