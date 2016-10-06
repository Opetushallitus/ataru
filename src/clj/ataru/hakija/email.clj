(ns ataru.hakija.email
  (:require [ataru.applications.application-store :as application-store]
            [org.httpkit.client :as http]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [info error]]
            [com.stuartsierra.component :as component]
            [ataru.hakija.email-store :as store]))

(def email-poll-interval 5000)

(declare send-email-verification)

(defn- emailer-loop
  []
  (while true
    (store/deliver-emails send-email-verification)
    (Thread/sleep email-poll-interval)))

(defrecord Emailer []
  component/Lifecycle

  (start [this]
    (info "Starting emailer process")
    (let [emailer-future (future (emailer-loop))]
      (info "Started emailer process")
      (assoc this :email emailer-future)))
  (stop [this]
    (info "Stopping emailer process")
    (when-let [emailer-future (:email this)]
      (future-cancel emailer-future)
      (if (future-cancelled? emailer-future)
        (do
          (info "Stopped emailer process")
          (assoc this :email nil))
        (error "Error stopping emailer process")))))

(defn new-emailer
  []
  (->Emailer))

(def ^:private subject
  {:fi "Opintopolku.fi - Hakemuksesi on vastaanotettu"
   :sv "Opintopolku.fi - Din ansökan har tagits emot"
   :en "Opintopolku.fi - Your application has been received"})

(defn- send-email-verification
  "Synchronous call to keep DB transactions intact"
  [email-verification]
  (let [url            (str
                         (get-in config [:email :email_service_url])
                         "/ryhmasahkoposti-service/email/firewall")
        id             (:id email-verification)
        application-id (:application_id email-verification)
        recipient      (:recipient email-verification)
        application    (application-store/get-application application-id)
        template       (str "templates/email_confirmation_template_" (or (:lang application) "fi") ".txt")
        body           (selmer/render-file template {})
        subject        (get subject (-> application :lang keyword))]
    (info "sending email" id "to viestintäpalvelu at address" url "for application" application-id)
    @(http/post url {:headers {"content-type" "application/json"}
                    :body (json/generate-string {:email {:from "no-reply@opintopolku.fi"
                                                         :subject subject
                                                         :isHtml true
                                                         :body body}
                                                 :recipient [{:email recipient}]})})))
