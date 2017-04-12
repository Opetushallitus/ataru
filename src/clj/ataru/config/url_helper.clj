(ns ataru.config.url-helper
  (:require [ataru.config.core :refer [config]])
  (:import (fi.vm.sade.properties OphProperties)))

; TODO component if not too much work?
(def ^OphProperties url-properties (atom nil))

(defn- load-config
  []
  (let [{:keys [virkailija-host hakija-host editor-url liiteri-url] :or
               {virkailija-host "" hakija-host "" editor-url "" liiteri-url ""}} (:urls config)]
    (reset! url-properties
            (doto (OphProperties. (into-array String ["/ataru-oph.properties"]))
              (.addDefault "host-virkailija" virkailija-host)
              (.addDefault "host-hakija" hakija-host)
              (.addDefault "url-editor" editor-url)
              (.addDefault "url-liiteri" liiteri-url)))))

(defn resolve-url
  [key & params]
  (when (nil? @url-properties)
    (load-config))
  (.url @url-properties (name key) (to-array (or params []))))
