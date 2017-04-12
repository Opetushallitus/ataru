(ns ataru.config.url-helper
  (:require [ataru.config.core :refer [config]])
  (:import (fi.vm.sade.properties OphProperties)))

(def ^OphProperties url-properties
  (let [{:keys [virkailija-host hakija-host editor-url liiteri-url] :or
               {virkailija-host "" hakija-host "" editor-url "" liiteri-url ""}} (:urls config)]
    (doto (OphProperties. (into-array String ["/ataru-oph.properties"]))
      (.addDefault "host-virkailija" virkailija-host)
      (.addDefault "host-hakija" hakija-host)
      (.addDefault "url-editor" editor-url)
      (.addDefault "url-liiteri" liiteri-url))))

(defn resolve-url
  [key & params]
  (.url url-properties (name key) (to-array (or params []))))
