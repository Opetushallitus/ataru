(ns ataru.harkinnanvaraisuus.harkinnanvaraisuus-job
  (:require [clj-time.core :as time]
            [ataru.harkinnanvaraisuus.harkinnanvaraisuus-process-store :as store]
            [clj-time.coerce :as coerce]
            [ataru.haku.haku-service :as haku-service]
            [ataru.applications.application-store :as application-store]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-util :as hutil]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol :as valintalaskentakoostepalvelu]))

(defn- assoc-only-harkinnanvarainen-to-application
  [application]
  {:id (:id application)
   :key (:key application)
   :harkinnanvarainen (hutil/does-application-belong-to-only-harkinnanvarainen-valinta? application)})

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
          harkinnanvaraisuudet-from-koostepalvelu (valintalaskentakoostepalvelu/hakemusten-harkinnanvaraisuus-valintalaskennasta valintalaskentakoostepalvelu-service application-keys-to-check)]
      (prn valid-haku-oids)
      (prn processes)
      (prn processes-that-can-be-skipped)
      (prn applications-to-check)
      (prn applications-with-harkinnanvaraisuus)
      (prn harkinnanvaraisuudet-from-koostepalvelu)
      (when (< 0 (count processes-that-can-be-skipped))
        (store/mark-do-not-check-harkinnanvaraisuus-processes processes-that-can-be-skipped))
      {:transition      {:id :to-next :step :initial}
       :updated-state   {:last-run-long (coerce/to-long now)}
       :next-activation (time/plus now (time/hours 1))})
     (catch Exception e
       (prn e)
       )))

(def job-definition {:steps {:initial check-harkinnanvaraisuus-step}
                     :type  "harkinnanvaraisuus-check"})