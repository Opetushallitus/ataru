(ns ataru.suoritus.suorituspalvelu-client
  (:require [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url]
            [cheshire.core :as json]
            [clojure.core.match :refer [match]]
            [ataru.config.core :refer [config]]))

(def cas-client (cas-client/new-client "/suorituspalvelu" "/api/login/j_spring_cas_security_check" "JSESSIONID" (-> config :public-config :hakija-caller-id)))

(defn oppilaitoksen-luokat [oppilaitos-oid vuosi]
  (let [url (url/resolve-url
              "suorituspalvelu.oppilaitoksen-luokat"
              oppilaitos-oid
              vuosi)]
    (match [(cas-client/cas-authenticated-get
              cas-client
              url)]
           [{:status 200 :body body}]
           (json/parse-string body true)
           [r]
           (throw (new RuntimeException
                       (str "Fetching luokat failed: " r))))))

(defn lahtokoulut [henkilo-oid]
  (let [url (url/resolve-url
              "suorituspalvelu.lahtokoulut"
              henkilo-oid)]
    (match [(cas-client/cas-authenticated-get
              cas-client
              url)]
           [{:status 200 :body body}]
           (json/parse-string body true)
           [r]
           (throw (new RuntimeException
                       (str "Fetching lahtokoulut failed: " r))))))

(defn hakemuksen-avainarvot [hakemus-oid]
  (let [url (url/resolve-url
              "suorituspalvelu.hakemuksen-avainarvot"
              hakemus-oid)]
    (match [(cas-client/cas-authenticated-get
              cas-client
              url)]
           [{:status 200 :body body}]
           (json/parse-string body true)
           [r]
           (throw (new RuntimeException
                       (str "Fetching avainarvot failed: " r))))))