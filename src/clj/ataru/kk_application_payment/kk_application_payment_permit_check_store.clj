(ns ataru.kk-application-payment.kk-application-payment-permit-check-store
  "Storage of virkailija's notes on kk application payment residence permit attachments. Kept apart from the
   access controlled handling of the notes, as payment determination depends on them and the access control
   depends on payment determination."
  (:require [ataru.db.db :as db]
            [yesql.core :as yesql]))

(declare yesql-get-kk-application-payment-permit-checks)
(declare yesql-upsert-kk-application-payment-permit-check)
(declare yesql-delete-kk-application-payment-permit-checks)
(yesql/defqueries "sql/kk-application-payment-permit-check-queries.sql")

(defn get-permit-checks
  ([application-key]
   (db/exec :db yesql-get-kk-application-payment-permit-checks {:application_key application-key}))
  ([connection application-key]
   (yesql-get-kk-application-payment-permit-checks {:application_key application-key} {:connection connection})))

(defn- check-of-attachment [checks attachment-key]
  (some #(when (= attachment-key (:attachment-key %)) %) checks))

(defn get-permit-check
  ([application-key attachment-key]
   (check-of-attachment (get-permit-checks application-key) attachment-key))
  ([connection application-key attachment-key]
   (check-of-attachment (get-permit-checks connection application-key) attachment-key)))

(defn upsert-permit-check!
  [connection check]
  (first (yesql-upsert-kk-application-payment-permit-check check {:connection connection})))

(defn delete-permit-checks!
  [connection application-key modified-until]
  (yesql-delete-kk-application-payment-permit-checks
    {:application_key application-key
     :modified_until  modified-until}
    {:connection connection}))
