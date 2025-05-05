(ns ataru.background-job.email-job
  "You can send any email with this, it's not tied to any particular email-type"
  (:require [ataru.config.core :refer [config]]
            [ataru.config.url-helper :refer [resolve-url]]
            [taoensso.timbre :as log])
  (:import (java.util Optional)
           (fi.oph.viestinvalitys ViestinvalitysClient ClientBuilder)
           (fi.oph.viestinvalitys.vastaanotto.model ViestinvalitysBuilder LuoViestiSuccessResponse)))

(defn- viestinvalityspalvelu-endpoint []
  (resolve-url :viestinvalityspalvelu-endpoint))

(defn viestinvalitys-client ^ViestinvalitysClient []
  (-> (ClientBuilder/viestinvalitysClientBuilder)
      (.withEndpoint (viestinvalityspalvelu-endpoint))
      (.withUsername (-> config :cas :username))
      (.withPassword (-> config :cas :password))
      (.withCasEndpoint (resolve-url :cas-client))
      (.withCallerId "1.2.246.562.10.00000000001.ataru.backend")
      (.build)))

(defn- vastaanottajat [recipients]
  (.build
    (reduce
      (fn [builder recipient]
        (.withVastaanottaja builder (Optional/empty) recipient))
      (ViestinvalitysBuilder/vastaanottajatBuilder)
      recipients)))

(defn- maskit [masks]
  (.build
    (reduce
      (fn [builder {:keys [secret mask]}]
        (.withMaski builder secret (or mask "********")))
      (ViestinvalitysBuilder/maskitBuilder)
      masks)))

(defn- metadatat [metadata]
  (.build
    (reduce
      (fn [builder [key values]]
        (.withMetadata builder
                       (name key)
                       values))
      (ViestinvalitysBuilder/metadatatBuilder)
      metadata)))

(defn- kayttooikeusrajoitukset [privileges]
  (.build
    (reduce
      (fn [builder {:keys [privilege organization]}]
        (.withKayttooikeus builder privilege organization))
      (ViestinvalitysBuilder/kayttooikeusrajoituksetBuilder)
      privileges)))

(defn send-email [from recipients subject body masks metadata privileges]
  (let [client (viestinvalitys-client)
        lahetys-response (-> client
                             (.luoLahetys (-> (ViestinvalitysBuilder/lahetysBuilder)
                                              (.withOtsikko subject)
                                              (.withLahettavaPalvelu "hakemuspalvelu")
                                              (.withLahettaja (Optional/of "Opetushallitus") from)
                                              (.withNormaaliPrioriteetti)
                                              (.withSailytysaika 365)
                                              (.build))))
         viesti-response (-> client
                             (.luoViesti (-> (ViestinvalitysBuilder/viestiBuilder)
                                             (.withOtsikko subject)
                                             (.withHtmlSisalto body)
                                             (.withVastaanottajat (vastaanottajat recipients))
                                             (.withKayttooikeusRajoitukset
                                               (kayttooikeusrajoitukset
                                                 (conj privileges
                                                       {:privilege "APP_VIESTINVALITYS_OPH_PAAKAYTTAJA"
                                                        :organization "1.2.246.562.10.00000000001"})))
                                             (.withLahetysTunniste (str (.getLahetysTunniste lahetys-response)))
                                             (.withMaskit (maskit masks))
                                             (.withMetadatat (metadatat metadata))
                                             (.build))))]
    (log/info "Got response" (str viesti-response) "from viestinvälityspalvelu")
    (when-not (instance? LuoViestiSuccessResponse viesti-response)
      (throw (Exception. (str "Could not send email to " (apply str recipients)))))))

(defn send-email-handler [{:keys [from recipients subject body masks metadata privileges]} _]
  {:pre [(every? #(identity %) [from recipients subject body masks metadata privileges])]}
  (log/info "Trying to send email" subject "to" recipients "with metadata" metadata "and privileges" privileges
            "via viestinvälityspalvelu at address" (viestinvalityspalvelu-endpoint))
  (send-email from recipients subject body masks metadata privileges)
  (log/info "Successfully sent email to" recipients))

(def job-definition {:handler send-email-handler
                     :type    (str (ns-name *ns*))
                     :queue   {:proletarian/worker-threads 2
                               :proletarian/polling-interval-ms 1000}})
