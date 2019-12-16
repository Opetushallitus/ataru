(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-handlers
  (:require [ataru.virkailija.kevyt-valinta.virkailija-kevyt-valinta-pseudo-random-valintatapajono-oids :as valintatapajono-oids]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-mappings :as mappings]
            [cljs-time.core :as t]
            [cljs-time.format :as format]
            [re-frame.core :as re-frame])
  (:require-macros [cljs.core.match :refer [match]]))

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
            :handler-or-dispatch :virkailija-kevyt-valinta/handle-fetch-valinnan-tulos
            :handler-args        {:application-key application-key}}}))

(re-frame/reg-event-fx
  :virkailija-kevyt-valinta/handle-fetch-valinnan-tulos
  (fn [{db :db} [_ response {application-key :application-key}]]
    (let [db           (update-in db
                                  [:application :valinta-tulos-service]
                                  (fn valinnan-tulokset->db [valinta-tulos-service-db]
                                    (->> response
                                         (filter (fn [valinnan-tulos]
                                                   (let [vastaanotto-tila (-> valinnan-tulos :valinnantulos :vastaanottotila)]
                                                     (not= vastaanotto-tila "OTTANUT_VASTAAN_TOISEN_PAIKAN"))))
                                         (reduce (fn valinnan-tulos->db [acc valinnan-tulos]
                                                   (let [hakemus-oid   (-> valinnan-tulos :valinnantulos :hakemusOid)
                                                         hakukohde-oid (-> valinnan-tulos :valinnantulos :hakukohdeOid)]
                                                     (assoc-in acc
                                                               [hakemus-oid hakukohde-oid]
                                                               valinnan-tulos)))
                                                 valinta-tulos-service-db))))
          dispatch-vec (->> response
                            (filter (fn [valinnan-tulos]
                                      (let [vastaanotto-tila (-> valinnan-tulos :valinnantulos :vastaanottotila)]
                                        (= vastaanotto-tila "OTTANUT_VASTAAN_TOISEN_PAIKAN"))))
                            (map (fn [valinnan-tulos]
                                   (let [hakukohde-oid (-> valinnan-tulos :valinnantulos :hakukohdeOid)]
                                     [:virkailija-kevyt-valinta/change-kevyt-valinta-property
                                      :kevyt-valinta/valinnan-tila
                                      hakukohde-oid
                                      application-key
                                      "PERUUNTUNUT"]))))]
      (cond-> {:db db}
              (not-empty dispatch-vec)
              (assoc :dispatch-n dispatch-vec)))))

(re-frame/reg-event-db
  :virkailija-kevyt-valinta/toggle-kevyt-valinta-dropdown
  (fn [db [_ kevyt-valinta-property]]
    (update-in db
               [:application :kevyt-valinta]
               (fn [kevyt-valinta-db]
                 (as-> kevyt-valinta-db
                       kevyt-valinta-db'

                       (->> kevyt-valinta-db'
                            (keys)
                            (filter (comp (partial not= :kevyt-valinta-ui/ongoing-request-for-property)))
                            (filter (comp (partial not= kevyt-valinta-property)))
                            (reduce (fn [acc kevyt-valinta-property]
                                      (assoc-in acc [kevyt-valinta-property :open?] false))
                                    kevyt-valinta-db'))

                       (update-in kevyt-valinta-db'
                                  [kevyt-valinta-property :open?]
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
    (let [new-kevyt-valinta-property-value (mappings/kevyt-valinta-property-value->valinta-tulos-service-value
                                             new-kevyt-valinta-property-value
                                             kevyt-valinta-property)
          now                              (t/now)
          request-id                       (keyword (str (name kevyt-valinta-property) "-" now))
          valinta-tulos-service-property   (mappings/kevyt-valinta-property->valinta-tulos-service-property kevyt-valinta-property)
          haku-oid                         (-> db :application :selected-application-and-form :application :haku)
          henkilo-oid                      (-> db :application :selected-application-and-form :application :person :oid)
          db                               (-> db
                                               (update-in [:application :kevyt-valinta kevyt-valinta-property]
                                                          merge
                                                          {:request-id request-id
                                                           :open?      false})
                                               (assoc-in [:application :kevyt-valinta :kevyt-valinta-ui/ongoing-request-for-property]
                                                         kevyt-valinta-property)
                                               (update-in [:application
                                                           :valinta-tulos-service
                                                           application-key
                                                           hakukohde-oid
                                                           :valinnantulos]
                                                          (fn [valinnantulos]
                                                            (if valinnantulos
                                                              (let [{valinnan-tila         :valinnantila
                                                                     julkaisun-tila        :julkaistavissa
                                                                     vastaanotto-tila      :vastaanottotila
                                                                     ilmoittautumisen-tila :ilmoittautumistila} (assoc
                                                                                                                  valinnantulos
                                                                                                                  valinta-tulos-service-property
                                                                                                                  new-kevyt-valinta-property-value)
                                                                    new-kevyt-valinta-states (match [vastaanotto-tila]
                                                                                                    [(:or "EHDOLLISESTI_VASTAANOTTANUT" "VASTAANOTTANUT_SITOVASTI")]
                                                                                                    {:valinnantila       "HYVAKSYTTY"
                                                                                                     :julkaistavissa     julkaisun-tila
                                                                                                     :vastaanottotila    vastaanotto-tila
                                                                                                     :ilmoittautumistila ilmoittautumisen-tila}

                                                                                                    ["EI_VASTAANOTETTU_MAARA_AIKANA"]
                                                                                                    {:valinnantila       "PERUUNTUNUT"
                                                                                                     :julkaistavissa     julkaisun-tila
                                                                                                     :vastaanottotila    vastaanotto-tila
                                                                                                     :ilmoittautumistila ilmoittautumisen-tila}

                                                                                                    ["PERUNUT"]
                                                                                                    {:valinnantila       "PERUNUT"
                                                                                                     :julkaistavissa     julkaisun-tila
                                                                                                     :vastaanottotila    vastaanotto-tila
                                                                                                     :ilmoittautumistila ilmoittautumisen-tila}

                                                                                                    ["PERUUTETTU"]
                                                                                                    {:valinnantila       "PERUUTETTU"
                                                                                                     :julkaistavissa     julkaisun-tila
                                                                                                     :vastaanottotila    vastaanotto-tila
                                                                                                     :ilmoittautumistila ilmoittautumisen-tila}

                                                                                                    :else
                                                                                                    {:valinnantila       valinnan-tila
                                                                                                     :julkaistavissa     julkaisun-tila
                                                                                                     :vastaanottotila    vastaanotto-tila
                                                                                                     :ilmoittautumistila ilmoittautumisen-tila})]
                                                                (merge valinnantulos new-kevyt-valinta-states))
                                                              {:vastaanottotila    "KESKEN"
                                                               :hakukohdeOid       hakukohde-oid
                                                               :ilmoittautumistila "EI_TEHTY"
                                                               :henkiloOid         henkilo-oid
                                                               :valintatapajonoOid (valintatapajono-oids/pseudo-random-valintatapajono-oid haku-oid hakukohde-oid)
                                                               :hakemusOid         application-key
                                                               :valinnantila       new-kevyt-valinta-property-value
                                                               :julkaistavissa     false})))
                                               (assoc-in [:application
                                                          :valinta-tulos-service
                                                          application-key
                                                          hakukohde-oid
                                                          :valinnantulos
                                                          valinta-tulos-service-property]
                                                         new-kevyt-valinta-property-value))
          valinnan-tulos                   (-> db
                                               :application
                                               :valinta-tulos-service
                                               (get application-key)
                                               (get hakukohde-oid)
                                               :valinnantulos)
          valintatapajono-oid              (:valintatapajonoOid valinnan-tulos)
          request-body                     [(select-keys valinnan-tulos
                                                         [:vastaanottotila
                                                          :hakukohdeOid
                                                          :ilmoittautumistila
                                                          :henkiloOid
                                                          :valintatapajonoOid
                                                          :hakemusOid
                                                          :valinnantila
                                                          :julkaistavissa])]
          formatted-now                    (str (format/unparse rfc-1123-date-formatter now) " GMT")]
      {:db   db
       :http {:method              :patch
              :path                (str valinta-tulos-service-url "/" valintatapajono-oid "?erillishaku=true")
              :id                  request-id
              :override-args       {:params  request-body
                                    :headers {"X-If-Unmodified-Since" formatted-now}}
              :handler-or-dispatch :virkailija-kevyt-valinta/handle-changed-kevyt-valinta-property}})))

(re-frame/reg-event-db
  :virkailija-kevyt-valinta/handle-changed-kevyt-valinta-property
  (fn [db]
    (update-in db
               [:application :kevyt-valinta]
               dissoc
               :kevyt-valinta-ui/ongoing-request-for-property)))
