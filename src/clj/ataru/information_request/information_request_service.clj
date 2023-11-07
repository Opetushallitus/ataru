(ns ataru.information-request.information-request-service
  (:require [ataru.db.db :as db]
            [ataru.forms.form-store :as forms]
            [ataru.log.audit-log :as audit-log]
            [ataru.translations.translation-util :as translations]
            [ataru.util :as u]
            [ataru.email.email-util :as email-util]
            [ataru.email.application-email-jobs :refer [->safe-html]]
            [ataru.information-request.information-request-job :as information-request-job]
            [ataru.information-request.information-request-store :as information-request-store]
            [ataru.tutkintojen-tunnustaminen :as tutkintojen-tunnustaminen]
            [ataru.applications.application-store :as app-store]
            [clojure.java.jdbc :as jdbc]
            [selmer.parser :as selmer]
            [clojure.string :as string]
            [ataru.background-job.job :as job]
            [taoensso.timbre :as log]))

(defn- information-request-email-template-filename
  [lang]
  (str "templates/information_request_template_" (name lang) ".html"))

(defn- extract-answer-value [answer-key-str application]
  (->> (:answers application)
       (filter (comp (partial = answer-key-str) :key))
       (map :value)
       (first)))
(defn- initial-state [connection information-request guardian?]
  (let [add-update-link? (:add-update-link information-request)]
    (when add-update-link?
      (app-store/add-new-secret-to-application-in-tx
        connection
        (:application-key information-request)))
    (let [application      (app-store/get-latest-application-by-key-in-tx
                             connection
                             (:application-key information-request))
          form              (forms/fetch-by-id (:form application))
          lang             (-> application :lang keyword)
          recipient-emails (if guardian?
                             (distinct
                               (flatten
                                 (filter some?
                                         [(extract-answer-value "guardian-email" application)
                                          (extract-answer-value "guardian-email-secondary" application)])))
                             (remove string/blank? [(extract-answer-value "email" application)]))
          translations     (translations/get-translations lang)
          url-and-link (email-util/get-application-url-and-text form application lang)
          body             (selmer/render-file (information-request-email-template-filename lang)
                                               (merge {:message (->safe-html (:message information-request))}
                                                      (if (or guardian? (not add-update-link?))
                                                        {}
                                                        url-and-link)
                                                      translations))
          subject-with-application-key (email-util/enrich-subject-with-application-key-and-limit-length
                                         (:subject information-request) (:application-key information-request) lang)]
      (when (not-empty recipient-emails)
        (-> (select-keys information-request [:application-key :id])
            (merge {:from       "no-reply@opintopolku.fi"
                    :recipients recipient-emails
                    :body       body})
            (assoc :subject subject-with-application-key))))))

(defn- start-email-job [job-runner connection information-request]
  (let [job-type (:type information-request-job/job-definition)
        target (:recipient-target information-request)]
    (when (or (= "hakija" target)
              (= "hakija_ja_huoltajat" target))
      (if-let [job-state (initial-state connection information-request false)]
        (let [job-id (job/start-job job-runner
                                    connection
                                    job-type
                                    job-state)]
          (log/info (str "Started information request email job with job id " job-id
                         " for application " (:application-key information-request))))
        (log/info (str "Skipped information request email job for application "
                       (:application-key information-request)
                       " because application doesn't contain email"))))
    (when (or (= "huoltajat" target)
              (= "hakija_ja_huoltajat" target))
      (if-let [job-state (initial-state connection information-request true)]
        (let [job-id (job/start-job job-runner
                                    connection
                                    job-type
                                    job-state)]
          (log/info (str "Started information request email job with job id " job-id
                         " for application " (:application-key information-request))))
        (log/info (str "Skipped information request email job for guardian for application "
                       (:application-key information-request)
                       " because application doesn't contain guardian email"))))))

(defn- store-in-tx
  [session information-request job-runner connection]
  {:pre [(-> information-request :subject u/not-blank?)
         (-> information-request :message u/not-blank?)
         (-> information-request :application-key u/not-blank?)
         (-> information-request :message-type u/not-blank?)]}
    (let [add-update-link (:add-update-link information-request)
          information-request (information-request-store/add-information-request
                             information-request
                             (-> session :identity :oid)
                             connection)
          information-request-with-add-update-link (assoc information-request :add-update-link add-update-link)]
    (start-email-job job-runner connection information-request-with-add-update-link)
    (audit-log/log (:audit-logger job-runner)
                   {:new       information-request
                    :operation audit-log/operation-new
                    :session   session
                    :id        {:applicationOid (:application-key information-request)}})
    (tutkintojen-tunnustaminen/start-tutkintojen-tunnustaminen-information-request-sent-job
      job-runner
      information-request)
    information-request))

(defn store [session information-request job-runner]
  {:pre [(-> information-request :subject u/not-blank?)
         (-> information-request :message u/not-blank?)
         (-> information-request :application-key u/not-blank?)
         (-> information-request :message-type u/not-blank?)]}
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (store-in-tx session information-request job-runner connection)))

(defn mass-information-request-job-step
  [state job-runner]
  (if (empty? (:application-keys state))
    {:transition {:id :final}}
    (let [[now later] (split-at 100 (:application-keys state))]
      (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
        (doseq [key now]
          (store-in-tx {:identity {:oid (:virkailija-oid state)}}
                       (assoc (:information-request state)
                              :application-key key)
                       job-runner
                       connection)))
      {:transition    {:id :to-next :step :initial}
       :updated-state (assoc state :application-keys later)})))

(defn mass-store
  [information-request application-keys virkailija-oid job-runner]
  {:pre [(-> information-request :subject u/not-blank?)
         (-> information-request :message u/not-blank?)
         (-> information-request :message-type u/not-blank?)]}
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (job/start-job job-runner
                   connection
                   "mass-information-request-job"
                   {:information-request information-request
                    :application-keys    application-keys
                    :virkailija-oid      virkailija-oid})))
