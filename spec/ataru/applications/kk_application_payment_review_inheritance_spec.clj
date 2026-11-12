(ns ataru.applications.kk-application-payment-review-inheritance-spec
  (:require [ataru.applications.application-store :as application-store]
            [ataru.component-data.kk-application-payment-module :as payment-module]
            [ataru.db.db :as db]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [ataru.kk-application-payment.kk-application-payment-permit-check :as permit-check]
            [ataru.log.audit-log :as audit-log]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.test-utils :as test-utils]
            [ataru.time :as time]
            [clojure.java.jdbc :as jdbc]
            [speclj.core :refer [after-all describe it should= should-be-nil tags before]]
            [yesql.core :as sql]))

(sql/defqueries "sql/virkailija-queries.sql")
(declare yesql-upsert-virkailija<!)

(def audit-logger (audit-log/new-dummy-audit-logger))
(def fake-organization-service (organization-service/->FakeOrganizationService))

(def hakukohde-1 "payment-info-test-kk-hakukohde")
(def hakukohde-2 "payment-info-test-kk-hakukohde-2")
(def hakukohde-3 "payment-info-test-kk-hakukohde-3")

(def earlier-time (time/date-time 2025 1 5 12 0))
(def reviewed-time (time/date-time 2025 1 10 12 0))

(def form
  (merge form-fixtures/payment-exemption-test-form
         {:id      909911
          :key     "kk-payment-review-inheritance-form"
          :content [(payment-module/kk-application-payment-module)]}))

(defn- init-application [hakukohteet]
  (unit-test-db/init-db-fixture form)
  (-> (application-store/add-application (assoc application-fixtures/application-with-hakemusmaksu-exemption
                                                :form (:id form)
                                                :hakukohde hakukohteet)
                                         (map (fn [oid] {:oid oid}) hakukohteet)
                                         form
                                         {}
                                         audit-logger
                                         nil)
      :id
      application-store/get-application))

(defn- latest-application [application-key]
  (application-store/get-application (:id (application-store/get-latest-application-by-key application-key))))

(defn- review-attachment [application-key hakukohde state modified-time]
  (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                      :attachment_key  "passport-attachment"
                                      :hakukohde       hakukohde
                                      :state           state
                                      :modified_time   modified-time
                                      :updated?        true}]))

(defn- review-obligation [application-key hakukohde state modified-time]
  (application-store/save-application-hakukohde-review application-key
                                                       hakukohde
                                                       "kk-application-payment-obligation"
                                                       state
                                                       modified-time
                                                       {}
                                                       audit-logger))

(defn- edit-hakukohteet
  ([application hakukohteet]
   (edit-hakukohteet application hakukohteet identity {}))
  ([application hakukohteet update-answers-fn session]
   (application-store/update-application
     (-> application
         (assoc :hakukohde hakukohteet)
         (update :answers update-answers-fn))
     (map (fn [oid] {:oid oid}) hakukohteet)
     form
     session
     audit-logger
     nil)))

(defn- attachment-review [application-key hakukohde]
  (->> (application-store/get-application-attachment-reviews application-key false)
       (filter #(and (= "passport-attachment" (:attachment-key %)) (= hakukohde (:hakukohde %))))
       first))

(defn- obligation-review [application-key hakukohde]
  (->> (payment/get-kk-application-payment-obligation-reviews application-key)
       (filter #(= hakukohde (:hakukohde %)))
       first))

(describe "kk application payment reviews of a hakukohde added to the application"
  (tags :unit)

  (before
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
      (jdbc/delete! conn :kk_application_payment_permit_checks [])
      (jdbc/delete! conn :application_hakukohde_reviews [])
      (jdbc/delete! conn :application_hakukohde_attachment_reviews [])))

  ; The applications of the form share hakukohteet with other specs
  (after-all
    (unit-test-db/init-db-fixture form))

  (it "inherits the payment module attachment reviews and the payment obligation review with their modified times"
    (let [application     (init-application [hakukohde-1])
          application-key (:key application)]
      (review-attachment application-key hakukohde-1 "checked" reviewed-time)
      (review-obligation application-key hakukohde-1 "reviewed" reviewed-time)
      (edit-hakukohteet application [hakukohde-1 hakukohde-2])
      (should= {:state "checked" :modified-time reviewed-time}
               (select-keys (attachment-review application-key hakukohde-2) [:state :modified-time]))
      (should= {:state "reviewed" :modified-time reviewed-time}
               (select-keys (obligation-review application-key hakukohde-2) [:state :modified-time]))
      (should= {:state "checked" :modified-time reviewed-time}
               (select-keys (attachment-review application-key hakukohde-1) [:state :modified-time]))
      (should= {:state "reviewed" :modified-time reviewed-time}
               (select-keys (obligation-review application-key hakukohde-1) [:state :modified-time]))))

  (it "does not take an inherited unchecked attachment for a new one when resetting the payment obligation review"
    (let [application     (init-application [hakukohde-1])
          application-key (:key application)]
      (review-attachment application-key hakukohde-1 "not-checked" earlier-time)
      (review-obligation application-key hakukohde-1 "reviewed" reviewed-time)
      (edit-hakukohteet application [hakukohde-1 hakukohde-2])
      ; the attachment deadline is not checked, as the inherited attachment is older than the review
      (should= {:state "not-checked" :modified-time earlier-time}
               (select-keys (attachment-review application-key hakukohde-2) [:state :modified-time]))))

  (it "inherits the reviews when virkailija adds the hakukohde"
    (let [application     (init-application [hakukohde-1])
          application-key (:key application)]
      (review-attachment application-key hakukohde-1 "checked" reviewed-time)
      (review-obligation application-key hakukohde-1 "reviewed" reviewed-time)
      (edit-hakukohteet (-> application
                            (assoc :virkailija-secret (test-utils/create-fake-virkailija-rewrite-secret application-key))
                            (dissoc :secret))
                        [hakukohde-1 hakukohde-2]
                        identity
                        {:identity {:oid "1.2.246.562.24.00000001213"}})
      (should= "checked" (:state (attachment-review application-key hakukohde-2)))
      (should= "reviewed" (:state (obligation-review application-key hakukohde-2)))))

  (it "inherits the latest reviews when the hakukohteet have been reviewed differently"
    (let [application     (init-application [hakukohde-1 hakukohde-3])
          application-key (:key application)]
      (review-attachment application-key hakukohde-1 "checked" earlier-time)
      (review-attachment application-key hakukohde-3 "incomplete-attachment" reviewed-time)
      (review-obligation application-key hakukohde-1 "reviewed" earlier-time)
      (review-obligation application-key hakukohde-3 "unreviewed" reviewed-time)
      (edit-hakukohteet application [hakukohde-1 hakukohde-3 hakukohde-2])
      (should= "incomplete-attachment" (:state (attachment-review application-key hakukohde-2)))
      (should= "unreviewed" (:state (obligation-review application-key hakukohde-2)))))

  (it "inherits the reviews when a hakukohde is replaced with another one"
    (let [application     (init-application [hakukohde-1])
          application-key (:key application)]
      (review-attachment application-key hakukohde-1 "checked" reviewed-time)
      (review-obligation application-key hakukohde-1 "reviewed" reviewed-time)
      (edit-hakukohteet application [hakukohde-2])
      (should= "checked" (:state (attachment-review application-key hakukohde-2)))
      (should= "reviewed" (:state (obligation-review application-key hakukohde-2)))))

  (it "replaces the reviews left from an earlier time a hakukohde added again was applied to"
    (let [application     (init-application [hakukohde-1 hakukohde-2])
          application-key (:key application)]
      (review-attachment application-key hakukohde-1 "checked" earlier-time)
      (review-attachment application-key hakukohde-2 "checked" earlier-time)
      (review-obligation application-key hakukohde-1 "reviewed" earlier-time)
      (review-obligation application-key hakukohde-2 "reviewed" earlier-time)
      (edit-hakukohteet application [hakukohde-1])
      (review-attachment application-key hakukohde-1 "incomplete-attachment" reviewed-time)
      (review-obligation application-key hakukohde-1 "unreviewed" reviewed-time)
      (edit-hakukohteet (latest-application application-key) [hakukohde-1 hakukohde-2])
      (should= {:state "incomplete-attachment" :modified-time reviewed-time}
               (select-keys (attachment-review application-key hakukohde-2) [:state :modified-time]))
      (should= {:state "unreviewed" :modified-time reviewed-time}
               (select-keys (obligation-review application-key hakukohde-2) [:state :modified-time]))))

  (it "updates the modified time of a review left in the same state for a hakukohde added again"
    (let [application      (init-application [hakukohde-1 hakukohde-2])
          application-key  (:key application)
          not-checked-time (time/date-time 2025 1 7 12 0)]
      (review-obligation application-key hakukohde-1 "reviewed" earlier-time)
      (review-obligation application-key hakukohde-2 "reviewed" earlier-time)
      (edit-hakukohteet application [hakukohde-1])
      (review-attachment application-key hakukohde-1 "not-checked" not-checked-time)
      (review-obligation application-key hakukohde-1 "unreviewed" reviewed-time)
      (review-obligation application-key hakukohde-1 "reviewed" reviewed-time)
      (edit-hakukohteet (latest-application application-key) [hakukohde-1 hakukohde-2])
      (should= {:state "reviewed" :modified-time reviewed-time}
               (select-keys (obligation-review application-key hakukohde-2) [:state :modified-time]))
      (should= {:state "not-checked" :modified-time not-checked-time}
               (select-keys (attachment-review application-key hakukohde-2) [:state :modified-time]))))

  (it "does not inherit anything when the previous hakukohteet have no reviews"
    (let [application     (init-application [hakukohde-1])
          application-key (:key application)]
      (edit-hakukohteet application [hakukohde-1 hakukohde-2])
      (should= "attachment-missing" (:state (attachment-review application-key hakukohde-2)))
      (should-be-nil (obligation-review application-key hakukohde-2))))

  (it "does not inherit the review of an attachment changed in the same edit"
    (let [application     (init-application [hakukohde-1])
          application-key (:key application)]
      (review-attachment application-key hakukohde-1 "checked" reviewed-time)
      (review-obligation application-key hakukohde-1 "reviewed" reviewed-time)
      (edit-hakukohteet application
                        [hakukohde-1 hakukohde-2]
                        #(conj % {:key       "passport-attachment"
                                  :fieldType "attachment"
                                  :value     ["new-attachment-key"]})
                        {})
      (should= "not-checked" (:state (attachment-review application-key hakukohde-1)))
      (should= "not-checked" (:state (attachment-review application-key hakukohde-2)))
      (should= "reviewed" (:state (obligation-review application-key hakukohde-2)))))

  (it "keeps the residence permit checks, which are not specific to a hakukohde"
    (let [application     (init-application [hakukohde-1])
          application-key (:key application)
          virkailija-oid  "1.2.246.562.24.00000001213"]
      (db/exec :db yesql-upsert-virkailija<! {:oid        virkailija-oid
                                              :first_name "Hemuli"
                                              :last_name  "Hemuli?"})
      (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
        (jdbc/insert! conn :kk_application_payment_permit_checks
                      {:application_key          application-key
                       :attachment_key           "continuous-residence-permit-front"
                       :payment_obligation_basis "not-obligated"
                       :virkailija_oid           virkailija-oid
                       :checked_time             (java.sql.Timestamp. (System/currentTimeMillis))}))
      (edit-hakukohteet application [hakukohde-1 hakukohde-2])
      (should= ["continuous-residence-permit-front"]
               (map :attachment-key (permit-check/get-permit-checks fake-organization-service application-key))))))
