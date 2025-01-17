(ns ataru.fixtures.db.unit-test-db
  (:require [yesql.core :refer [defqueries]]
            [ataru.forms.form-store :as form-store]
            [ataru.cas-oppija.cas-oppija-session-store :as oss]
            [ataru.applications.application-store :as application-store]
            [ataru.config.core :refer [config]]
            [ataru.db.db :as ataru-db]
            [ataru.log.audit-log :as audit-log]))

; Make linter happy again
(declare yesql-delete-fixture-application-review!)
(declare yesql-delete-fixture-application-events!)
(declare yesql-delete-fixture-application-secrets!)
(declare yesql-delete-fixture-application-answers!)
(declare yesql-delete-fixture-application-multi-answers!)
(declare yesql-delete-fixture-application-group-answers!)
(declare yesql-delete-fixture-application!)
(declare yesql-delete-fixture-form!)
(declare yesql-delete-fixture-forms-with-key!)
(declare yesql-set-form-id!)
(declare yesql-delete-kk-payments-history!)
(declare yesql-delete-kk-payments!)

(defqueries "sql/dev-form-queries.sql")

(defn- nuke-old-fixture-data [form-id]
  (ataru-db/exec :db yesql-delete-fixture-application-review! {:form_id form-id})
  (ataru-db/exec :db yesql-delete-fixture-application-events! {:form_id form-id})
  (ataru-db/exec :db yesql-delete-fixture-application-secrets! {:form_id form-id})
  (ataru-db/exec :db yesql-delete-fixture-application-answers! {:form_id form-id})
  (ataru-db/exec :db yesql-delete-fixture-application-multi-answers! {:form_id form-id})
  (ataru-db/exec :db yesql-delete-fixture-application-group-answers! {:form_id form-id})
  (ataru-db/exec :db yesql-delete-fixture-application! {:form_id form-id})
  (ataru-db/exec :db yesql-delete-fixture-form! {:id form-id}))

(defn nuke-old-fixture-forms-with-key [form-key]
  (ataru-db/exec :db yesql-delete-fixture-forms-with-key! {:key form-key}))

; NB: has to be done in this order, otherwise deleted rows from payments get to history...
(defn nuke-kk-payment-data []
  (ataru-db/exec :db yesql-delete-kk-payments! {})
  (ataru-db/exec :db yesql-delete-kk-payments-history! {}))

(defn init-db-form-fixture
  [form-fixture]
  (let [{:keys [id] :as form}
        (if (some? (:key form-fixture))
          (form-store/create-new-form! form-fixture (:key form-fixture))
          (form-store/create-new-form! form-fixture))]
    (ataru-db/exec :db yesql-set-form-id! {:old_id id :new_id (:id form-fixture)})
    form))

(defn init-db-application-fixture
  [form-fixture application-fixture application-hakukohde-reviews-fixture application-reviews-fixture]
  (when (or (nil? (:id form-fixture)) (not= (:id form-fixture) (:form application-fixture)))
    (throw (Exception. (str "Incorrect fixture data, application should refer the given form"))))
  (let [audit-logger       (audit-log/new-dummy-audit-logger)
        _                  (init-db-form-fixture form-fixture)
        application-id     (-> (application-store/add-application
                               application-fixture
                               (:hakukohde application-fixture)
                               form-fixture
                               {}
                               audit-logger
                               nil)
                               :id)
        stored-application (application-store/get-application application-id)]
    (doseq [{hakukohde :hakukohde review-requirement :review-requirement review-state :review-state}
            application-hakukohde-reviews-fixture]
      (application-store/save-application-hakukohde-review
       (:key stored-application) hakukohde review-requirement review-state {} audit-logger))
    (doseq [review application-reviews-fixture]
      (application-store/save-application-review
        (merge review {:application-key (:key stored-application)}) {} audit-logger))
    application-id))

(defn init-db-multi-application-fixture
  [form-fixture application-fixtures]
  (when (or (nil? (:id form-fixture)) (some #(not= (:id form-fixture) (:form %)) application-fixtures))
    (throw (Exception. (str "Incorrect fixture data, application should refer the given form"))))
  (let [audit-logger       (audit-log/new-dummy-audit-logger)
        _                  (init-db-form-fixture form-fixture)]
    (doall (map (fn [application-fixture] (-> (application-store/add-application
                                                application-fixture
                                                (:hakukohde application-fixture)
                                                form-fixture
                                                {}
                                                audit-logger
                                                nil)
                                              :id)) application-fixtures))))

(defn init-db-fixture
  ([form-fixture]
    (nuke-old-fixture-data (:id form-fixture))
    (init-db-form-fixture form-fixture))
  ([form-fixture application-fixtures]
   (nuke-old-fixture-data (:id form-fixture))
   (init-db-multi-application-fixture form-fixture application-fixtures))
  ([form-fixture application-fixture application-hakukohde-reviews-fixture]
    (nuke-old-fixture-data (:id form-fixture))
    (init-db-application-fixture form-fixture application-fixture application-hakukohde-reviews-fixture nil))
  ([form-fixture application-fixture application-hakukohde-reviews-fixture application-reviews-fixture]
   (nuke-old-fixture-data (:id form-fixture))
   (init-db-application-fixture form-fixture application-fixture
                                application-hakukohde-reviews-fixture application-reviews-fixture)))

(defn init-oppija-session-to-db
  [ticket data]
  (oss/persist-session! (oss/generate-new-random-key) ticket data))

(defn clear-database []
                     (ataru-db/clear-db! :db (get-in config [:db :schema])))
