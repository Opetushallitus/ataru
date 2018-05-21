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
            [taoensso.timbre :as log]))

(defn- get-application
  [application-id]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (let [application (-> (jdbc/query connection
                                      ["SELECT la.key AS key,
                                               la.person_oid AS person_oid,
                                               la.haku AS haku_oid,
                                               la.hakukohde AS hakukohde_oids
                                        FROM latest_applications AS la
                                        JOIN applications AS a ON a.key = la.key
                                        WHERE a.id = ?"
                                       application-id])
                          first
                          (clojure.set/rename-keys
                           {:person_oid     :person-oid
                            :haku_oid       :haku-oid
                            :hakukohde_oids :hakukohde-oids}))]
      (when (nil? application)
        (throw (new RuntimeException (str "Application " application-id
                                          " not found"))))
      (when (nil? (:person-oid application))
        (throw (new RuntimeException (str "Application " application-id
                                          " is not linked to a person"))))
      application)))

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

(defn- get-ylioppilas?
  [suoritus-service application]
  (suoritus-service/ylioppilas? suoritus-service (:person-oid application)))

(defn- query-eligibility-automatically-set?
  [connection application hakukohde]
  (->> (jdbc/query connection ["SELECT event_type = 'eligibility-state-automatically-changed' AS result
                                FROM application_events
                                WHERE id = (SELECT max(id)
                                            FROM application_events
                                            WHERE application_key = ?
                                              AND hakukohde = ?
                                              AND review_key = 'eligibility-state')"
                               (:key application)
                               (:oid hakukohde)])
       first
       :result))

(defn- insert-application-event
  [connection application hakukohde new-state]
  (jdbc/execute! connection ["INSERT INTO application_events
                              (new_review_state,
                               event_type,
                               application_key,
                               hakukohde,
                               review_key)
                              VALUES
                              (?,
                               'eligibility-state-automatically-changed',
                               ?,
                               ?,
                               'eligibility-state')"
                             new-state
                             (:key application)
                             (:oid hakukohde)]))

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

(defn- update-application-hakukohde-review
  [connection {:keys [application hakukohde from to]}]
  (when (->> (jdbc/execute! connection
                            ["INSERT INTO application_hakukohde_reviews
                              (application_key, requirement, state, hakukohde)
                              VALUES (?, 'eligibility-state', ?, ?)
                              ON CONFLICT (application_key, hakukohde, requirement)
                              DO UPDATE
                              SET state = EXCLUDED.state,
                                  modified_time = DEFAULT
                              WHERE application_hakukohde_reviews.state = ?"
                             (:key application)
                             to
                             (:oid hakukohde)
                             from])
             first
             (= 1))
    (insert-application-event connection application hakukohde to)
    (audit-log application hakukohde from to)))

(defn automatic-eligibility-if-ylioppilas
  [application
   haku
   ohjausparametrit
   now
   hakukohteet
   ylioppilas?
   eligibility-automatically-set?]
  (when (automatic-eligibility-if-ylioppilas-in-use? haku ohjausparametrit now)
    (->> hakukohteet
         (filter :ylioppilastutkintoAntaaHakukelpoisuuden)
         (keep (fn [hakukohde]
                 (cond ylioppilas?
                       {:from        "unreviewed"
                        :to          "eligible"
                        :application application
                        :hakukohde   hakukohde}
                       (eligibility-automatically-set? hakukohde)
                       {:from        "eligible"
                        :to          "unreviewed"
                        :application application
                        :hakukohde   hakukohde}))))))

(defn start-automatic-eligibility-if-ylioppilas-job
  [job-definitions application-id]
  (job/start-job job-definitions
                 "automatic-eligibility-if-ylioppilas-job"
                 {:application-id application-id}))

(defn automatic-eligibility-if-ylioppilas-job-step
  [{:keys [application-id]}
   {:keys [ohjausparametrit-service
           tarjonta-service
           suoritus-service]}]
  (let [application      (get-application application-id)
        haku             (get-haku tarjonta-service application)
        ohjausparametrit (get-ohjausparametrit ohjausparametrit-service
                                               application)
        now              (time/now)
        hakukohteet      (get-hakukohteet tarjonta-service application)
        ylioppilas?      (get-ylioppilas? suoritus-service application)]
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}
                               {:isolation :serializable}]
      (doseq [update (automatic-eligibility-if-ylioppilas
                      application
                      haku
                      ohjausparametrit
                      now
                      hakukohteet
                      ylioppilas?
                      (partial query-eligibility-automatically-set?
                               connection
                               application))]
        (update-application-hakukohde-review connection update))))
  {:transition {:id :final}})
