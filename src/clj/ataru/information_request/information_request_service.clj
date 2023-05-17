(ns ataru.information-request.information-request-service
  (:require [ataru.config.core :refer [config]]
            [ataru.db.db :as db]
            [ataru.log.audit-log :as audit-log]
            [ataru.translations.translation-util :as translations]
            [ataru.translations.texts :refer [email-default-texts]]
            [ataru.util :as u]
            [ataru.email.application-email-jobs :refer [->safe-html]]
            [ataru.information-request.information-request-job :as information-request-job]
            [ataru.information-request.information-request-store :as information-request-store]
            [ataru.applications.application-store :as app-store]
            [clojure.java.jdbc :as jdbc]
            [selmer.parser :as selmer]
            [clojure.string :as string]
            [ataru.background-job.job :as job]
            [taoensso.timbre :as log]))

(defn- enrich-subject-with-application-key [prefix application-key lang]
  (if application-key
    (let [postfix (str "(" (get-in email-default-texts [:hakemusnumero (or lang :fi)]) ": " application-key ")")]
      (string/join " " [prefix postfix]))
    prefix))

(defn- extract-answer-value [answer-key-str application]
  (->> (:answers application)
       (filter (comp (partial = answer-key-str) :key))
       (map :value)
       (first)))

(defn- initial-state [connection information-request guardian?]
  (let [
        ;add-update-link? (:add-update-link information-request)
        secret           (app-store/add-new-secret-to-application-in-tx
                          connection
                          (:application-key information-request))
        application      (app-store/get-latest-application-by-key-in-tx
                          connection
                          (:application-key information-request))
        lang             (-> application :lang keyword)
        recipient-emails (if guardian?
                           (distinct
                             (flatten
                               (filter some?
                                       [(extract-answer-value "guardian-email" application)
                                        (extract-answer-value "guardian-email-secondary" application)])))
                           (remove string/blank? [(extract-answer-value "email" application)]))
        translations     (translations/get-translations lang)
        service-url      (get-in config [:public-config :applicant :service_url])
        application-url  (str service-url "/hakemus?modify=" secret)
        ;  show-link             (if (and add-update-link? guardian?){} {})
        body             (selmer/render-file "templates/information-request-template.html"
                                             (merge {:message         (->safe-html (:message information-request))}
                                                    (if guardian?
                                                      {}
                                                      {:application-url application-url})
                                                    translations))
        subject-with-application-key (enrich-subject-with-application-key (:subject information-request) (:application-key information-request) lang)]
    (when (not-empty recipient-emails)
      (-> (select-keys information-request [:application-key :id])
          (merge {:from       "no-reply@opintopolku.fi"
                  :recipients recipient-emails
                  :body       body})
          (assoc :subject subject-with-application-key)))))

(defn- start-email-job [job-runner connection information-request]
  (println "information-request-service.start-email-job: information-request = " information-request)
  (let [job-type (:type information-request-job/job-definition)
        target (:recipient-target information-request)
        ;add-update-link (:add-update-link information-request)
        ]
    (when (or (= "hakija" target)
              (= "hakija_ja_huoltajat" target))
      (let [job-id (job/start-job job-runner
                                  connection
                                  job-type
                                  (initial-state connection information-request false))]
        (log/info (str "Started information request email job with job id " job-id
                       " for application " (:application-key information-request)))))
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
    (let [
        add-update-link (:add-update-link information-request)
        information-request (information-request-store/add-information-request
                             information-request
                             (-> session :identity :oid)
                             connection)
        information-request-with-add-update-link (assoc information-request :add-update-link add-update-link)
        ]
    (start-email-job job-runner connection information-request-with-add-update-link)
    (audit-log/log (:audit-logger job-runner)
                   {:new       information-request
                    :operation audit-log/operation-new
                    :session   session
                    :id        {:applicationOid (:application-key information-request)}})
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
