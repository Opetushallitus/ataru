(ns ataru.harkinnanvaraisuus.harkinnanvaraisuus-process-store
  (:require [ataru.db.db :refer [exec]]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/harkinnanvaraisuus-process-queries.sql")

;; Declare queries to keep linting happy
(declare yesql-upsert-harkinnanvaraisuus-process!)
(declare yesql-fetch-harkinnanvaraisuus-unprocessed)
(declare yesql-skip-checking-harkinnanvaraisuus-processes!)
(declare yesql-update-harkinnanvaraisuus-process!)
(declare yesql-fetch-checked-harkinnanvaraisuus-processes)

(defn- execute [yesql-query-fn params]
  (exec :db yesql-query-fn params))

(defn upsert-harkinnanvaraisuus-process [application-id application-key haku-oid]
  (execute yesql-upsert-harkinnanvaraisuus-process! {:application_id application-id
                                                    :application_key application-key
                                                    :haku_oid haku-oid}))

(defn fetch-unprocessed-harkinnanvaraisuus-processes []
  (execute yesql-fetch-harkinnanvaraisuus-unprocessed nil))

(defn mark-do-not-check-harkinnanvaraisuus-processes [ids]
  (execute yesql-skip-checking-harkinnanvaraisuus-processes! {:ids ids}))

(defn update-harkinnanvaraisuus-process
  [application-id harkinnanvarainen-only? checked-time]
  (execute yesql-update-harkinnanvaraisuus-process! {:application_id application-id
                                                     :harkinnanvarainen_only harkinnanvarainen-only?
                                                     :last_checked checked-time}))

(defn fetch-checked-harkinnanvaraisuus-processes [before-this]
  (execute yesql-fetch-checked-harkinnanvaraisuus-processes {:before_this before-this}))