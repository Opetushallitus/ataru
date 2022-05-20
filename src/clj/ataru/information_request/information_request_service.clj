(ns ataru.information-request.information-request-service
  (:require [ataru.config.core :refer [config]]
            [ataru.db.db :as db]
            [ataru.log.audit-log :as audit-log]
            [ataru.translations.translation-util :as translations]
            [ataru.util :as u]
            [ataru.email.application-email-jobs :refer [->safe-html]]
            [ataru.information-request.information-request-job :as information-request-job]
            [ataru.information-request.information-request-store :as information-request-store]
            [ataru.applications.application-store :as app-store]
            [clojure.java.jdbc :as jdbc]
            [selmer.parser :as selmer]
            [ataru.background-job.job :as job]
            [taoensso.timbre :as log]))

(defn- extract-answer-value [answer-key-str application]
  (->> (:answers application)
       (filter (comp (partial = answer-key-str) :key))
       (map :value)
       (first)))

(defn- initial-state [connection information-request guardian?]
  (let [secret           (app-store/add-new-secret-to-application-in-tx
                          connection
                          (:application-key information-request))
        application      (app-store/get-latest-application-by-key-in-tx
                          connection
                          (:application-key information-request))
        lang             (-> application :lang keyword)
        recipient-emails (if guardian?
                           (filter some?
                                   [(extract-answer-value "guardian-email" application)
                                    (extract-answer-value "guardian-email-secondary" application)])
                           [(extract-answer-value "email" application)])
        translations     (translations/get-translations lang)
        service-url      (get-in config [:public-config :applicant :service_url])
        application-url  (str service-url "/hakemus?modify=" secret)
        body             (selmer/render-file "templates/information-request-template.html"
                                             (merge {:message         (->safe-html (:message information-request))}
                                                    (if guardian?
                                                      {}
                                                      {:application-url application-url})
                                                    translations))]
    (when (not-empty recipient-emails)
      (-> (select-keys information-request [:subject :application-key :id])
          (merge {:from       "no-reply@opintopolku.fi"
                  :recipients recipient-emails
                  :body       body})))))

(defn- start-email-job [job-runner connection information-request]
  (let [job-type      (:type information-request-job/job-definition)
        target        (:recipient-target information-request)]
    (cond-> nil
            (or (= "hakija" target)
                (= "hakija_ja_huoltajat" target))
            #(let [job-id (job/start-job job-runner
                                         connection
                                         job-type
                                         (initial-state connection information-request false))]
               (log/info (str "Started information request email job with job id " job-id
                              " for application " (:application-key information-request))))

            (or (= "huoltajat" target)
                (= "hakija_ja_huoltajat" target))
            #(if-let [job-state (initial-state connection information-request true)]
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
         (-> information-request :message-type u/not-blank?)
         (-> information-request :recipient-target u/not-blank?)]}
  (let [information-request (information-request-store/add-information-request
                             information-request
                             (-> session :identity :oid)
                             connection)]
    (start-email-job job-runner connection information-request)
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
