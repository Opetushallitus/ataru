(ns ataru.virkailija.authentication.virkailija-edit
  (:require [ataru.db.db :refer [exec]]
            [yesql.core :refer [defqueries]]
            [ataru.virkailija.user.ldap-client :refer [get-virkailija-oid-by-username]])
  (:import (java.util UUID)))

(defqueries "sql/virkailija-edit-queries.sql")

(defn create-virkailija-credentials [session application-key]
  (let [secret         (str (UUID/randomUUID))
        user-name      (-> session :identity :username)
        virkailija-oid (get-virkailija-oid-by-username user-name)]
    (exec :db yesql-upsert-virkailija-credentials! {:secret          secret
                                                    :username        user-name
                                                    :oid             virkailija-oid
                                                    :application_key application-key})
    {:secret secret}))
