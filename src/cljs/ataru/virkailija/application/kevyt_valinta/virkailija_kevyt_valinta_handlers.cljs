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

(re-frame/reg-event-fx
  :virkailija-kevyt-valinta/fetch-valinnan-tulos
  [(re-frame/inject-cofx :virkailija/resolve-url {:url-key    :valinta-tulos-service.valinnan-tulos.hakemus
                                                  :target-key :valinta-tulos-service-url})]
  (fn [{valinta-tulos-service-url :valinta-tulos-service-url} [_ application-key]]
    {:http {:method              :get
            :path                (str valinta-tulos-service-url "?hakemusOid=" application-key)
            :handler-or-dispatch :virkailija-kevyt-valinta/handle-fetch-valinnan-tulos}}))

(re-frame/reg-event-db
  :virkailija-kevyt-valinta/handle-fetch-valinnan-tulos
  (fn [db [_ response]]
    (update-in db
               [:application :valinta-tulos-service]
               (fn valinnan-tulokset->db [valinta-tulos-service-db]
                 (reduce (fn valinnan-tulos->db [acc valinnan-tulos]
                           (let [hakemus-oid   (-> valinnan-tulos :valinnantulos :hakemusOid)
                                 hakukohde-oid (-> valinnan-tulos :valinnantulos :hakukohdeOid)]
                             (assoc-in acc
                                       [hakemus-oid hakukohde-oid]
                                       valinnan-tulos)))
                         valinta-tulos-service-db
                         response)))))
