(ns ataru.virkailija.authentication.virkailija-edit
  (:require [ataru.db.db :as db]
            [yesql.core :as sql]
            [ataru.util :as u]
            [clojure.java.jdbc :as jdbc]
            [camel-snake-kebab.core :as t]
            [camel-snake-kebab.extras :as te])
  (:import (java.util UUID)))

(declare yesql-get-virkailija-for-update)
(declare yesql-get-virkailija)
(sql/defqueries "sql/virkailija-queries.sql")

(defn create-virkailija-create-secret
  [session]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (let [secret (str (UUID/randomUUID))]
      (jdbc/execute! connection ["INSERT INTO virkailija_create_secrets
                                  (virkailija_oid, secret)
                                  VALUES (?, ?)"
                                 (get-in session [:identity :oid])
                                 secret])
      secret)))

(defn invalidate-virkailija-create-secret
  [secret]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (jdbc/execute! connection ["UPDATE virkailija_create_secrets
                                SET valid = tstzrange(lower(valid), now(), '[)')
                                WHERE secret = ?"
                               secret])))

(defn virkailija-create-secret-valid?
  [secret]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (not (empty?
          (jdbc/query connection ["SELECT 1
                                   FROM virkailija_create_secrets
                                   WHERE secret = ? AND valid @> now()"
                                  secret])))))

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

(defn create-virkailija-rewrite-secret
  [session application-key]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (let [secret (str (UUID/randomUUID))]
      (jdbc/execute! connection ["INSERT INTO virkailija_rewrite_secrets
                                  (virkailija_oid, application_key, secret)
                                  VALUES (?, ?, ?)"
                                 (get-in session [:identity :oid])
                                 application-key
                                 secret])
      secret)))

(defn virkailija-rewrite-secret-valid?
  [secret]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (not (empty?
           (jdbc/query connection ["SELECT 1
                                   FROM virkailija_rewrite_secrets
                                   WHERE secret = ? AND valid @> now()"
                                   secret])))))

(defn virkailija-oid-with-rewrite-secret
  [secret]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                           (:virkailija_oid
                             (first
                              (jdbc/query connection ["SELECT virkailija_oid
                                                      FROM virkailija_rewrite_secrets
                                                      WHERE secret = ? AND valid @> now()"
                                                      secret])))))

(defn invalidate-virkailija-update-and-rewrite-secret
  [secret]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (jdbc/execute! connection ["UPDATE virkailija_update_secrets
                                SET valid = tstzrange(lower(valid), now(), '[)')
                                WHERE secret = ?"
                               secret])
    (jdbc/execute! connection ["UPDATE virkailija_rewrite_secrets
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

(defn virkailija-oid-with-update-secret
  [secret]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                           (:virkailija_oid
                             (first
                               (jdbc/query connection ["SELECT virkailija_oid
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
