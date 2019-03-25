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
            [ataru.virkailija.background-jobs.virkailija-jobs :as virkailija-jobs]
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

(defn- start-email-job [job-runner information-request]
  (let [initial-state (initial-state information-request)
        job-type      (:type information-request-job/job-definition)
        job-id        (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                        (job/start-job job-runner
                                       connection
                                       job-type
                                       initial-state))]
    (log/info (str "Started information request email job with job id " job-id ", initial state: " initial-state))))

(defn store [information-request session job-runner]
  {:pre [(-> information-request :subject u/not-blank?)
         (-> information-request :message u/not-blank?)
         (-> information-request :application-key u/not-blank?)]}
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [information-request (information-request-store/add-information-request
                                (merge information-request {:message-type "information-request"})
                                session
                                conn)]
      (audit-log/log {:new       information-request
                      :operation audit-log/operation-new
                      :id        (-> session :identity :oid)})
      (start-email-job job-runner information-request)
      information-request)))

(defn mass-store
  [information-requests session job-runner]
  {:pre [(every? (comp u/not-blank? :subject) information-requests)
         (every? (comp u/not-blank? :message) information-requests)
         (every? (comp u/not-blank? :application-key) information-requests)]}
  (let [stored-information-requests (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                                      (mapv
                                        #(information-request-store/add-information-request
                                           (merge % {:message-type "mass-information-request"})
                                           session
                                           conn)
                                        information-requests))]
    (doseq [stored-information-request stored-information-requests]
      (start-email-job job-runner stored-information-request)
      (audit-log/log {:new       stored-information-request
                      :operation audit-log/operation-new
                      :id        (-> session :identity :oid)}))
    stored-information-requests))
