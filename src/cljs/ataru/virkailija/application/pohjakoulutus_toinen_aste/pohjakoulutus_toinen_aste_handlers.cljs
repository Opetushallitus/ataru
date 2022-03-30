(ns ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-handlers
  (:require [re-frame.core :as re-frame]
            [ataru.tarjonta.haku :as haku]))

(re-frame/reg-event-fx
  :application/fetch-applicant-pohjakoulutus
  (fn [_ [_ haku-oid application-key]]
    {:http {:method              :get
            :path                (str "/lomake-editori/api/valintalaskentakoostepalvelu/suoritukset/" haku-oid "?application-key=" application-key)
            :handler-or-dispatch :application/handle-fetch-applicant-pohjakoulutus-response
            :handler-args        application-key
            :id                  :fetch-applicant-pohjakoulutus}}))

(re-frame/reg-event-db
  :application/handle-fetch-applicant-pohjakoulutus-response
  (fn [db [_ response application-key]]
    (-> db
      (assoc-in [:application :pohjakoulutus-by-application-key application-key] response))))

(defn create-fetch-applicant-pohjakoulutus-event-if-toisen-asteen-yhteishaku
  [application]
  (when (haku/toisen-asteen-yhteishaku? (:tarjonta application))
    [:application/fetch-applicant-pohjakoulutus (:haku application) (:key application)]))
