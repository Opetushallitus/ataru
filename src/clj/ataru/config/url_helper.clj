(ns ataru.config.url-helper
  (:require [ataru.config.core :refer [config]]
            [clojure.pprint :as pprint]
            [taoensso.timbre :as log])
  (:import (fi.vm.sade.properties OphProperties)))

; TODO component if not too much work?
(def ^fi.vm.sade.properties.OphProperties url-properties (atom nil))

(defn- add-default! [oph-properties name value]
  (if (some? value)
    (doto oph-properties (.addDefault name value))
    (throw (ex-info (str "Default value for oph-properties property '" name "' is missing.")
                    {:property-name name
                     :error         "missing default value"}))))

(defn- pretty-print [config]
  (with-out-str
    (binding [pprint/*print-right-margin* 120]
      (pprint/pprint config))))

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
                tutu-service-base-url
                valintalaskenta-ui-service-base-url
                ataru-hakija-login-url
                cas-oppija-url]} (:urls config)]
    (log/info "load-config: url-properties default values:\n" (pretty-print (:urls config)))
    (reset! url-properties
            (doto (OphProperties. (into-array String ["/ataru-oph.properties"]))
              (add-default! "host-virkailija" virkailija-host)
              (add-default! "host-hakija" hakija-host)
              (add-default! "url-editor" editor-url)
              (add-default! "url-liiteri" liiteri-url)
              (add-default! "baseurl-valinta-tulos-service" valinta-tulos-service-base-url)
              (add-default! "baseurl-organisaatio-service" organisaatio-service-base-url)
              (add-default! "baseurl-koodisto-service" koodisto-service-base-url)
              (add-default! "baseurl-ohjausparametrit-service" ohjausparametrit-service-base-url)
              (add-default! "baseurl-tutu-service" tutu-service-base-url)
              (add-default! "baseurl-valintalaskenta-ui-service" valintalaskenta-ui-service-base-url)
              (add-default! "ataru-hakija-login-url" ataru-hakija-login-url)
              (add-default! "cas-oppija-url" cas-oppija-url)))))

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
