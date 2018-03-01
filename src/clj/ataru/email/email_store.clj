(ns ataru.email.email-store
  (:require [ataru.db.db :as db]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer [defqueries]]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]))

(defqueries "sql/email-template-queries.sql")

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn create-or-update-email-template
  [form-key lang session content]
  (let [virkailija (virkailija-edit/upsert-virkailija session)]
    (exec-db :db yesql-upsert-email-template!< {:form_key       form-key
                                                :lang           lang
                                                :virkailija_oid (:oid virkailija)
                                                :haku_oid       nil
                                                :content        content})))
