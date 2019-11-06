(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-handlers
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
  :virkailija-kevyt-valinta/fetch-valintalaskentakoostepalvelu-valintalaskenta-in-use?
  (fn [_ [_ hakukohde-oid]]
    {:http {:method              :get
            :path                (str "/lomake-editori/api/valintalaskentakoostepalvelu/valintaperusteet/hakukohde/"
                                      hakukohde-oid
                                      "/kayttaa-valintalaskentaa")
            :handler-or-dispatch :virkailija-kevyt-valinta/handle-fetch-valintalaskentakoostepalvelu-valintalaskenta-in-use?
            :override-args       {:error-handler #(re-frame/dispatch [:application/handle-fetch-application-error])}}}))

(re-frame/reg-event-db
  :virkailija-kevyt-valinta/handle-fetch-valintalaskentakoostepalvelu-valintalaskenta-in-use?
  (fn [db [_ {hakukohde-oid :hakukohde-oid valintalaskenta :valintalaskenta}]]
    (assoc-in db
              [:application :valintalaskentakoostepalvelu hakukohde-oid :valintalaskenta]
              valintalaskenta)))
