(ns ataru.hakija.email
  (:require [aleph.http :as http]
            [cheshire.core :as json]
            [oph.soresu.common.config :refer [config]]))

(defn send-email-verification
  [application]
  (let [url (str
              (get-in config [:email :email_service_url])
              "/ryhmasahkoposti-service/email/firewall")]
    (http/post url {:headers {"content-type" "application/json"}
                    :body (json/generate-string {:email {:from "no-reply@opintopolku.fi"
                                                         :subject "Hakemus vastaanotettu"
                                                         :isHtml false
                                                         :body "Hakemuksesi on vastaanotettu!"}
                                                 :recipient [{:email "hakija@opintopolku.fi"}]})})))
