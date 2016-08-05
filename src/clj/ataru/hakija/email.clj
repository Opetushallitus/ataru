(ns ataru.hakija.email
  (:require [aleph.http :as http]
            [manifold.deferred :as deferred]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]
            [selmer.parser :as selmer]
            [taoensso.timbre :refer [info error]]))

(defn send-email-verification
  [application application-id]
  (let [url       (str
                    (get-in config [:email :email_service_url])
                    "/ryhmasahkoposti-service/email/firewall")
        body      (selmer/render-file "templates/email_confirmation_template.txt" {})
        recipient (-> (filter #(= "email" (:key %)) (:answers application))
                      first
                      :value)]
    (info "sending email to viestintäpalvelu at address " url " for application " application-id)
    (let [reply (http/post url {:headers {"content-type" "application/json"}
                                :body (json/generate-string {:email {:from "no-reply@opintopolku.fi"
                                                             :subject "Opintopolku.fi - Hakemuksesi on vastaanotettu"
                                                             :isHtml true
                                                             :body body}
                                                             :recipient [{:email recipient}]})})]
          (deferred/on-realized
            reply
            (fn [_] (info "Successfully sent email to viestintäpalvelu for application " application-id))
            (fn [error-details]
              (error "Sending email to viestintäpalvelu failed for application " application-id)
              (error "email details:")
              (error "recipient: " recipient)
              (error "body:")
              (error  body)
              (error "error details:")
              (error error-details))))))
