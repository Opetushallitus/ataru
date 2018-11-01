(ns ataru.forms.hakukohderyhmat
  (:require [ataru.db.db :as db]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.util :as util]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [yesql.core :refer [defqueries]])
  (:import org.postgresql.util.PSQLException))

(defqueries "sql/hakukohderyhmat-queries.sql")

(defn- hakukohteiden-hakukohderyhmat
  [tarjonta-service ryhmat]
  (reduce #(->> %2
                :ryhmaliitokset
                (map :ryhmaOid)
                set
                (assoc %1 (:oid %2)))
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
  (if-let [ryhmat (seq (db/exec :db yesql-rajaavat-hakukohderyhmat
                                {:haku_oid haku-oid}))]
    {:ryhmat        (map #(dissoc % :last-modified) ryhmat)
     :last-modified (:last-modified (first ryhmat))}
    {:ryhmat []}))

(defn insert-rajaava-hakukohderyhma
  [ryhma]
  (when-let [ryhma (try
                     (first
                      (db/exec :db yesql-insert-rajaava-hakukohderyhma
                               {:haku_oid           (:haku-oid ryhma)
                                :hakukohderyhma_oid (:hakukohderyhma-oid ryhma)
                                :raja               (:raja ryhma)}))
                     (catch PSQLException e nil))]
    {:ryhma         (dissoc ryhma :last-modified)
     :last-modified (:last-modified ryhma)}))

(defn update-rajaava-hakukohderyhma
  [ryhma if-unmodified-since]
  (when-let [ryhma (first
                    (db/exec :db yesql-update-rajaava-hakukohderyhma
                             {:haku_oid            (:haku-oid ryhma)
                              :hakukohderyhma_oid  (:hakukohderyhma-oid ryhma)
                              :raja                (:raja ryhma)
                              :if_unmodified_since if-unmodified-since}))]
    {:ryhma         (dissoc ryhma :last-modified)
     :last-modified (:last-modified ryhma)}))

(defn delete-rajaava-hakukohderyhma
  [haku-oid hakukohderyhma-oid]
  (db/exec :db yesql-delete-rajaava-hakukohderyhma!
           {:haku_oid           haku-oid
            :hakukohderyhma_oid hakukohderyhma-oid}))

(defn priorisoivat-hakukohderyhmat
  [tarjonta-service haku-oid]
  (if-let [ryhmat (seq (db/exec :db yesql-priorisoivat-hakukohderyhmat
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
                      (db/exec :db yesql-insert-priorisoiva-hakukohderyhma
                               {:haku_oid           (:haku-oid ryhma)
                                :hakukohderyhma_oid (:hakukohderyhma-oid ryhma)
                                :prioriteetit       (json/generate-string (:prioriteetit ryhma))}))
                     (catch PSQLException e nil))]
    {:ryhma         (dissoc ryhma :last-modified)
     :last-modified (:last-modified ryhma)}))

(defn update-priorisoiva-hakukohderyhma
  [ryhma if-unmodified-since]
  (when-let [ryhma (first
                    (db/exec :db yesql-update-priorisoiva-hakukohderyhma
                             {:haku_oid            (:haku-oid ryhma)
                              :hakukohderyhma_oid  (:hakukohderyhma-oid ryhma)
                              :prioriteetit        (json/generate-string (:prioriteetit ryhma))
                              :if_unmodified_since if-unmodified-since}))]
    {:ryhma         (dissoc ryhma :last-modified)
     :last-modified (:last-modified ryhma)}))

(defn delete-priorisoiva-hakukohderyhma
  [haku-oid hakukohderyhma-oid]
  (db/exec :db yesql-delete-priorisoiva-hakukohderyhma!
           {:haku_oid           haku-oid
            :hakukohderyhma_oid hakukohderyhma-oid}))
