(ns ataru.virkailija.virkailija-application-service 
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.synthetic-application-util :as synthetic-application-util]
            [ataru.hakija.hakija-form-service :as hakija-form-service]
            [ataru.hakija.validator :as validator]
            [ataru.log.audit-log :as audit-log]
            [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
            [ataru.util :as util]
            [taoensso.timbre :as log]))

(defn- store-and-log [application applied-hakukohteet form is-modify? session audit-logger]
  {:pre [(boolean? is-modify?)]}
  (let [store-fn (if is-modify? application-store/update-application application-store/add-application)
        key-and-id (store-fn application applied-hakukohteet form session audit-logger)]
    (log/info "Stored synthetic application with id" (:id key-and-id))
    {:passed?        true
     :id (:id key-and-id)}))

 ; TODO add reference to inserted row as "key"?
(defn validate-and-store [form-by-id-cache
                          koodisto-cache
                          tarjonta-service
                          organization-service
                          ohjausparametrit-service
                          audit-logger
                          application
                          is-modify?
                          session]
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
        final-application             application
        validation-result             (validator/valid-application?
                                       koodisto-cache
                                       false ; TODO: has-applied OK?
                                       final-application
                                       form
                                       applied-hakukohderyhmat
                                       true
                                       "NEW_APPLICATION_ID"
                                       "NEW_APPLICATION_KEY")]
    (cond
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
      (assoc (store-and-log final-application applied-hakukohteet form is-modify? session audit-logger)
             :key (:key nil)))))

(defn handle-synthetic-application-submit
  [form-by-id-cache
   koodisto-cache
   tarjonta-service
   organization-service
   ohjausparametrit-service
   audit-logger
   synthetic-application
   session]
  (log/info "Synthetic application submitted" synthetic-application)
  (let [form-id (hakija-form-service/latest-form-id-by-haku-oid (:hakuOid synthetic-application) tarjonta-service)
        application (synthetic-application-util/synthetic-application->application synthetic-application form-id)
        result (validate-and-store form-by-id-cache
                            koodisto-cache
                            tarjonta-service
                            organization-service
                            ohjausparametrit-service
                            audit-logger
                            application
                            false
                            session)]
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
