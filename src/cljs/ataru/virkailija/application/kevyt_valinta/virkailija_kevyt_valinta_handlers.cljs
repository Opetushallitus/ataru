(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-handlers
  (:require [cljs-time.core :as t]
            [cljs-time.format :as format]
            [re-frame.core :as re-frame]))

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

(re-frame/reg-event-db
  :virkailija-kevyt-valinta/toggle-kevyt-valinta-dropdown
  (fn [db [_ kevyt-valinta-dropdown-id]]
    (update-in db
               [:application :kevyt-valinta]
               (fn [kevyt-valinta-db]
                 (as-> kevyt-valinta-db
                       kevyt-valinta-db'

                       (->> kevyt-valinta-db'
                            (keys)
                            (filter (comp (partial not= kevyt-valinta-dropdown-id)))
                            (reduce (fn [acc kevyt-valinta-property]
                                      (assoc-in acc [kevyt-valinta-property :open?] false))
                                    kevyt-valinta-db'))

                       (update-in kevyt-valinta-db'
                                  [kevyt-valinta-dropdown-id :open?]
                                  not))))))

(def rfc-1123-date-formatter (format/formatter "E, d MMM yyyy HH:mm:ss"))

(re-frame/reg-event-fx
  :virkailija-kevyt-valinta/change-kevyt-valinta-property
  [(re-frame/inject-cofx :virkailija/resolve-url {:url-key    :valinta-tulos-service.valinnan-tulos
                                                  :target-key :valinta-tulos-service-url})]
  (fn [{valinta-tulos-service-url :valinta-tulos-service-url
        db                        :db}
       [_
        kevyt-valinta-property
        hakukohde-oid
        application-key
        new-kevyt-valinta-property-value]]
    (let [request-id                     (keyword (str (name kevyt-valinta-property) "-" (t/epoch)))
          valinta-tulos-service-property (case kevyt-valinta-property
                                           :kevyt-valinta/valinnan-tila :valinnantila
                                           :kevyt-valinta/julkaisun-tila :julkaistavissa)
          db                             (-> db
                                             (update-in [:application :kevyt-valinta kevyt-valinta-property]
                                                        merge
                                                        {:request-id request-id
                                                         :open?      false})
                                             (assoc-in [:application :kevyt-valinta :kevyt-valinta-ui/ongoing-request-for-property]
                                                       kevyt-valinta-property)
                                             (assoc-in [:application
                                                        :valinta-tulos-service
                                                        application-key
                                                        hakukohde-oid
                                                        :valinnantulos
                                                        valinta-tulos-service-property]
                                                       new-kevyt-valinta-property-value))
          valinnan-tulos                 (-> db
                                             :application
                                             :valinta-tulos-service
                                             (get application-key)
                                             (get hakukohde-oid)
                                             :valinnantulos)
          valintatapajono-oid            (:valintatapajonoOid valinnan-tulos)
          request-body                   [(select-keys valinnan-tulos
                                                       [:vastaanottotila
                                                        :hakukohdeOid
                                                        :ilmoittautumistila
                                                        :henkiloOid
                                                        :valintatapajonoOid
                                                        :hakemusOid
                                                        :valinnantila])]
          now                            (t/now)
          formatted-now                  (str (format/unparse rfc-1123-date-formatter now) " GMT")]
      {:db   db
       :http {:method        :patch
              :path          (str valinta-tulos-service-url "/" valintatapajono-oid "?erillishaku=true")
              :id            request-id
              :override-args {:params              request-body
                              :headers             {"X-If-Unmodified-Since" formatted-now}
                              :handler-or-dispatch :virkailija-kevyt-valinta/handle-changed-kevyt-valinta-property}}})))

(re-frame/reg-event-db
  :virkailija-kevyt-valinta/handle-changed-kevyt-valinta-property
  (fn [db [_ response]]
    (println (str "response: " response))
    db))
