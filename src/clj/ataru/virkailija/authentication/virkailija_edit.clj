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

(defn create-virkailija-update-secret
  [session application-key]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (let [secret (str (UUID/randomUUID))]
      (jdbc/execute! connection ["INSERT INTO virkailija_update_secrets
                                  (virkailija_oid, application_key, secret)
                                  VALUES (?, ?, ?)"
                                 (get-in session [:identity :oid])
                                 application-key
                                 secret])
      secret)))

(defn invalidate-virkailija-update-secret
  [secret]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (jdbc/execute! connection ["UPDATE virkailija_update_secrets
                                SET valid = tstzrange(lower(valid), now(), '[)')
                                WHERE secret = ?"
                               secret])))

(defn virkailija-update-secret-valid?
  [secret]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (not (empty?
          (jdbc/query connection ["SELECT 1
                                   FROM virkailija_update_secrets
                                   WHERE secret = ? AND valid @> now()"
                                  secret])))))

(defn- get-virkailija-for-update [oid conn]
  (->> (yesql-get-virkailija-for-update {:oid oid}
                                        {:connection conn})
       (map (partial te/transform-keys t/->kebab-case-keyword))
       (first)))

(defn set-review-setting [review-setting session]
  {:pre [(-> review-setting :setting-kwd u/not-blank?)
         (-> review-setting :enabled some?)]}
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [virkailija (get-virkailija-for-update (-> session :identity :oid) conn)
          settings   (-> virkailija
                         :settings
                         (assoc-in [:review (:setting-kwd review-setting)] (:enabled review-setting)))]
      (yesql-update-virkailija-settings! {:oid      (-> session :identity :oid)
                                          :settings settings}
        {:connection conn})
      review-setting)))

(defn get-review-settings [session]
  (or (->> (db/exec :db yesql-get-virkailija {:oid (-> session :identity :oid)})
           (eduction (map (partial te/transform-keys t/->kebab-case-keyword))
             (map :settings))
           (first))
      {:review {}}))
