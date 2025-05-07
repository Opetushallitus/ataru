(ns ataru.virkailija.virkailija-application-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.suoritus-filter :as suoritus-filter]
            [ataru.applications.synthetic-application-util :as synthetic-application-util]
            [ataru.hakija.hakija-form-service :as hakija-form-service]
            [ataru.hakija.validator :as validator]
            [ataru.log.audit-log :as audit-log]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.person-service.person-service :as person-service]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.util :as util]
            [taoensso.timbre :as log]
            [ataru.person-service.person-integration :as person-integration]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit]))


(defn get-opiskelijan-luokkatieto
  [henkilo-oid haku-oid hakemus-datetime koodisto-cache tarjonta-service
   organization-service ohjausparametrit-service person-service suoritus-service]
  (let [luokkatasot (suoritus-filter/luokkatasot-for-suoritus-filter)
        tarjonta-info (when haku-oid
                        (tarjonta-parser/parse-tarjonta-info-by-haku
                          koodisto-cache
                          tarjonta-service
                          organization-service
                          ohjausparametrit-service
                          haku-oid))
        linked-oids (get (person-service/linked-oids person-service [henkilo-oid]) henkilo-oid)
        aliases     (conj (:linked-oids linked-oids) (:master-oid linked-oids))
        opiskelijat (map #(suoritus-service/opiskelijan-luokkatieto-for-hakemus suoritus-service % luokkatasot hakemus-datetime tarjonta-info)
                         aliases)]
    (when-let [opiskelija (last (sort-by :alkupaiva opiskelijat))]
      (let [[organization] (organization-service/get-organizations-for-oids
                             organization-service [(:oppilaitos-oid opiskelija)])]
        {:oppilaitos-name (:name organization)
         :luokka          (:luokka opiskelija)}))))

(defn- uses-synthetic-applications?
  [ohjausparametrit-service haku-oid]
  (get (ohjausparametrit/get-parametri ohjausparametrit-service haku-oid) :synteettisetHakemukset))

(defn- synthetic-application-form-key
  [ohjausparametrit-service haku-oid]
  (get (ohjausparametrit/get-parametri ohjausparametrit-service haku-oid) :synteettisetLomakeavain))


(defn- store-synthetic-application [{:keys [application form applied-hakukohteet]}
                                    {:keys [session audit-logger job-runner person-service]}]
  (let [key-and-id (application-store/add-application application applied-hakukohteet form session audit-logger nil)
        person-oid (person-integration/upsert-person-synchronized job-runner person-service (:id key-and-id))]
    (log/info "Stored synthetic application with id" (:id key-and-id) ", oid " (:key key-and-id) " and person oid" person-oid)
    {:passed? true :hakemusOid (:key key-and-id) :personOid person-oid}))

(defn- validate-synthetic-application [application
                                       {:keys [form-by-id-cache
                                               koodisto-cache
                                               tarjonta-service
                                               organization-service
                                               ohjausparametrit-service
                                               audit-logger
                                               session]}]
  (let [tarjonta-info                 (when (:haku application)
                                        (tarjonta-parser/parse-tarjonta-info-by-haku
                                         koodisto-cache
                                         tarjonta-service
                                         organization-service
                                         ohjausparametrit-service
                                         (:haku application)))
        hakukohteet                   (get-in tarjonta-info [:tarjonta :hakukohteet])
        applied-hakukohteet           (filter #(contains? (set (:hakukohde application)) (:oid %))
                                              hakukohteet)
        applied-hakukohderyhmat       (set (mapcat :hakukohderyhmat applied-hakukohteet))
        form                          (when (:form application) (hakija-form-service/fetch-form-by-id
                                                                 (:form application)
                                                                 [:virkailija]
                                                                 form-by-id-cache
                                                                 koodisto-cache
                                                                 nil
                                                                 false
                                                                 {}))
        validation-result             (when form (validator/valid-application?
                                                  koodisto-cache
                                                  false ; TODO: has-applied OK?
                                                  application
                                                  form
                                                  applied-hakukohderyhmat
                                                  true
                                                  "NEW_APPLICATION_ID"
                                                  "NEW_APPLICATION_KEY"))
        result (cond
                 (and (:haku application)
                      (not (uses-synthetic-applications? ohjausparametrit-service (:haku application))))
                 {:passed? false
                  :failures ["Synthetic applications not enabled for haku"]
                  :code :internal-server-error}

                 (not (:form application))
                 {:passed? false
                  :failures ["Synthetic form key not defined for haku"]
                  :code :internal-server-error}

                 (not form)
                 {:passed? false
                  :failures ["Synthetic form was not found with form key"]
                  :code :internal-server-error}

                 (and (:haku application)
                      (empty? (:hakukohde application)))
                 {:passed? false
                  :failures ["Hakukohde must be specified"]
                  :code :internal-server-error}

                 (true? (get-in form [:properties :closed] false))
                 {:passed? false
                  :failures ["Form is closed"]
                  :code :form-closed}

                 (not (:passed? validation-result))
                 validation-result

                 :else
                 {:passed? true
                  :application application
                  :form form
                  :applied-hakukohteet applied-hakukohteet})]
    (if (:passed? result)
      result
      (do
        (audit-log/log audit-logger
                       {:new       application
                        :operation audit-log/operation-failed
                        :session   session
                        :id        {:email (util/extract-email application)}})
        (log/warn "Synthetic application failed verification" result)
        result))))

(defn- convert-synthetic-application
  [application {:keys [ohjausparametrit-service]}]
  (let [haku-oid (:hakuOid application)
        form-id (hakija-form-service/latest-form-id-by-key (synthetic-application-form-key ohjausparametrit-service haku-oid))
        converted (synthetic-application-util/synthetic-application->application application form-id)]
    (log/info "Synthetic application submitted and converted" converted)
    converted))

(defn batch-submit-synthetic-applications
  [applications data]
  (let [converted-applications (map #(convert-synthetic-application % data) applications)
        validation-results     (map #(validate-synthetic-application % data) converted-applications)
        all-applications-valid (not-any? #(= false (:passed? %)) validation-results)]
    (if all-applications-valid
      {:success true :applications (doall (map #(store-synthetic-application % data) validation-results))}
      {:success false :applications validation-results})))
