(ns ataru.forms.hakukohderyhmat
  (:require [ataru.db.db :as db]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer [defqueries]])
  (:import org.postgresql.util.PSQLException))

(declare yesql-rajaavat-hakukohderyhmat)
(declare yesql-insert-rajaava-hakukohderyhma)
(declare yesql-update-rajaava-hakukohderyhma)
(declare yesql-delete-rajaava-hakukohderyhma!)
(declare yesql-priorisoivat-hakukohderyhmat)
(declare yesql-insert-priorisoiva-hakukohderyhma)
(declare yesql-update-priorisoiva-hakukohderyhma)
(declare yesql-delete-priorisoiva-hakukohderyhma!)
(defqueries "sql/hakukohderyhmat-queries.sql")

(defn- db-exec [query params]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (query params {:connection connection})))

(defn- hakukohteiden-hakukohderyhmat
  [tarjonta-service ryhmat]
  (reduce #(assoc %1 (:oid %2) (set (:ryhmaliitokset %2)))
          {}
          (some->> ryhmat
                   (mapcat :prioriteetit)
                   (mapcat identity)
                   distinct
                   seq
                   (tarjonta/get-hakukohteet tarjonta-service))))

(defn- remove-hakukohteet-not-in-hakukohderyhma
  [hakukohderyhmat {:keys [hakukohderyhma-oid] :as ryhma}]
  (update ryhma :prioriteetit
          (fn [prioriteetit]
            (->> prioriteetit
                 (map (fn [prioriteetin-hakukohteet]
                        (filter #(contains? (get hakukohderyhmat %)
                                            hakukohderyhma-oid)
                                prioriteetin-hakukohteet)))
                 (remove empty?)))))

(defn rajaavat-hakukohderyhmat
  [haku-oid]
  (if-let [ryhmat (seq (db-exec yesql-rajaavat-hakukohderyhmat
                                {:haku_oid haku-oid}))]
    {:ryhmat        (map #(dissoc % :last-modified) ryhmat)
     :last-modified (:last-modified (first ryhmat))}
    {:ryhmat []}))

(defn insert-rajaava-hakukohderyhma
  [ryhma]
  (when-let [ryhma (try
                     (first
                      (db-exec yesql-insert-rajaava-hakukohderyhma
                               {:haku_oid           (:haku-oid ryhma)
                                :hakukohderyhma_oid (:hakukohderyhma-oid ryhma)
                                :raja               (:raja ryhma)}))
                     (catch PSQLException _ nil))]
    {:ryhma         (dissoc ryhma :last-modified)
     :last-modified (:last-modified ryhma)}))

(defn update-rajaava-hakukohderyhma
  [ryhma if-unmodified-since]
  (when-let [ryhma (first
                    (db-exec yesql-update-rajaava-hakukohderyhma
                             {:haku_oid            (:haku-oid ryhma)
                              :hakukohderyhma_oid  (:hakukohderyhma-oid ryhma)
                              :raja                (:raja ryhma)
                              :if_unmodified_since if-unmodified-since}))]
    {:ryhma         (dissoc ryhma :last-modified)
     :last-modified (:last-modified ryhma)}))

(defn delete-rajaava-hakukohderyhma
  [haku-oid hakukohderyhma-oid]
  (db-exec yesql-delete-rajaava-hakukohderyhma!
           {:haku_oid           haku-oid
            :hakukohderyhma_oid hakukohderyhma-oid}))

(defn priorisoivat-hakukohderyhmat
  [tarjonta-service haku-oid]
  (if-let [ryhmat (seq (db-exec yesql-priorisoivat-hakukohderyhmat
                                {:haku_oid haku-oid}))]
    {:ryhmat        (->> ryhmat
                         (map (partial remove-hakukohteet-not-in-hakukohderyhma
                                       (hakukohteiden-hakukohderyhmat
                                        tarjonta-service
                                        ryhmat)))
                         (map #(dissoc % :last-modified)))
     :last-modified (:last-modified (first ryhmat))}
    {:ryhmat []}))

(defn insert-priorisoiva-hakukohderyhma
  [ryhma]
  (when-let [ryhma (try
                     (first
                      (db-exec yesql-insert-priorisoiva-hakukohderyhma
                               {:haku_oid           (:haku-oid ryhma)
                                :hakukohderyhma_oid (:hakukohderyhma-oid ryhma)
                                :prioriteetit       (json/generate-string (:prioriteetit ryhma))}))
                     (catch PSQLException _ nil))]
    {:ryhma         (dissoc ryhma :last-modified)
     :last-modified (:last-modified ryhma)}))

(defn update-priorisoiva-hakukohderyhma
  [ryhma if-unmodified-since]
  (when-let [ryhma (first
                    (db-exec yesql-update-priorisoiva-hakukohderyhma
                             {:haku_oid            (:haku-oid ryhma)
                              :hakukohderyhma_oid  (:hakukohderyhma-oid ryhma)
                              :prioriteetit        (json/generate-string (:prioriteetit ryhma))
                              :if_unmodified_since if-unmodified-since}))]
    {:ryhma         (dissoc ryhma :last-modified)
     :last-modified (:last-modified ryhma)}))

(defn delete-priorisoiva-hakukohderyhma
  [haku-oid hakukohderyhma-oid]
  (db-exec yesql-delete-priorisoiva-hakukohderyhma!
           {:haku_oid           haku-oid
            :hakukohderyhma_oid hakukohderyhma-oid}))
