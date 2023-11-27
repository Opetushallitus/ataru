(ns ataru.siirtotiedosto-service
  (:require [ataru.applications.application-store :as application-store]))

(defprotocol SiirtotiedostoService
  (siirtotiedosto-applications [this session params]))

(defrecord CommonSiirtotiedostoService [organization-service
                                        tarjonta-service
                                        ohjausparametrit-service
                                        audit-logger
                                        person-service
                                        valinta-tulos-service
                                        koodisto-cache
                                        job-runner
                                        liiteri-cas-client
                                        suoritus-service
                                        form-by-id-cache
                                        valintalaskentakoostepalvelu-service]
  SiirtotiedostoService
  (siirtotiedosto-applications
    [_ session params]
    (if (-> session :identity :superuser)
      (application-store/siirtotiedosto-applications-paged params)
      {:unauthorized nil})))

(defn new-siirtotiedosto-service [] (->CommonSiirtotiedostoService nil nil nil nil nil nil nil nil nil nil nil nil))
