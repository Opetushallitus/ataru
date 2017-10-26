(ns ataru.information-request.information-request-service
  (:require [ataru.config.core :refer [config]]
            [ataru.db.db :as db]
            [ataru.log.audit-log :as audit-log]
            [ataru.translations.translation-util :as translations]
            [ataru.util :as u]
            [ataru.information-request.information-request-job :as information-request-job]
            [ataru.applications.application-store :as app-store]
            [ataru.virkailija.background-jobs.virkailija-jobs :as virkailija-jobs]
            [camel-snake-kebab.core :as c]
            [camel-snake-kebab.extras :as t]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :as sql]
            [selmer.parser :as selmer]
            [ataru.background-job.job :as job]
            [taoensso.timbre :as log]))

(sql/defqueries "sql/information-request-queries.sql")

(def ^:private ->kebab-case-kw (partial t/transform-keys c/->kebab-case-keyword))
(def ^:private ->snake-case-kw (partial t/transform-keys c/->snake_case_keyword))
(def ^:private information-request-translations {:hello-text   {:fi "Hei"
                                                                :sv "Hej"
                                                                :en "Hi"}
                                                 :best-regards {:fi "terveisin"
                                                                :sv "Med vänliga hälsningar"
                                                                :en "Best Regards"}})

(defn- extract-answer-value [answer-key-str application]
  (->> (:answers application)
       (filter (comp (partial = answer-key-str) :key))
       (map :value)
       (first)))

(defn- create-email [information-request]
  (let [application     (-> information-request :application-key app-store/get-latest-application-by-key-unrestricted)
        lang            (-> application :lang keyword)
        first-name      (extract-answer-value "preferred-name" application)
        recipient-email (extract-answer-value "email" application)
        translations    (translations/get-translations lang information-request-translations)
        service-url     (get-in config [:public-config :applicant :service_url])
        application-url (str service-url "/hakemus?modify=" (:secret application))
        body            (selmer/render-file "templates/information-request-template.html"
                                            (merge {:first-name      first-name
                                                    :message         (:message information-request)
                                                    :application-url application-url}
                                                   translations))]
    {:from       "no-reply@opintopolku.fi"
     :recipients [recipient-email]
     :subject    (:subject information-request)
     :body       body}))

(defn- start-email-job [information-request]
  (let [email    (create-email information-request)
        job-type (:type information-request-job/job-definition)
        job-id   (job/start-job virkailija-jobs/job-definitions
                                job-type
                                email)]
    (log/info (str "Started information request email job with job id " job-id ", email to be sent: " email))))

(defn store [information-request
             session]
  {:pre [(-> information-request :subject u/not-blank?)
         (-> information-request :message u/not-blank?)
         (-> information-request :application-key u/not-blank?)]}
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (audit-log/log {:new       information-request
                    :operation audit-log/operation-new
                    :id        (-> session :identity :username)})
    (start-email-job information-request)
    (-> (yesql-add-information-request<! (->snake-case-kw information-request)
                                         {:connection conn})
        (->kebab-case-kw)
        (dissoc :id))))
