(ns ataru.virkailija.authentication.virkailija-edit
  (:require [ataru.db.db :refer [exec]]
            [yesql.core :refer [defqueries]]
            [ataru.virkailija.user.ldap-client :as ldap])
  (:import (java.util UUID)))

(defqueries "sql/virkailija-edit-queries.sql")

(defn create-virkailija-credentials [session application-key]
  (let [secret         (str (UUID/randomUUID))
        user-name      (-> session :identity :username)
        virkailija          (ldap/get-virkailija-by-username user-name)]
    (exec :db yesql-upsert-virkailija-credentials! {:secret          secret
                                                    :username        user-name
                                                    :oid             (:employeeNumber virkailija)
                                                    :application_key application-key
                                                    :first_name      (:givenName virkailija)
                                                    :last_name       (:sn virkailija)})
    {:secret secret}))
