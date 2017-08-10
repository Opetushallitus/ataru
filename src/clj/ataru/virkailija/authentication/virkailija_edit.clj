(ns ataru.virkailija.authentication.virkailija-edit
  (:require [ataru.db.db :refer [exec]]
            [yesql.core :refer [defqueries]])
  (:import (java.util UUID)))

(defqueries "sql/virkailija-edit-queries.sql")

(defn create-hakija-credentials [session application-key]
  (let [secret         (str (UUID/randomUUID))
        virkailija-oid "tsers"]
    (exec :db yesql-upsert-virkailija-credentials! {:secret secret
                                                 :username (:username session)
                                                 :oid virkailija-oid
                                                 :application_key application-key})
    {:secret secret}))
