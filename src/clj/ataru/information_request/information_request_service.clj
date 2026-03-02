(ns ataru.information-request.information-request-service
  (:require [ataru.db.db :as db]
            [ataru.forms.form-store :as forms]
            [ataru.log.audit-log :as audit-log]
            [ataru.translations.translation-util :as translations]
            [ataru.util :as u]
            [ataru.email.email-util :as email-util]
            [ataru.email.application-email-jobs :refer [->safe-html]]
            [ataru.email.application-email :as application-email]
            [ataru.information-request.information-request-job :as information-request-job]
            [ataru.information-request.information-request-store :as information-request-store]
            [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-store :as tutkintojen-tunnustaminen-store]
            [ataru.applications.application-store :as app-store]
            [ataru.config.core :refer [config]]
            [clojure.java.jdbc :as jdbc]
            [selmer.parser :as selmer]
            [clojure.string :as string]
            [ataru.background-job.job :as job]
            [taoensso.timbre :as log]
            [ataru.time :as time]))

(defn- information-request-email-template-filename
  [lang]
  (str "templates/information_request_template_" (name lang) ".html"))

(defn- extract-answer-value [answer-key-str application]
  (->> (:answers application)
       (filter (comp (partial = answer-key-str) :key))
       (map :value)
       (first)))

(defn- initial-state [connection information-request guardian?
                      {:keys [tarjonta-service ohjausparametrit-service koodisto-cache organization-service]}]
  (let [add-update-link? (:add-update-link information-request)]
    (when add-update-link?
      (app-store/add-new-secret-to-application-in-tx
        connection
        (:application-key information-request)))
    (let [application       (app-store/get-latest-application-by-key-in-tx
                              connection
                              (:application-key information-request))
          form              (forms/fetch-by-id (:form application))
          tarjonta-info     (application-email/get-tarjonta-info koodisto-cache tarjonta-service organization-service
                                                                 ohjausparametrit-service application)
          organization-oids (application-email/organization-oids tarjonta-info application)
          lang              (-> application :lang keyword)
          recipient-emails  (if guardian?
                              (distinct
                                (flatten
                                  (filter some?
                                          [(extract-answer-value "guardian-email" application)
                                           (extract-answer-value "guardian-email-secondary" application)])))
                              (remove string/blank? [(extract-answer-value "email" application)]))
          translations      (translations/get-translations lang)
          {:keys [application-url application-url-text oma-opintopolku-link]} (email-util/get-application-url-and-text form application lang)
          body              (selmer/render-file (information-request-email-template-filename lang)
                                                (merge {:message (->safe-html (:message information-request))}
                                                       (if (or guardian?
                                                               (not (or add-update-link?
                                                                        (:reminder? information-request))))
                                                         {}
                                                         {:application-url application-url
                                                          :application-url-text (->safe-html application-url-text)
                                                          :oma-opintopolku-link oma-opintopolku-link})
                                                       translations))
          subject-with-application-key (email-util/enrich-subject-with-application-key-and-limit-length
                                         (if (:reminder? information-request)
                                           (str (:information-request-reminder-subject-prefix translations)
                                                ": "
                                                (:subject information-request))
                                           (:subject information-request)) (:application-key information-request) lang)]
      (when (not-empty recipient-emails)
        (-> (select-keys information-request [:application-key :id])
            (merge {:from       "no-reply@opintopolku.fi"
                    :recipients recipient-emails
                    :body       body
                    :masks      (if application-url
                                  [{:secret application-url
                                    :mask   "https://hakemuslinkki-piilotettu.opintopolku.fi/"}]
                                  [])
                    :metadata   (email-util/->metadata (:application-key information-request) (:person-oid application))
                    :privileges (email-util/->hakemus-privileges organization-oids)})
            (assoc :subject subject-with-application-key))))))

(defn start-email-job [job-runner connection information-request]
  (let [job-type (:type information-request-job/job-definition)
        target (:recipient-target information-request)]
    (when (or (= "hakija" target)
              (= "hakija_ja_huoltajat" target))
      (if-let [job-state (initial-state connection information-request false job-runner)]
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
      (if-let [job-state (initial-state connection information-request true job-runner)]
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
    (let [add-update-link   (:add-update-link information-request)
          reminder-hour     (get-in config [:public-config :information-request-reminder-job-hour])
          send-reminder-time (when (:send-reminder? information-request)
                               (time/plus
                                 (time/with-time (time/now) (time/local-time reminder-hour 0))
                                 (time/days (:reminder-days information-request))))
          information-request (information-request-store/add-information-request
                                (assoc
                                  information-request
                                  :send-reminder-time
                                  send-reminder-time)
                                (-> session :identity :oid)
                                connection)
          information-request-with-add-update-link (assoc information-request :add-update-link add-update-link)]
    (start-email-job job-runner connection information-request-with-add-update-link)
    (audit-log/log (:audit-logger job-runner)
                   {:new       information-request
                    :operation audit-log/operation-new
                    :session   session
                    :id        {:applicationOid (:application-key information-request)}})
    (tutkintojen-tunnustaminen-store/start-tutkintojen-tunnustaminen-information-request-jobs
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
  (let [keys (:application-keys state)]
    (jdbc/with-db-transaction
      [connection {:datasource (db/get-datasource :db)}]
      (if (< 1 (count keys))
        ; splitataan hakemukset omiin jobeihin, näin nähdään hakemuskohtaisesti onko lisätietopyyntö onnistunut
        (doseq [key keys]
          (job/start-job job-runner connection "mass-information-request-job" (assoc state :application-keys [key])))
        ; ajetaan jobi yhdelle hakemukselle
        (store-in-tx {:identity {:oid (:virkailija-oid state)}}
                     (assoc (:information-request state)
                       :application-key (first keys))
                     job-runner
                     connection)))))

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
