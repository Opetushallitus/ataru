(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-handlers
  (:require [ataru.virkailija.kevyt-valinta.virkailija-kevyt-valinta-pseudo-random-valintatapajono-oids :as valintatapajono-oids]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-mappings :as mappings]
            [cljs-time.core :as t]
            [cljs-time.format :as format]
            [re-frame.core :as re-frame])
  (:require-macros [cljs.core.match :refer [match]]))

(re-frame/reg-event-fx
  :virkailija-kevyt-valinta/fetch-valintalaskentakoostepalvelu-valintalaskenta-in-use?
  (fn [{db :db}
       [_ {:keys [hakukohde-oid]}]]
    (if (-> db :application :valintalaskentakoostepalvelu (get hakukohde-oid) :valintalaskenta some?)
      {}
      {:http {:method              :get
              :path                (str "/lomake-editori/api/valintalaskentakoostepalvelu/valintaperusteet/hakukohde/"
                                        hakukohde-oid
                                        "/kayttaa-valintalaskentaa")
              :handler-or-dispatch :virkailija-kevyt-valinta/handle-fetch-valintalaskentakoostepalvelu-valintalaskenta-in-use?
              :override-args       {:error-handler #(re-frame/dispatch [:application/handle-fetch-application-error])}}})))

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
  (fn [{db                        :db
        valinta-tulos-service-url :valinta-tulos-service-url} [_ {:keys [application-key
                                                                         memoize]}]]
    (if (and memoize
             (-> db :valinta-tulos-service (get application-key) not-empty))
      {}
      {:db   (update db :valinta-tulos-service dissoc application-key)
       :http {:method              :get
              :path                (str valinta-tulos-service-url "?hakemusOid=" application-key)
              :handler-or-dispatch :virkailija-kevyt-valinta/handle-fetch-valinnan-tulos
              :handler-args        {:application-key application-key}}})))

(re-frame/reg-event-fx
  :virkailija-kevyt-valinta/handle-fetch-valinnan-tulos
  (fn [{db :db} [_ response {application-key :application-key}]]
    (let [db           (-> db
                           (assoc-in [:valinta-tulos-service application-key] {})
                           (update
                             :valinta-tulos-service
                             (fn valinnan-tulokset->db [valinta-tulos-service-db]
                               (->> response
                                    (reduce (fn valinnan-tulos->db [acc valinnan-tulos]
                                              (let [hakemus-oid   (-> valinnan-tulos :valinnantulos :hakemusOid)
                                                    hakukohde-oid (-> valinnan-tulos :valinnantulos :hakukohdeOid)]
                                                (assoc-in acc
                                                          [hakemus-oid hakukohde-oid]
                                                          valinnan-tulos)))
                                            valinta-tulos-service-db)))))
          dispatch-vec []]
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

(defn- new-kevyt-valinta-states [valinnan-tila
                                 julkaisun-tila
                                 vastaanotto-tila
                                 ilmoittautumisen-tila]
  (match [vastaanotto-tila]
         [(:or "EHDOLLISESTI_VASTAANOTTANUT" "VASTAANOTTANUT_SITOVASTI")]
         {:valinnantila       "HYVAKSYTTY"
          :julkaistavissa     julkaisun-tila
          :vastaanottotila    vastaanotto-tila
          :ilmoittautumistila ilmoittautumisen-tila}

         [(:or "EI_VASTAANOTETTU_MAARA_AIKANA" "OTTANUT_VASTAAN_TOISEN_PAIKAN")]
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
          :ilmoittautumistila ilmoittautumisen-tila}))

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
          valintatapajono-oid              (valintatapajono-oids/pseudo-random-valintatapajono-oid haku-oid hakukohde-oid)
          db                               (as-> db db'
                                                 (update-in db' [:application :kevyt-valinta kevyt-valinta-property]
                                                            merge
                                                            {:request-id request-id
                                                             :open?      false})
                                                 (assoc-in db' [:application :kevyt-valinta :kevyt-valinta-ui/ongoing-request-for-property]
                                                           kevyt-valinta-property)
                                                 (update-in db' [:valinta-tulos-service
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
                                                                      kevyt-valinta-states (new-kevyt-valinta-states valinnan-tila
                                                                                                                     julkaisun-tila
                                                                                                                     vastaanotto-tila
                                                                                                                     ilmoittautumisen-tila)]
                                                                  (merge valinnantulos kevyt-valinta-states))
                                                                {:vastaanottotila              "KESKEN"
                                                                 :hakukohdeOid                 hakukohde-oid
                                                                 :ilmoittautumistila           "EI_TEHTY"
                                                                 :henkiloOid                   henkilo-oid
                                                                 :valintatapajonoOid           valintatapajono-oid
                                                                 :hakemusOid                   application-key
                                                                 :valinnantila                 new-kevyt-valinta-property-value
                                                                 :julkaistavissa               false
                                                                 :valinnantilanViimeisinMuutos now})))
                                                 (assoc-in db' [:valinta-tulos-service
                                                                application-key
                                                                hakukohde-oid
                                                                :valinnantulos
                                                                valinta-tulos-service-property]
                                                           new-kevyt-valinta-property-value)
                                                 (cond-> db'
                                                         (= kevyt-valinta-property :kevyt-valinta/valinnan-tila)
                                                         (update-in [:valinta-tulos-service
                                                                     application-key
                                                                     hakukohde-oid
                                                                     :tilaHistoria]
                                                                    (fnil conj [])
                                                                    (let [old-valinnan-tulos               (-> db
                                                                                                               :valinta-tulos-service
                                                                                                               (get application-key)
                                                                                                               (get hakukohde-oid)
                                                                                                               :valinnantulos)
                                                                          old-kevyt-valinta-property-value (:valinnantila old-valinnan-tulos)
                                                                          created-time                     (:valinnantilanViimeisinMuutos old-valinnan-tulos)]
                                                                      {:valintatapajonoOid valintatapajono-oid
                                                                       :hakemusOid         application-key
                                                                       :tila               old-kevyt-valinta-property-value
                                                                       :luotu              created-time}))))
          valinnan-tulos                   (-> db
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
              :handler-or-dispatch :virkailija-kevyt-valinta/handle-changed-kevyt-valinta-property
              :handler-args        {:application-key application-key}}})))

(re-frame/reg-event-fx
  :virkailija-kevyt-valinta/handle-changed-kevyt-valinta-property
  (fn [{db :db} [_ _ {application-key :application-key}]]
    {:db       (update-in db
                          [:application :kevyt-valinta]
                          dissoc
                          :kevyt-valinta-ui/ongoing-request-for-property)
     :dispatch [:virkailija-kevyt-valinta/fetch-valinnan-tulos {:application-key application-key}]}))
