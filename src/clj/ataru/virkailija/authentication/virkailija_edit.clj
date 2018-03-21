(ns ataru.virkailija.authentication.virkailija-edit
  (:require [ataru.db.db :as db]
            [yesql.core :as sql]
            [ataru.organization-service.ldap-client :as ldap]
            [ataru.config.core :refer [config]]
            [ataru.util :as u]
            [clojure.java.jdbc :as jdbc]
            [camel-snake-kebab.core :as t]
            [camel-snake-kebab.extras :as te])
  (:import (java.util UUID)))

(sql/defqueries "sql/virkailija-queries.sql")
(sql/defqueries "sql/virkailija-credentials-queries.sql")

(defn create-virkailija-credentials [session application-key]
  (let [secret (str (UUID/randomUUID))]
    (db/exec :db yesql-upsert-virkailija-credentials<! {:oid             (:oid session)
                                                        :secret          secret
                                                        :application_key application-key})))

(defn invalidate-virkailija-credentials [virkailija-secret]
  (db/exec :db yesql-invalidate-virkailija-credentials! {:virkailija_secret virkailija-secret}))

(defn virkailija-secret-valid? [virkailija-secret]
  (-> (db/exec :db yesql-get-virkailija-secret-valid {:virkailija_secret virkailija-secret})
      first
      :valid))

(defn- get-virkailija-for-update [oid conn]
  (->> (yesql-get-virkailija-for-update {:oid oid}
                                        {:connection conn})
       (map (partial te/transform-keys t/->kebab-case-keyword))
       (first)))

(defn set-review-setting [review-setting session]
  {:pre [(-> review-setting :setting-kwd u/not-blank?)
         (-> review-setting :enabled some?)]}
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [virkailija (get-virkailija-for-update (:oid session) conn)
          settings   (-> virkailija
                         :settings
                         (assoc-in [:review (:setting-kwd review-setting)] (:enabled review-setting)))]
      (yesql-update-virkailija-settings! {:oid      (:oid session)
                                          :settings settings}
        {:connection conn})
      review-setting)))

(defn get-review-settings [session]
  (or (->> (db/exec :db yesql-get-virkailija {:oid (:oid session)})
           (eduction (map (partial te/transform-keys t/->kebab-case-keyword))
             (map :settings))
           (first))
      {:review {}}))
