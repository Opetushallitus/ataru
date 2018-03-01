(ns ataru.email.email-store
  (:require [ataru.db.db :as db]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/email-template-queries.sql")

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn create-or-update-email-template
  [form-key lang virkailija-oid content]
  (first (exec-db :db yesql-upsert-email-template!< {:form_key       form-key
                                                     :lang           lang
                                                     :virkailija_oid virkailija-oid
                                                     :haku_oid       ""
                                                     :content        content})))

(defn get-email-template
  [form-key lang]
  (first (exec-db :db yesql-get-email-template {:form_key form-key
                                                :lang     lang
                                                :haku_oid ""})))