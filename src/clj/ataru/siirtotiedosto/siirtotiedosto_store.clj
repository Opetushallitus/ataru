(ns ataru.siirtotiedosto.siirtotiedosto-store
  (:require [ataru.db.db :as db]
            [yesql.core :refer [defqueries]]
            [taoensso.timbre :as log]))


(defqueries "sql/siirtotiedosto-queries.sql")

(declare upsert-siirtotiedosto-data!)
(declare latest-siirtotiedosto-data)

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn get-latest-data []
  (log/info "Fetching latest siirtotiedosto data")
  (first (exec-db :db latest-siirtotiedosto-data {})))

(defn persist-siirtotiedosto-data [data]
  (log/info "Persisting siirtotiedosto data" data)
  (exec-db :db upsert-siirtotiedosto-data! data))