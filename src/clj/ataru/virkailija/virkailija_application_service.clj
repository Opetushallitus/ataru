(ns ataru.virkailija.virkailija-application-service 
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.synthetic-application-util :as synthetic-application-util]
            [ataru.hakija.hakija-form-service :as hakija-form-service]
            [ataru.hakija.validator :as validator]
            [ataru.log.audit-log :as audit-log]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.util :as util]
            [taoensso.timbre :as log]
            [ataru.person-service.person-integration :as person-integration]))

(defn- store-synthetic-application [application {:keys [session
                                                        audit-logger
                                                        job-runner
                                                        person-service
                                                        koodisto-cache
                                                        tarjonta-service
                                                        organization-service
                                                        ohjausparametrit-service
                                                        form-by-id-cache]}]
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
        form                          (hakija-form-service/fetch-form-by-id
                                       (:form application)
                                       [:virkailija] ; TODO is this the correct role?
                                       form-by-id-cache
                                       koodisto-cache
                                       nil
                                       false
                                       {}
                                       false)
        key-and-id (application-store/add-application application applied-hakukohteet form session audit-logger)
        person-oid (person-integration/upsert-person-synchronized job-runner person-service (:id key-and-id))]
    (log/info "Stored synthetic application with id" (:id key-and-id) "and person oid" person-oid)
    {:passed? true :id (:id key-and-id) :personOid person-oid}))

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
        form                          (hakija-form-service/fetch-form-by-id
                                       (:form application)
                                       [:virkailija] ; TODO is this the correct role?
                                       form-by-id-cache
                                       koodisto-cache
                                       nil
                                       false
                                       {}
                                       false)
        validation-result             (validator/valid-application?
                                       koodisto-cache
                                       false ; TODO: has-applied OK?
                                       application
                                       form
                                       applied-hakukohderyhmat
                                       true
                                       "NEW_APPLICATION_ID"
                                       "NEW_APPLICATION_KEY")
        result (cond
                 (and (:haku application)
                      (empty? (:hakukohde application)))
                 {:passed? false
                  :failures ["Hakukohde must be specified"]
                  :key  (:key nil)
                  :code :internal-server-error}

                 (true? (get-in form [:properties :closed] false))
                 {:passed? false
                  :failures ["Form is closed"]
                  :key (:key nil)
                  :code :form-closed}

                 (not (:passed? validation-result))
                 (assoc validation-result :key (:key nil))

                 :else
                 {:passed? true})]
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
  [application {:keys [tarjonta-service]}]
  (let [form-id (hakija-form-service/latest-form-id-by-haku-oid (:hakuOid application) tarjonta-service)
        converted (synthetic-application-util/synthetic-application->application application form-id)]
    (log/info "Synthetic application submitted and converted" converted)
    converted))

(defn batch-submit-synthetic-applications
  [applications data]
  (let [converted-applications (map #(convert-synthetic-application % data) applications)
        validation-results     (map #(validate-synthetic-application % data) converted-applications)
        all-applications-valid (not-any? #(= false (:passed? %)) validation-results)]
    (if all-applications-valid
      {:success true :applications (doall (map #(store-synthetic-application % data) converted-applications))}
      {:success false :applications validation-results})))
