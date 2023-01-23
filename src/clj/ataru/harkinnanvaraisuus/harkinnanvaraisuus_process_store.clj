(ns ataru.harkinnanvaraisuus.harkinnanvaraisuus-process-store
  (:require [ataru.db.db :refer [exec]]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/harkinnanvaraisuus-process-queries.sql")

;; Declare queries to keep linting happy
(declare yesql-upsert-harkinnanvaraisuus-process!)

(defn- execute [yesql-query-fn params]
  (exec :db yesql-query-fn params))


(defn upsert-harkinnanvaraisuus-process [application-id application-key haku-oid]
  (execute yesql-upsert-harkinnanvaraisuus-process! {:application_id application-id
                                                    :application_key application-key
                                                    :haku_oid haku-oid}))