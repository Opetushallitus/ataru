(ns ataru.cas-oppija.cas-oppija-utils
  (:require [clojure.data.xml :as xml]
            [ataru.config.core :refer [config]]
            [taoensso.timbre :as log]
            [clojure.string :as str]))

(defn parse-oppija-attributes-if-successful [validation-response]
  (let [xml (xml/parse-str validation-response)
        success? (= (-> xml
                        :content
                        (first)
                        :tag)
                    :authenticationSuccess)
        attributes (-> xml
                       :content
                       (first)
                       :content
                       (last)
                       :content)
        parsed-raw-map (into {} (map (fn [element] [(:tag element) (first (:content element))]) attributes))] ;todo onkohan tämä tarpeeksi robusti, xml-käsittely pitää vielä käydä läpi
    ;todo, tähän tulee vielä lisää kenttiä.
    ;Avaimet täytyy pitää samoina kuin henkilötietomoduulin esitäytettävien vastausten tunnisteet.
    (when success?
      {:person-oid (:personOid parsed-raw-map)
       :display-name (:displayName parsed-raw-map)
       :fields     {:first-name           {:value  (:firstName parsed-raw-map)
                                           :locked true}
                    :preferred-name       {:value  (:givenName parsed-raw-map)
                                           :locked true}
                    :last-name            {:value  (:sn parsed-raw-map)
                                           :locked true}
                    :ssn                  {:value  (:nationalIdentificationNumber parsed-raw-map)
                                           :locked true}
                    :email                {:value  (:mail parsed-raw-map)
                                           :locked false}
                    :country-of-residence {:value  (or
                                                     (:VakinainenUlkomainenLahiosoiteValtiokoodi3 parsed-raw-map)
                                                     "246")
                                           :locked false}
                    :address              {:value  (or
                                                     (:VakinainenKotimainenLahiosoiteS parsed-raw-map)
                                                     (:VakinainenUlkomainenLahiosoite parsed-raw-map))
                                           :locked false}
                    :postal-code          {:value  (:VakinainenKotimainenLahiosoitePostinumero parsed-raw-map)
                                           :locked false}
                    :home-town            {:value  (:KotikuntaKuntanumero parsed-raw-map)
                                           :locked false}}})))

(defn parse-cas-oppija-login-url [locale target]
  (let [url (str
              (-> config :urls :cas-oppija-url)
              "/login?locale=" (or locale "fi") "&valtuudet=false&service="
              (-> config :urls :ataru-hakija-login-url)
              "?target=" target)]
    (log/info "cas-oppija-login-url" url)
    url))

(defn parse-cas-oppija-ticket-validation-url [ticket target]
  (let [url (str
              (-> config :urls :cas-oppija-url)
              "/serviceValidate?ticket=" ticket "&service="
              (-> config :urls :ataru-hakija-login-url)
              "?target=" target)]
    (log/info "cas-oppija-ticket validation url" url)
    url))

(def logout-pages {:fi "/konfo/fi/sivu/uloskirjautuminen"
                   :sv "/konfo/sv/sivu/utloggningen"
                   :en "/konfo/en/sivu/logout"})

(defn parse-cas-oppija-logout-url [lang]
  (let [url (str
              (-> config :urls :cas-oppija-url)
              "/logout?service=https://"
              (-> config :urls :hakija-host)
              (lang logout-pages))]
    (log/info "cas-oppija logout url" url)
    url))

(defn parse-ticket-from-lockout-request [logout-request]
  (if-let [ticket (some #(when (= (:tag %) :SessionIndex)
                           (first (:content %)))
                        (:content (xml/parse-str logout-request)))]
    (str/trim ticket)
    nil))