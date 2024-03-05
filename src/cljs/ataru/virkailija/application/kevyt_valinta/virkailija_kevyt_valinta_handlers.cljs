(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-handlers
  (:require [ataru.virkailija.kevyt-valinta.virkailija-kevyt-valinta-pseudo-random-valintatapajono-oids :as valintatapajono-oids]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-mappings :as mappings]
            [ataru.virkailija.application.application-subs :as application]
            [ataru.application.filtering :as application-filtering]
            [clojure.string :as string]
            [cljs-time.core :as t]
            [cljs-time.format :as format]
            [re-frame.core :as re-frame])
  (:require-macros [cljs.core.match :refer [match]]))

(defn- kevyt-valinta-enabled-for-application-and-hakukohde? [db
                                                             application
                                                             hakukohde-oid]
  (let [haut                                    (:haut db)
        valid-hakukohde?                        (not (= hakukohde-oid "form"))
        sijoittelu-not-enabled-for-application? (not (->> application
                                                          :haku
                                                          (get haut)
                                                          :sijoittelu))
        valintalaskenta-not-in-hakukohde?       (-> db
                                                    :application
                                                    :valintalaskentakoostepalvelu
                                                    (get hakukohde-oid)
                                                    :valintalaskenta
                                                    true?
                                                    not)
        selection-state-not-used-in-hakukohde?  (-> db
                                                    :hakukohteet
                                                    (get hakukohde-oid)
                                                    :selection-state-used
                                                    not)]
    (and valid-hakukohde?
         sijoittelu-not-enabled-for-application?
         valintalaskenta-not-in-hakukohde?
         selection-state-not-used-in-hakukohde?)))

(re-frame/reg-event-db
  :virkailija-kevyt-valinta/filter-applications
  (fn [db]
    (let [{:keys [kevyt-valinta-hakukohde-oids
                  valintakasittelymerkinta-hakukohde-oids]}
          (->> db
               :application
               :applications
               (mapcat (fn [{hakukohde-oids :hakukohde
                             :as            application}]
                         (map (fn [hakukohde-oid]
                                {:application   application
                                 :hakukohde-oid hakukohde-oid})
                              (if (empty? hakukohde-oids)
                                ["form"]
                                hakukohde-oids))))
               (reduce (fn [acc {:keys [hakukohde-oid
                                        application]}]
                         (let [kw (if (kevyt-valinta-enabled-for-application-and-hakukohde?
                                        db
                                        application
                                        hakukohde-oid)
                                    :kevyt-valinta-hakukohde-oids
                                    :valintakasittelymerkinta-hakukohde-oids)]
                           (update-in acc
                                      [kw hakukohde-oid]
                                      (fnil conj #{})
                                      (:key application))))
                       {:kevyt-valinta-hakukohde-oids            {}
                        :valintakasittelymerkinta-hakukohde-oids {}}))
          selection-state-filter
          (-> db :application :selection-state-filter set)
          applications
          (-> db :application :applications)
          update-counts-fn (fn [db] (-> db
                                        (update-in
                                          [:application :kevyt-valinta-selection-state-counts]
                                          application-filtering/add-kevyt-valinta-selection-state-counts
                                          db
                                          applications
                                          kevyt-valinta-hakukohde-oids)
                                        (update-in
                                          [:application :kevyt-valinta-vastaanotto-state-counts]
                                          application-filtering/add-kevyt-valinta-vastaanotto-state-counts
                                          db
                                          applications
                                          kevyt-valinta-hakukohde-oids)))]
      (as-> db db'
            (update-in
              db'
              [:application :applications]
              (fn [applications]
                (filter (fn [{application-key :key
                              hakukohde-oids  :hakukohde
                              :as             application}]
                          (let [{:keys [kevyt-valinta-hakukohde-oids'
                                        valintakasittelymerkinta-hakukohde-oids']}
                                (reduce (fn [acc hakukohde-oid]
                                          (let [kw (if (-> kevyt-valinta-hakukohde-oids
                                                           (get hakukohde-oid)
                                                           (contains? application-key))
                                                     :kevyt-valinta-hakukohde-oids'
                                                     :valintakasittelymerkinta-hakukohde-oids')]
                                            (update acc kw conj hakukohde-oid)))
                                        {:kevyt-valinta-hakukohde-oids'            #{}
                                         :valintakasittelymerkinta-hakukohde-oids' #{}}
                                        (if (empty? hakukohde-oids)
                                          ["form"]
                                          hakukohde-oids))]
                            (and (or (empty? kevyt-valinta-hakukohde-oids')
                                     (and (application-filtering/filter-by-kevyt-valinta-selection-state
                                            db
                                            application-key
                                            kevyt-valinta-hakukohde-oids')
                                          (application-filtering/filter-by-kevyt-valinta-vastaanotto-state
                                            db
                                            application-key
                                            kevyt-valinta-hakukohde-oids')))
                                 (or (empty? valintakasittelymerkinta-hakukohde-oids')
                                     (application-filtering/filter-by-hakukohde-review
                                       application
                                       valintakasittelymerkinta-hakukohde-oids'
                                       "selection-state"
                                       selection-state-filter)))))
                        applications)))
            (cond-> db'
                    (not-empty valintakasittelymerkinta-hakukohde-oids)
                    (update-in
                      [:application :selection-state-counts]
                      application-filtering/add-review-state-counts
                      applications
                      valintakasittelymerkinta-hakukohde-oids
                      "selection-state")
                    (not-empty kevyt-valinta-hakukohde-oids)
                    (update-counts-fn))))))

(re-frame/reg-event-fx
  :virkailija-kevyt-valinta/fetch-valintalaskentakoostepalvelu-valintalaskenta-in-use?
  (fn [{db :db}
       [_ {:keys [hakukohde-oid]}]]
    (if (-> db :application :valintalaskentakoostepalvelu (get hakukohde-oid) :valintalaskenta some?)
      (let [new-multiple-requests-count (some-> db :kevyt-valinta :multiple-requests-count dec)]
        (cond-> {}
                (= new-multiple-requests-count 0)
                (assoc
                  :db
                  (update db :kevyt-valinta dissoc :multiple-requests-count))
                (> new-multiple-requests-count 0)
                (assoc
                  :db
                  (assoc-in db [:kevyt-valinta :multiple-requests-count] new-multiple-requests-count))))
      {:http {:method              :get
              :path                (str "/lomake-editori/api/valintalaskentakoostepalvelu/valintaperusteet/hakukohde/"
                                        hakukohde-oid
                                        "/kayttaa-valintalaskentaa")
              :handler-or-dispatch :virkailija-kevyt-valinta/handle-fetch-valintalaskentakoostepalvelu-valintalaskenta-in-use?
              :override-args       {:error-handler #(re-frame/dispatch [:application/handle-fetch-application-error])}}})))

(re-frame/reg-event-fx
  :virkailija-kevyt-valinta/handle-fetch-valintalaskentakoostepalvelu-valintalaskenta-in-use?
  (fn [{db :db}
       [_
        {:keys [hakukohde-oid
                valintalaskenta]}]]
    (let [new-multiple-requests-count (some-> db :kevyt-valinta :multiple-requests-count dec)]
      (cond-> {:db (as-> db db'
                         (assoc-in db'
                                   [:application :valintalaskentakoostepalvelu hakukohde-oid :valintalaskenta]
                                   valintalaskenta)
                         (cond-> db'
                                 (= new-multiple-requests-count 0)
                                 (update :kevyt-valinta dissoc :multiple-requests-count)
                                 (> new-multiple-requests-count 0)
                                 (assoc-in [:kevyt-valinta :multiple-requests-count] new-multiple-requests-count)))}
              (= new-multiple-requests-count 0)
              (assoc :dispatch [:virkailija-kevyt-valinta/filter-applications])))))

(re-frame/reg-event-fx
  :virkailija-kevyt-valinta/fetch-valinnan-tulos-monelle
  (fn [{db                        :db} [_ {:keys [application-keys]}]]
    (let [ei-tulosta (set (doall (filter (fn [application-key] (not (contains? (get db :valinta-tulos-service) application-key))) application-keys)))
          new-multiple-requests-count (some-> db :kevyt-valinta :multiple-requests-count dec)]
      (if (empty? ei-tulosta)
        (cond-> {:dispatch [:virkailija-kevyt-valinta/filter-applications]}
                (= new-multiple-requests-count 0)
                (assoc
                  :db
                  (update db :kevyt-valinta dissoc :multiple-requests-count))
                (> new-multiple-requests-count 0)
                (assoc
                  :db
                  (assoc-in db [:kevyt-valinta :multiple-requests-count] new-multiple-requests-count)))
        {:db   db
         :http {:method              :post
                :path                "/lomake-editori/api/valinta-tulos-service/valinnan-tulos/hakemus"
                :params              ei-tulosta
                :handler-or-dispatch :virkailija-kevyt-valinta/handle-fetch-valinnan-tulos-monelle
                :handler-args        {:application-keys ei-tulosta}}}))))

(re-frame/reg-event-fx
  :virkailija-kevyt-valinta/fetch-valinnan-tulos
  (fn [{db                        :db} [_ {:keys [application-key memoize]}]]
    (if (and memoize
             (-> db :valinta-tulos-service (get application-key) not-empty))
      (let [new-multiple-requests-count (some-> db :kevyt-valinta :multiple-requests-count dec)]
        (cond-> {}
                (= new-multiple-requests-count 0)
                (assoc
                  :db
                  (update db :kevyt-valinta dissoc :multiple-requests-count))
                (> new-multiple-requests-count 0)
                (assoc
                  :db
                  (assoc-in db [:kevyt-valinta :multiple-requests-count] new-multiple-requests-count))))
      {:db   (update db :valinta-tulos-service dissoc application-key)
       :http {:method              :get
              :path                (str "/lomake-editori/api/valinta-tulos-service/valinnan-tulos/hakemus?hakemusOid=" application-key)
              :handler-or-dispatch :virkailija-kevyt-valinta/handle-fetch-valinnan-tulos
              :handler-args        {:application-key application-key}}})))

(def korkeakouluhaku-selected?
  (fn [db] (true? (some-> (get-in db [:haut (application/selected-haku-oid db)])
                          :kohdejoukko-uri
                          (string/starts-with? "haunkohdejoukko_12#")))))

(re-frame/reg-sub :resolve-korkeakouluhaku-selected (fn [db _] (korkeakouluhaku-selected? db)))

(defn- fake-vastaanottotila-for-deadline-passed
  [valinnan-tulos db]
  (let [tulos (:valinnantulos valinnan-tulos)]
    (cond->
      valinnan-tulos
      (and (korkeakouluhaku-selected? db)
           (:julkaistavissa tulos)
           (contains? #{:HYVAKSYTTY :VARASIJALTA_HYVAKSYTTY :PERUNUT}
                      (keyword (:valinnantila tulos)))
           (= "KESKEN" (:vastaanottotila tulos))
           (:vastaanottoDeadlineMennyt tulos))
      (assoc-in [:valinnantulos :vastaanottotila] "EI_VASTAANOTETTU_MAARA_AIKANA"))))

(re-frame/reg-event-fx
  :virkailija-kevyt-valinta/handle-fetch-valinnan-tulos-monelle
  (fn [{db :db} [_ response {application-keys :application-keys}]]
    (let [new-multiple-requests-count (some-> db :kevyt-valinta :multiple-requests-count dec)]
      (cond-> {:db (as-> db db'

                         (-> db'
                             (update :valinta-tulos-service ;pre-clear results for all affected hakemukses
                               (fn valinnan-tulokset->db [valinta-tulos-service-db]
                                 (->> application-keys
                                      (reduce (fn valinnan-tulos->db [acc key]
                                                (assoc acc key {}))
                                              valinta-tulos-service-db))))
                             (update
                               :valinta-tulos-service ;insert results
                               (fn valinnan-tulokset->db [valinta-tulos-service-db]
                                 (->> response
                                      (reduce (fn valinnan-tulos->db [acc valinnan-tulos]
                                                (let [hakemus-oid   (-> valinnan-tulos :valinnantulos :hakemusOid)
                                                      hakukohde-oid (-> valinnan-tulos :valinnantulos :hakukohdeOid)]
                                                  (assoc-in acc
                                                            [hakemus-oid hakukohde-oid]
                                                            (fake-vastaanottotila-for-deadline-passed
                                                              valinnan-tulos db'))))
                                              valinta-tulos-service-db)))))

                         (cond-> db'
                                 (= new-multiple-requests-count 0)
                                 (update :kevyt-valinta dissoc :multiple-requests-count)
                                 (> new-multiple-requests-count 0)
                                 (assoc-in [:kevyt-valinta :multiple-requests-count] new-multiple-requests-count)))}
              (= new-multiple-requests-count 0)
              (assoc :dispatch [:virkailija-kevyt-valinta/filter-applications])))))

(re-frame/reg-event-fx
  :virkailija-kevyt-valinta/handle-fetch-valinnan-tulos
  (fn [{db :db} [_ response {application-key :application-key}]]
    (let [new-multiple-requests-count (some-> db :kevyt-valinta :multiple-requests-count dec)]
      (cond-> {:db (as-> db db'

                         (-> db'
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
                                                            (fake-vastaanottotila-for-deadline-passed
                                                              valinnan-tulos db'))))
                                              valinta-tulos-service-db)))))

                         (cond-> db'
                                 (= new-multiple-requests-count 0)
                                 (update :kevyt-valinta dissoc :multiple-requests-count)
                                 (> new-multiple-requests-count 0)
                                 (assoc-in [:kevyt-valinta :multiple-requests-count] new-multiple-requests-count)))}
              (= new-multiple-requests-count 0)
              (assoc :dispatch [:virkailija-kevyt-valinta/filter-applications])))))

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
  (fn [{db                        :db}
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
              :path                (str  "/lomake-editori/api/valinta-tulos-service/valinnan-tulos/" valintatapajono-oid)
              :id                  request-id
              :override-args       {:params  request-body
                                    :headers {"If-Unmodified-Since" formatted-now}}
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
