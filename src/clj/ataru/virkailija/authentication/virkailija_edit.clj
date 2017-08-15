(ns ataru.virkailija.authentication.virkailija-edit
  (:require [ataru.db.db :refer [exec]]
            [yesql.core :refer [defqueries]]
            [ataru.virkailija.user.ldap-client :as ldap])
  (:import (java.util UUID)))

(defqueries "sql/virkailija-queries.sql")
(defqueries "sql/virkailija-edit-queries.sql")

(defn- upsert-virkailija [session]
  (when-let [virkailija (ldap/get-virkailija-by-username (-> session :identity :username))]
    (exec :db yesql-upsert-virkailija<! {:oid        (:employeeNumber virkailija)
                                        :first_name (:givenName virkailija)
                                        :last_name  (:sn virkailija)})))

(defn create-virkailija-credentials [session application-key]
  (when-let [virkailija (upsert-virkailija session)]
    (let [secret (str (UUID/randomUUID))]
      (exec :db yesql-upsert-virkailija-credentials<! {:oid             (:oid virkailija)
                                                      :secret          secret
                                                      :application_key application-key}))))

(defn invalidate-virkailija-credentials [virkailija-secret]
  (exec :db yesql-invalidate-virkailija-credentials! {:virkailija_secret virkailija-secret}))

(defn virkailija-secret-valid? [virkailija-secret]
  (-> (exec :db yesql-get-virkailija-secret-valid {:virkailija_secret virkailija-secret})
      first
      :valid))
