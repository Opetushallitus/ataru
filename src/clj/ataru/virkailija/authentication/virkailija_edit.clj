(ns ataru.virkailija.authentication.virkailija-edit
  (:require [ataru.db.db :as db]
            [yesql.core :as sql]
            [ataru.virkailija.user.ldap-client :as ldap])
  (:import (java.util UUID)))

(sql/defqueries "sql/virkailija-queries.sql")
(sql/defqueries "sql/virkailija-credentials-queries.sql")

(defn- upsert-virkailija [session]
  (when-let [virkailija (ldap/get-virkailija-by-username (-> session :identity :username))]
    (db/exec :db yesql-upsert-virkailija<! {:oid        (:employeeNumber virkailija)
                                            :first_name (:givenName virkailija)
                                            :last_name  (:sn virkailija)})))

(defn create-virkailija-credentials [session application-key]
  (when-let [virkailija (upsert-virkailija session)]
    (let [secret (str (UUID/randomUUID))]
      (db/exec :db yesql-upsert-virkailija-credentials<! {:oid             (:oid virkailija)
                                                          :secret          secret
                                                          :application_key application-key}))))

(defn invalidate-virkailija-credentials [virkailija-secret]
  (db/exec :db yesql-invalidate-virkailija-credentials! {:virkailija_secret virkailija-secret}))

(defn virkailija-secret-valid? [virkailija-secret]
  (-> (db/exec :db yesql-get-virkailija-secret-valid {:virkailija_secret virkailija-secret})
      first
      :valid))
