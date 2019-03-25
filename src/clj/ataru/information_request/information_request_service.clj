(ns ataru.information-request.information-request-service
  (:require [ataru.config.core :refer [config]]
            [ataru.db.db :as db]
            [ataru.log.audit-log :as audit-log]
            [ataru.translations.translation-util :as translations]
            [ataru.util :as u]
            [ataru.email.application-email-confirmation :refer [->safe-html]]
            [ataru.information-request.information-request-job :as information-request-job]
            [ataru.information-request.information-request-store :as information-request-store]
            [ataru.applications.application-store :as app-store]
            [clojure.java.jdbc :as jdbc]
            [selmer.parser :as selmer]
            [ataru.background-job.job :as job]
            [taoensso.timbre :as log]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]))

(defn- extract-answer-value [answer-key-str application]
  (->> (:answers application)
       (filter (comp (partial = answer-key-str) :key))
       (map :value)
       (first)))

(defn- initial-state [information-request]
  (let [application-id  (app-store/add-new-secret-to-application (:application-key information-request))
        application     (app-store/get-application application-id)
        lang            (-> application :lang keyword)
        recipient-email (extract-answer-value "email" application)
        translations    (translations/get-translations lang)
        service-url     (get-in config [:public-config :applicant :service_url])
        application-url (str service-url "/hakemus?modify=" (:secret application))
        body            (selmer/render-file "templates/information-request-template.html"
                                            (merge {:message         (->safe-html (:message information-request))
                                                    :application-url application-url}
                                                   translations))]
    (-> (select-keys information-request [:subject :application-key :id])
        (merge {:from       "no-reply@opintopolku.fi"
                :recipients [recipient-email]
                :body       body}))))

(defn- start-email-job [job-runner connection information-request]
  (let [initial-state (initial-state information-request)
        job-type      (:type information-request-job/job-definition)
        job-id        (job/start-job job-runner
                                     connection
                                     job-type
                                     initial-state)]
    (log/info (str "Started information request email job with job id " job-id ", initial state: " initial-state))))

(defn store [information-request virkailija-oid job-runner]
  {:pre [(-> information-request :subject u/not-blank?)
         (-> information-request :message u/not-blank?)
         (-> information-request :application-key u/not-blank?)
         (-> information-request :message-type u/not-blank?)]}
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [information-request (information-request-store/add-information-request
                               information-request
                               virkailija-oid
                               conn)]
      (start-email-job job-runner conn information-request)
      (audit-log/log {:new       information-request
                      :operation audit-log/operation-new
                      :id        virkailija-oid})
      information-request)))

(defn mass-information-request-job-step
  [state job-runner]
  (if (empty? (:information-requests state))
    {:transition {:id :final}}
    (let [[ir & irs] (:information-requests state)]
      (store ir (:virkailija-oid state) job-runner)
      {:transition    {:id :to-next :step :initial}
       :updated-state {:information-requests irs
                       :virkailija-oid       (:virkailija-oid state)}})))

(defn mass-store
  [information-requests virkailija-oid job-runner]
  {:pre [(every? (comp u/not-blank? :subject) information-requests)
         (every? (comp u/not-blank? :message) information-requests)
         (every? (comp u/not-blank? :application-key) information-requests)
         (every? (comp u/not-blank? :message-type) information-requests)]}
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (job/start-job job-runner
                   connection
                   "mass-information-request-job"
                   {:information-requests information-requests
                    :virkailija-oid       virkailija-oid})))
