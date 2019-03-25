(ns ataru.applications.automatic-eligibility
  (:require [ataru.background-job.job :as job]
            [ataru.db.db :as db]
            [ataru.log.audit-log :as audit-log]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit-service]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/automatic-eligibility-queries.sql")

(defn- get-application
  [application-id]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (let [application (first (yesql-get-application {:id application-id}
                                                    {:connection connection}))]
      (when (nil? application)
        (throw (new RuntimeException (str "Application " application-id
                                          " not found"))))
      (when (some? (:person-oid application))
        application))))

(defn- get-haku
  [tarjonta-service application]
  (when-let [haku-oid (:haku-oid application)]
    (if-let [haku (tarjonta-service/get-haku tarjonta-service haku-oid)]
      haku
      (throw (new RuntimeException (str "Haku " haku-oid " not found"))))))

(defn- get-ohjausparametrit
  [ohjausparametrit-service application]
  (when-let [haku-oid (:haku-oid application)]
    (if-let [ohjausparametrit (ohjausparametrit-service/get-parametri
                               ohjausparametrit-service
                               haku-oid)]
      ohjausparametrit
      (throw (new RuntimeException (str "Ohjausparametrit for haku " haku-oid
                                        " not found"))))))

(defn- automatic-eligibility-if-ylioppilas-in-use?
  [haku ohjausparametrit now]
  (and (some? haku)
       (:ylioppilastutkintoAntaaHakukelpoisuuden haku)
       (if-let [automatic-eligibility-ends
                (some-> (get-in ohjausparametrit [:PH_AHP :date])
                        coerce/from-long)]
         (or (time/before? now automatic-eligibility-ends)
             (do (log/warn "PH_AHP" automatic-eligibility-ends
                           "passed in haku" (:oid haku))
                 false))
         true)))

(defn- get-hakukohteet
  [tarjonta-service application]
  (let [hakukohde-oids (:hakukohde-oids application)
        hakukohteet    (tarjonta-service/get-hakukohteet tarjonta-service
                                                         hakukohde-oids)]
    (when-let [missing-oids (seq (clojure.set/difference
                                  (set hakukohde-oids)
                                  (set (map :oid hakukohteet))))]
      (throw (new RuntimeException
                  (str "Hakukohteet " (clojure.string/join ", " missing-oids)
                       " not found"))))
    hakukohteet))

(defn- get-ylioppilas-tai-ammatillinen?
  [suoritus-service application]
  (suoritus-service/ylioppilas-tai-ammatillinen? suoritus-service (:person-oid application)))

(defn- insert-application-event
  [connection application hakukohde new-state]
  (when (not= 1 (yesql-insert-eligibility-state-automatically-changed-event!
                 {:state           new-state
                  :application_key (:key application)
                  :hakukohde       (:oid hakukohde)}
                 {:connection connection}))
    (throw (new RuntimeException
                (str "Could not insert eligibility-state-automatically-changed event"
                     " with state " new-state
                     " and application key " (:key application)
                     " and hakukohde " (:oid hakukohde))))))

(defn- audit-log
  [application hakukohde new-state old-state]
  (audit-log/log {:new       {:application_key (:key application)
                              :requirement     "eligibility-state"
                              :state           new-state
                              :hakukohde       (:oid hakukohde)}
                  :old       {:application_key (:key application)
                              :requirement     "eligibility-state"
                              :state           old-state
                              :hakukohde       (:oid hakukohde)}
                  :id        "automatic-eligibility-check"
                  :operation audit-log/operation-modify}))

(defn- set-eligible
  [connection application hakukohde]
  (case (yesql-from-unreviewed-to-eligible!
         {:application_key (:key application)
          :hakukohde       (:oid hakukohde)}
         {:connection connection})
    0 false
    1 true
    (throw (new RuntimeException
                (str "Updated more than one application_hakukohde_review row"
                     " from unreviewed to eligible"
                     " with application key " (:key application)
                     " and hakukohde " (:oid hakukohde))))))

(defn- set-unreviewed
  [connection application hakukohde]
  (case (yesql-from-eligible-to-unreviewed!
         {:application_key (:key application)
          :hakukohde       (:oid hakukohde)}
         {:connection connection})
    0 false
    1 true
    (throw (new RuntimeException
                (str "Updated more than one application_hakukohde_review row"
                     " from eligible to unreviewed"
                     " with application key " (:key application)
                     " and hakukohde " (:oid hakukohde))))))

(defn update-application-hakukohde-review
  [connection {:keys [application hakukohde from to]}]
  (when (case to
          "eligible"
          (set-eligible connection application hakukohde)
          "unreviewed"
          (set-unreviewed connection application hakukohde))
    (insert-application-event connection application hakukohde to)
    (audit-log application hakukohde from to)))

(defn automatic-eligibility-if-ylioppilas
  [application
   haku
   ohjausparametrit
   now
   hakukohteet
   ylioppilas-tai-ammatillinen?]
  (when (automatic-eligibility-if-ylioppilas-in-use? haku ohjausparametrit now)
    (->> hakukohteet
         (filter :ylioppilastutkintoAntaaHakukelpoisuuden)
         (keep (fn [hakukohde]
                 (if ylioppilas-tai-ammatillinen?
                   {:from        "unreviewed"
                    :to          "eligible"
                    :application application
                    :hakukohde   hakukohde}
                   {:from        "eligible"
                    :to          "unreviewed"
                    :application application
                    :hakukohde   hakukohde}))))))

(defn start-automatic-eligibility-if-ylioppilas-job
  [job-runner application-id]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (job/start-job job-runner
                   connection
                   "automatic-eligibility-if-ylioppilas-job"
                   {:application-id application-id})))

(defn automatic-eligibility-if-ylioppilas-job-step
  [{:keys [application-id]}
   {:keys [ohjausparametrit-service
           tarjonta-service
           suoritus-service]}]
  (if-let [application (get-application application-id)]
    (let [haku                         (get-haku tarjonta-service application)
          ohjausparametrit             (get-ohjausparametrit ohjausparametrit-service
                                                             application)
          hakukohteet                  (get-hakukohteet tarjonta-service application)
          ylioppilas-tai-ammatillinen? (get-ylioppilas-tai-ammatillinen? suoritus-service application)
          now                          (time/now)]
      (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}
                                 {:isolation :serializable}]
        (doseq [update (automatic-eligibility-if-ylioppilas
                        application
                        haku
                        ohjausparametrit
                        now
                        hakukohteet
                        ylioppilas-tai-ammatillinen?)]
          (update-application-hakukohde-review connection update)))
      {:transition {:id :final}})
    {:transition {:id :retry}}))

(defn- get-application-ids
  [suoritukset]
  (when-let [person-oids (seq (distinct (keep :person-oid suoritukset)))]
    (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
      (map :id (yesql-get-application-ids {:person_oids person-oids}
                                          {:connection connection})))))

(defn start-automatic-eligibility-if-ylioppilas-job-job-step
  [{:keys [last-run-long]} job-runner]
  (let [now         (time/now)
        suoritukset (suoritus-service/ylioppilas-ja-ammatilliset-suoritukset-modified-since
                     (:suoritus-service job-runner)
                     (coerce/from-long last-run-long))]
    (doseq [application-id (get-application-ids suoritukset)]
      (start-automatic-eligibility-if-ylioppilas-job job-runner
                                                     application-id))
    {:transition      {:id :to-next :step :initial}
     :updated-state   {:last-run-long (coerce/to-long now)}
     :next-activation (time/plus now (time/days 1))}))
