(ns ataru.applications.automatic-eligibility
  (:require [ataru.background-job.job :as job]
            [ataru.component-data.person-info-module :as person-info-module]
            [ataru.db.db :as db]
            [ataru.forms.form-store :as form-store]
            [ataru.log.audit-log :as audit-log]
            [ataru.cache.cache-service :as cache-service]
            [ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu-service :as hakukohderyhmapalvelu-service]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit-service]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]
            [clojure.java.jdbc :as jdbc]
            [clojure.set]
            [clojure.string]
            [taoensso.timbre :as log]
            [yesql.core :refer [defqueries]]))

(declare yesql-insert-eligibility-state-automatically-changed-event!)
(declare yesql-from-unreviewed-to-eligible!)
(declare yesql-from-eligible-to-unreviewed!)
(defqueries "sql/automatic-eligibility-queries.sql")

(defn- get-application
  [application-id]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (if-let [application (first (yesql-get-application {:id application-id}
                                                       {:connection connection}))]
      application
      (throw (new RuntimeException (str "Application " application-id
                                        " not found"))))))

(defn- get-haku
  [tarjonta-service application]
  (when-let [haku-oid (:haku-oid application)]
    (if-let [haku (tarjonta-service/get-haku tarjonta-service haku-oid)]
      haku
      (throw (new RuntimeException (str "Haku " haku-oid " not found"))))))

(defn- get-ohjausparametrit
  [ohjausparametrit-service application]
  (when-let [haku-oid (:haku-oid application)]
    (ohjausparametrit-service/get-parametri
     ohjausparametrit-service
     haku-oid)))

(defn- automatic-eligibility-if-ylioppilas-in-use?
  [haku ohjausparametrit now]
  (and (some? haku)
       (:ylioppilastutkinto-antaa-hakukelpoisuuden? haku)
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
  [audit-logger application hakukohde new-state old-state]
  (audit-log/log audit-logger
                 {:new       {:application_key (:key application)
                              :requirement     "eligibility-state"
                              :state           new-state
                              :hakukohde       (:oid hakukohde)}
                  :old       {:application_key (:key application)
                              :requirement     "eligibility-state"
                              :state           old-state
                              :hakukohde       (:oid hakukohde)}
                  :id        {:applicationOid (:key application)
                              :hakukohdeOid   (:oid hakukohde)
                              :requirement    "eligibility-state"}
                  :session   nil
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
  [connection audit-logger {:keys [application hakukohde from to]}]
  (when (case to
          "eligible"
          (set-eligible connection application hakukohde)
          "unreviewed"
          (set-unreviewed connection application hakukohde))
        (insert-application-event connection application hakukohde to)
        (audit-log audit-logger application hakukohde from to)))

(defn- automatic-eligibility-if-yo-amm-in-hakukohderyhma?
  [hakukohde hakukohderyhmapalvelu-service hakukohderyhma-settings-cache]
  (let [hakukohderyhmat (hakukohderyhmapalvelu-service/get-hakukohderyhma-oids-for-hakukohde hakukohderyhmapalvelu-service (:oid hakukohde))
        settings (map #(cache-service/get-from hakukohderyhma-settings-cache %) hakukohderyhmat)]
    (true? (some #(= (:yo-amm-autom-hakukelpoisuus %) true) settings))))

(defn- is-tarjonta-haku?
  [haku]
  (< (count (:oid haku)) 28))

(defn automatic-eligibility-if-ylioppilas
  [application
   haku
   ohjausparametrit
   now
   hakukohteet
   ylioppilas-tai-ammatillinen?
   hakukohderyhmapalvelu-service
   hakukohderyhma-settings-cache]
  (if (is-tarjonta-haku? haku)
    (when (automatic-eligibility-if-ylioppilas-in-use? haku ohjausparametrit now)
      (->> hakukohteet
           (filter :ylioppilastutkinto-antaa-hakukelpoisuuden?)
           (keep (fn [hakukohde]
                   (if ylioppilas-tai-ammatillinen?
                     {:from        "unreviewed"
                      :to          "eligible"
                      :application application
                      :hakukohde   hakukohde}
                     {:from        "eligible"
                      :to          "unreviewed"
                      :application application
                      :hakukohde   hakukohde})))))
    (doall
      (map (fn [hakukohde]
             (if (and ylioppilas-tai-ammatillinen? (automatic-eligibility-if-yo-amm-in-hakukohderyhma? hakukohde hakukohderyhmapalvelu-service hakukohderyhma-settings-cache))
               {:from        "unreviewed"
                :to          "eligible"
                :application application
                :hakukohde   hakukohde}
               {:from        "eligible"
                :to          "unreviewed"
                :application application
                :hakukohde   hakukohde}))
           hakukohteet))))


(defn start-automatic-eligibility-if-ylioppilas-job
  [job-runner application-id]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (job/start-job job-runner
                   connection
                   "automatic-eligibility-if-ylioppilas-job"
                   {:application-id application-id})))

(defn automatic-eligibility-if-ylioppilas-job-handler
  [{:keys [application-id]}
   {:keys [hakukohderyhmapalvelu-service
           hakukohderyhma-settings-cache
           ohjausparametrit-service
           tarjonta-service
           suoritus-service
           audit-logger]}]
  (let [application (get-application application-id)]
    (cond (some? (:person-oid application))
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
                                                       ylioppilas-tai-ammatillinen?
                                                       hakukohderyhmapalvelu-service
                                                       hakukohderyhma-settings-cache)]
                                        (update-application-hakukohde-review connection audit-logger update))))
          (person-info-module/muu-person-info-module?
           (form-store/fetch-by-id (:form-id application)))
          nil
          :else
          (throw (Exception. "automatic-eligibility-if-ylioppilas-job failed")))))

(defn- get-application-ids
  [suoritukset]
  (when-let [person-oids (seq (distinct (keep :person-oid suoritukset)))]
    (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
      (map :id (yesql-get-application-ids {:person_oids person-oids}
                                          {:connection connection})))))

(defonce suoritus-chunk-size 10000)
(defn start-automatic-eligibility-if-ylioppilas-job-job-handler
  [{:keys [last-run-long]} job-runner]
  (let [now                   (time/now)
        suoritukset           (suoritus-service/ylioppilas-ja-ammatilliset-suoritukset-modified-since
                                (:suoritus-service job-runner)
                                (coerce/from-long last-run-long))
        suoritus-chunks       (partition-all suoritus-chunk-size suoritukset)
        suoritus-chunks-count (count suoritus-chunks)]
    (log/info (str "Starting automatic eligibility job. Chunks (" suoritus-chunk-size  " suoritus per chunk): "
                   suoritus-chunks-count ". Count of suoritukset: " (count suoritukset) ". Modified-since: "
                   (coerce/from-long last-run-long)))
    (doseq [[n suoritus-chunk] (map-indexed #(vector (+ %1 1) %2) suoritus-chunks)]
      (let [application-ids (get-application-ids suoritus-chunk)]
        (log/info (str "Check automatic eligibility for chunk " n "/" suoritus-chunks-count ". Count: " (count application-ids)))
        (doseq [application-id application-ids]
          (start-automatic-eligibility-if-ylioppilas-job job-runner application-id))))
    {:transition      {:id :to-next :step :initial}
     :updated-state   {:last-run-long (coerce/to-long now)}
     :next-activation (time/plus now (time/days 1))}))
