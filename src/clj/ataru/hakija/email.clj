(ns ataru.hakija.email
  (:require [aleph.http :as http]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]
            [selmer.parser :as selmer]))

(defn send-email-verification
  [application]
  (let [url  (str
               (get-in config [:email :email_service_url])
               "/ryhmasahkoposti-service/email/firewall")
        body (selmer/render-file "templates/email_confirmation_template.txt" {})
        recipient (-> (filter #(= "email" (:key %)) (:answers application))
                      first
                      :value)]
    (http/post url {:headers {"content-type" "application/json"}
                    :body (json/generate-string {:email {:from "no-reply@opintopolku.fi"
                                                         :subject "Opintopolku.fi - Hakemuksesi on vastaanotettu"
                                                         :isHtml true
                                                         :body body}
                                                 :recipient [{:email recipient}]})})))
