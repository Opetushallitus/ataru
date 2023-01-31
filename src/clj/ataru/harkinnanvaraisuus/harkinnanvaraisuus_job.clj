(ns ataru.harkinnanvaraisuus.harkinnanvaraisuus-job
  (:require [clj-time.core :as time]
            [ataru.harkinnanvaraisuus.harkinnanvaraisuus-process-store :as store]
            [clj-time.coerce :as coerce]
            [ataru.haku.haku-service :as haku-service]
            [ataru.applications.application-store :as application-store]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-util :as hutil]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol :as valintalaskentakoostepalvelu]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-types :as hartyp]))

(defn- assoc-only-harkinnanvarainen-to-application
  [application]
  (assoc application :harkinnanvarainen-only? (hutil/does-application-belong-to-only-harkinnanvarainen-valinta? application)))

(def sure-harkinnanvarainen-only-reasons
  (-> [(:sure-ei-paattotodistusta hartyp/harkinnanvaraisuus-reasons) (:sure-yks-mat-ai hartyp/harkinnanvaraisuus-reasons)]
      set))

(defn- assoc-valintalaskentakoostepalvelu-harkinnainen-only
  [application-with-harkinnanvaraisuus applications-from-valintalaskentakoostepalvelu]
  (let [application-id (keyword (:key application-with-harkinnanvaraisuus))
        application-from-kooste (application-id applications-from-valintalaskentakoostepalvelu)
        has-only-sure-harkinnanvarainen? (->> (:hakutoiveet application-from-kooste)
                                              (map #(:harkinnanvaraisuudenSyy %))
                                              (some #(contains? sure-harkinnanvarainen-only-reasons %)))]
    (assoc application-with-harkinnanvaraisuus :sure-harkinnanvarainen-only? (boolean has-only-sure-harkinnanvarainen?))))

(defn- handle-harkinnanvaraisuus-processes-to-save
  [applications-with-harkinnanvaraisuus checked-time]
  (let [apps-without-email-job (filter #(= (:sure-harkinnanvarainen-only? %) (:harkinnanvarainen-only? %)) applications-with-harkinnanvaraisuus)
        email-job-apps (filter #(not (= (:sure-harkinnanvarainen-only? %) (:harkinnanvarainen-only? %))) applications-with-harkinnanvaraisuus)]
    (prn "Saving PROCESSES")
    (prn apps-without-email-job)
    (prn (count apps-without-email-job))
    (prn email-job-apps)
    (when (< 0 (count apps-without-email-job))
      (doall
        (for [app apps-without-email-job]
          (store/yesql-update-harkinnanvaraisuus-process (:id app) (:harkinnanvarainen-only? app) checked-time))))
    (when (< 0 (count email-job-apps))
      (doall
        (for [app email-job-apps]
          (store/yesql-update-harkinnanvaraisuus-process (:id app) (:sure-harkinnanvarainen-only? app) checked-time))))))

(defn check-harkinnanvaraisuus-step
  [{:keys [last-run-long]}
   {:keys [ohjausparametrit-service valintalaskentakoostepalvelu-service]}]
  (try
    (let [now       (time/now)
          processes (store/fetch-unprocessed-harkinnanvaraisuus-processes)
          valid-haku-oids (->> processes
                              (map #(:haku_oid %))
                              (distinct)
                              (filter #(not (haku-service/harkinnanvarainen-valinta-paattynyt? ohjausparametrit-service now %)))
                              (set))
          processids-where-check-can-be-skipped (->> processes
                                                     (filter #(not (contains? valid-haku-oids (:haku_oid %))))
                                                     (map #(:application_id %))
                                                     (set))
          processes-to-check (filter #(not (contains? processids-where-check-can-be-skipped (:application_id %))) processes)
          applications-to-check (->> processes-to-check
                                     (map #(application-store/get-application (:application_id %)))
                                     (filter #(not (hutil/can-skip-recheck-for-yks-ma-ai %))))
          application-keys-to-check (map #(:key %) applications-to-check)
          application-ids-to-check (->> applications-to-check
                                        (map #(:id %))
                                        (set))
          processes-that-can-be-skipped (->> processes-to-check
                                             (filter #(not (contains? application-ids-to-check (:application_id %))))
                                             (map #(:application_id %))
                                             (set))
          applications-with-harkinnanvaraisuus (->> applications-to-check
                                                    (map #(assoc-only-harkinnanvarainen-to-application %)))
          harkinnanvaraisuudet-from-koostepalvelu (valintalaskentakoostepalvelu/hakemusten-harkinnanvaraisuus-valintalaskennasta valintalaskentakoostepalvelu-service application-keys-to-check)
          applications-to-save (->> applications-with-harkinnanvaraisuus
                                    (map #(assoc-valintalaskentakoostepalvelu-harkinnainen-only % harkinnanvaraisuudet-from-koostepalvelu)))]
      (prn valid-haku-oids)
      (prn processes)
      (prn processes-that-can-be-skipped)
      (prn applications-to-check)
      (prn applications-with-harkinnanvaraisuus)
      (prn harkinnanvaraisuudet-from-koostepalvelu)
      (prn applications-to-save)
      (when (< 0 (count processes-that-can-be-skipped))
        (store/mark-do-not-check-harkinnanvaraisuus-processes processes-that-can-be-skipped))
      (when (< 0 (count applications-to-save))
        (handle-harkinnanvaraisuus-processes-to-save applications-to-save now))
      {:transition      {:id :to-next :step :initial}
       :updated-state   {:last-run-long (coerce/to-long now)}
       :next-activation (time/plus now (time/hours 1))})
     (catch Exception e
       (prn e)
       )))

(def job-definition {:steps {:initial check-harkinnanvaraisuus-step}
                     :type  "harkinnanvaraisuus-check"})