(ns ataru.config.url-helper
  (:require [ataru.config.core :refer [config]]
            [clojure.string :as string])
  (:import (fi.vm.sade.properties OphProperties)))

; TODO component if not too much work?
(def ^fi.vm.sade.properties.OphProperties url-properties (atom nil))

(defn- load-config
  []
  (let [{:keys [virkailija-host
                hakija-host
                editor-url
                liiteri-url
                valinta-tulos-service-base-url
                organisaatio-service-base-url
                koodisto-service-base-url
                ohjausparametrit-service-base-url
                valintalaskenta-ui-service-base-url]} (:urls config)]
    (reset! url-properties
            (doto (OphProperties. (into-array String ["/ataru-oph.properties"]))
              (.addDefault "host-virkailija" virkailija-host)
              (.addDefault "host-hakija" hakija-host)
              (.addDefault "url-editor" editor-url)
              (.addDefault "url-liiteri" liiteri-url)
              (.addDefault "url-liiteri" liiteri-url)
              (.addDefault "baseurl-valinta-tulos-service" valinta-tulos-service-base-url)
              (.addDefault "baseurl-organisaatio-service" organisaatio-service-base-url)
              (.addDefault "baseurl-koodisto-service" koodisto-service-base-url)
              (.addDefault "baseurl-ohjausparametrit-service" ohjausparametrit-service-base-url)
              (.addDefault "baseurl-valintalaskenta-ui-service" valintalaskenta-ui-service-base-url)))))

(defn resolve-url
  [key & params]
  (when (nil? @url-properties)
    (load-config))
  (.url @url-properties (name key) (to-array (or params []))))

(defn front-json
  []
  (when (nil? @url-properties)
    (load-config))
  (.frontPropertiesToJson @url-properties))
