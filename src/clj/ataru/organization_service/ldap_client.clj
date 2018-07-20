(ns ataru.organization-service.ldap-client
  (:require [ataru.config.core :refer [config]]
            [clj-ldap.client :as ldap])
  (:import (java.net InetAddress)))

(def people-path-base "ou=People,dc=opintopolku,dc=fi")

(defn- create-ldap-connection []
  {:pre [(some? (:ldap config))]}
  (let [ldap-config (:ldap config)
        host        (InetAddress/getByName (:server ldap-config))
        host-name   (.getHostName host)]
    (ldap/connect {:host [{:address host-name
                           :port (:port ldap-config)}]
                   :bind-dn (:userdn ldap-config)
                   :password (:password ldap-config)
                   :ssl? (:ssl ldap-config)
                   :num-connections 4})))

(def fake-org-by-oid
  {"1.2.246.562.10.11"         {:name {:fi "Lasikoulu"}, :oid "1.2.246.562.10.11", :type :organization}
   "1.2.246.562.10.22"         {:name {:fi "Omnia"}, :oid "1.2.246.562.10.22", :type :organization}
   "1.2.246.562.10.1234334543" {:name {:fi "Telajärven aikuislukio"}, :oid "1.2.246.562.10.1234334543", :type :organization}
   "1.2.246.562.10.0439845"    {:name {:fi "Test org"}, :oid "1.2.246.562.10.0439845" :type :organization}
   "1.2.246.562.28.1"          {:name {:fi "Test group"}, :oid "1.2.246.562.28.1" :type :group}
   "1.2.246.562.10.0439846"    {:name {:fi "Test org 2"}, :oid "1.2.246.562.10.0439846" :type :organization}
   "1.2.246.562.28.2"          {:name {:fi "Test group 2"}, :oid "1.2.246.562.28.2" :type :group}})

(def fake-orgs
  {"DEVELOPER"                        [(get fake-org-by-oid "1.2.246.562.10.0439845")
                                       (get fake-org-by-oid "1.2.246.562.28.1")]
   "USER-WITH-HAKUKOHDE-ORGANIZATION" [(get fake-org-by-oid "1.2.246.562.10.0439846")
                                       (get fake-org-by-oid "1.2.246.562.28.2")]})

(def fake-virkailija-value
  {"DEVELOPER"                        {:employeeNumber "1.2.246.562.11.11111111012"
                                       :givenName      "Veijo"
                                       :sn             "Virkailija"
                                       :description    "[\"APP_ATARU_EDITORI_CRUD_1.2.246.562.10.0439845\",\"APP_ATARU_HAKEMUS_CRUD_1.2.246.562.10.0439845\",\"APP_ATARU_EDITORI_CRUD_1.2.246.562.28.1\",\"APP_ATARU_HAKEMUS_CRUD_1.2.246.562.28.1\"]"}
   "USER-WITH-HAKUKOHDE-ORGANIZATION" {:employeeNumber "1.2.246.562.11.11111111000"
                                       :givenName      "Keijo"
                                       :sn             "Esimies"
                                       :description    "[\"APP_ATARU_EDITORI_CRUD_1.2.246.562.10.0439846\",\"APP_ATARU_HAKEMUS_CRUD_1.2.246.562.10.0439846\",\"APP_ATARU_EDITORI_CRUD_1.2.246.562.28.2\",\"APP_ATARU_HAKEMUS_CRUD_1.2.246.562.28.2\"]"}})

(defn- get-user [connection user-name]
  (first (ldap/search connection people-path-base {:filter (str "(uid=" user-name ")")})))

(defn get-virkailija-by-username
  [user-name]
  ; TODO this function should probably be in a service with a proper mock version
  (if (-> config :dev :fake-dependencies)
    (get fake-virkailija-value user-name (get fake-virkailija-value "DEVELOPER"))
    (let [connection (create-ldap-connection)
          user (get-user connection user-name)]
      (ldap/close connection)
      user)))

