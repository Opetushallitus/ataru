(ns ataru.kk-application-payment.kk-application-payment-permit-check
  "Virkailijan merkinnät hakemusmaksun oleskelulupaliitteistä: löytyikö lupa Migrin palvelusta, lupakortin
   voimassaolo ja maksuvelvollisuuden peruste."
  (:require [ataru.applications.application-access-control :as aac]
            [ataru.applications.application-store :as application-store]
            [ataru.component-data.kk-application-payment-module :as payment-module]
            [ataru.db.db :as db]
            [ataru.kk-application-payment.kk-application-payment-permit-check-store :as store]
            [ataru.log.audit-log :as audit-log]
            [ataru.organization-service.organization-client :as org-client]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.time :as time]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [taoensso.timbre :as log]))

(def ^:private oph-name
  {:fi "Opetushallitus"
   :sv "Utbildningsstyrelsen"
   :en "Finnish National Agency for Education"})

(defn- ->public
  "Tarkistuksen tekijä näkyy hakemuksen tapahtumissa, joissa virkailijan oidia ei myöskään näytetä. Perityn
   tarkistuksen tekijä näytetään alkuperäisen tarkistuksen tiedoissa, koska alkuperäisen hakemuksen tapahtumat eivät
   näy."
  [check]
  (dissoc check :virkailija-oid :virkailija-first-name :virkailija-last-name :organization-oid))

(defn- oppilaitos
  "Organisaation ja sen yläorganisaatioiden (ks. get-organization-with-parents) oppilaitostaso, esim. toimipisteen
   oppilaitos."
  [organizations]
  (last (filter #(contains? (:types %) :oppilaitos) organizations)))

(defn- organization-to-show
  "Oppilaitos, jossa tarkistus tehtiin, tai OPH. Nil, kun oppilaitosta ei tiedetä."
  [organization-service organization-oid]
  (cond
    (nil? organization-oid)
    nil

    (= org-client/oph-organization organization-oid)
    {:oid organization-oid :name oph-name}

    :else
    (try
      (some-> (organization-service/get-organization-with-parents organization-service organization-oid)
              oppilaitos
              (select-keys [:oid :name]))
      (catch Exception e
        (log/error e "Could not get the organization" organization-oid)
        nil))))

(defn- with-original-check
  [organization-service check]
  (cond-> (->public check)
          (some? (:inherited-from-application-key check))
          (assoc :original-check {:application-key (:inherited-from-application-key check)
                                  :checked-time    (:checked-time check)
                                  :virkailija-name (string/join " " (remove string/blank?
                                                                            [(:virkailija-first-name check)
                                                                             (:virkailija-last-name check)]))
                                  :organization    (organization-to-show organization-service
                                                                         (:organization-oid check))})))

(defn get-permit-checks
  [organization-service application-key]
  (mapv (partial with-original-check organization-service) (store/get-permit-checks application-key)))

(defn- authorized-organization-oid
  "Sen hakukohteen oppilaitos, joka on hakutoivejärjestyksessä ensimmäinen, johon virkailijalla on oikeus
   liitosorganisaationsa (muokkausoikeus) tai valitsemansa organisaation kautta. Organisaatio on hakukohteen tarjoaja
   tai jokin sen yläorganisaatioista.
   - Organisaation sijaan tallennetaan oppilaitos, esim. kun virkailija on liitetty toimipisteeseen tai
     koulutustoimijaan.
   - OPH, kun pääkäyttäjä ei ole valinnut organisaatiota tai virkailija on liitetty OPH:hon.
   - Nil, kun oikeus tulee vain lomakkeen organisaation tai hakukohderyhmän kautta tai hakukohteella ei ole
     oppilaitosta, koska korkeakoulua ei silloin voi päätellä. Nil myös, kun organisaatioita ei saada haettua, koska
     se ei saa estää tarkistuksen tallentamista."
  [organization-service tarjonta-service session application-key]
  (let [selected-organization (:selected-organization session)]
    (if (and (-> session :identity :superuser) (nil? selected-organization))
      org-client/oph-organization
      (try
        (let [attached-oids  (if selected-organization
                               #{(:oid selected-organization)}
                               (->> (get-in session [:identity :user-right-organizations :edit-applications])
                                    (map :oid)
                                    set))
              hakukohde-oids (some-> (application-store/applications-authorization-data [application-key])
                                     first
                                     :hakukohde)
              hakukohteet    (when (seq hakukohde-oids)
                               (let [by-oid (->> (tarjonta/get-hakukohteet tarjonta-service hakukohde-oids)
                                                 (map (juxt :oid identity))
                                                 (into {}))]
                                 (keep by-oid hakukohde-oids)))]
          (or (->> hakukohteet
                   (mapcat :tarjoaja-oids)
                   (keep (fn [tarjoaja-oid]
                           (let [organizations (organization-service/get-organization-with-parents organization-service
                                                                                                   tarjoaja-oid)]
                             (when (some (comp attached-oids :oid) organizations)
                               (:oid (oppilaitos organizations))))))
                   first)
              ; Yläorganisaatioihin ei kuulu OPH
              (attached-oids org-client/oph-organization)))
        (catch Exception e
          (log/error e "Could not determine the organization of the virkailija for application" application-key)
          nil)))))

; Päivän joustovara, koska selain tarkistaa rajat oman päivämääränsä mukaan
(defn- permit-valid-until-in-range?
  [permit-valid-until]
  (or (nil? permit-valid-until)
      (let [date  (java.time.LocalDate/parse permit-valid-until)
            today (time/today (time/time-zone-for-id "Europe/Helsinki"))]
        (not (or (.isBefore date (-> today
                                     (.minusYears payment-module/kk-payment-permit-valid-until-years-back)
                                     (.minusDays 1)))
                 (.isAfter date (-> today
                                    (.plusYears payment-module/kk-payment-permit-valid-until-years-ahead)
                                    (.plusDays 1))))))))

(defn- normalize-body
  [{:keys [payment-obligation-basis payment-obligation-other-reason] :as body}]
  (assoc body :payment-obligation-other-reason
              (when (= "obligated-other" payment-obligation-basis)
                (some-> payment-obligation-other-reason string/trim not-empty))))

(defn- add-event!
  [connection event-type application-key attachment-key session]
  (application-store/add-application-event-in-tx connection
                                                 {:application-key application-key
                                                  :event-type      event-type
                                                  :review-key      attachment-key}
                                                 session))

(defn save-permit-check
  "Palauttaa tallennetun tarkistuksen, :not-found, :unauthorized, :bad-request (liite ei ole oleskelulupaliite)
   tai :invalid-permit-valid-until (lupakortin voimassaolopäivä on sallittujen rajojen ulkopuolella)."
  [organization-service tarjonta-service audit-logger session application-key attachment-key body]
  (cond
    (empty? (application-store/applications-authorization-data [application-key]))
    :not-found

    (not (aac/applications-access-authorized?
           organization-service
           tarjonta-service
           session
           [application-key]
           [:edit-applications]))
    :unauthorized

    (not (payment-module/residence-permit-attachment? attachment-key))
    :bad-request

    ; Vain muuttunut päivämäärä tarkistetaan, jotta aiemmin tallennettu, sittemmin rajojen ulkopuolelle jäänyt
    ; päivämäärä ei estä tallentamista
    (not (or (permit-valid-until-in-range? (:permit-valid-until body))
             (= (:permit-valid-until body)
                (:permit-valid-until (store/get-permit-check application-key attachment-key)))))
    :invalid-permit-valid-until

    :else
    (let [{:keys [found-in-migri permit-valid-until payment-obligation-basis payment-obligation-other-reason]}
          (normalize-body body)
          organization-oid (authorized-organization-oid organization-service tarjonta-service session application-key)
          [old new]        (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                             (let [old (store/get-permit-check connection application-key attachment-key)
                                   new (store/upsert-permit-check!
                                         connection
                                         {:application_key                 application-key
                                          :attachment_key                  attachment-key
                                          :found_in_migri                  found-in-migri
                                          :permit_valid_until              permit-valid-until
                                          :payment_obligation_basis        payment-obligation-basis
                                          :payment_obligation_other_reason payment-obligation-other-reason
                                          :virkailija_oid                  (-> session :identity :oid)
                                          :organization_oid                organization-oid})]
                               (add-event! connection "kk-payment-permit-check-updated" application-key attachment-key
                                           session)
                               [old new]))]
      (audit-log/log audit-logger
                     (cond-> {:new       new
                              :id        {:applicationOid application-key
                                          :attachmentKey  attachment-key}
                              :session   session
                              :operation (if old audit-log/operation-modify audit-log/operation-new)}
                             ; virkailija yksilöidään oidilla
                             old (assoc :old (dissoc old :virkailija-first-name :virkailija-last-name))))
      (->public new))))

(defn reset-permit-checks
  "Poistaa hakemuksen tarkistukset, jotka on tallennettu viimeistään hetkellä modified-until, esim. kun
   maksuvelvollinen hakija toimittaa uuden vapautusliitteen ja maksuvelvollisuus pitää tarkastaa uudelleen. Palauttaa
   poistetut tarkistukset, jotta ne voidaan kirjata audit-lokiin transaktion jälkeen."
  [connection application-key modified-until]
  (let [removed (store/delete-permit-checks! connection application-key modified-until)]
    (doseq [check removed]
      (log/info "Removing kk application payment permit check for application" application-key
                ", attachment" (:attachment-key check))
      (add-event! connection "kk-payment-permit-check-reset" application-key (:attachment-key check) nil))
    removed))

(defn audit-log-reset
  [audit-logger application-key removed]
  (doseq [check removed]
    (audit-log/log audit-logger
                   {:old       check
                    :id        {:applicationOid application-key
                                :attachmentKey  (:attachment-key check)}
                    :session   {:cronjob "reset-kk-application-payment-permit-checks"}
                    :operation audit-log/operation-delete})))
