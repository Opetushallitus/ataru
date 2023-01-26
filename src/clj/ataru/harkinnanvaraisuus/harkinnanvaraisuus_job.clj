(ns ataru.harkinnanvaraisuus.harkinnanvaraisuus-job
  (:require [clj-time.core :as time]
            [ataru.harkinnanvaraisuus.harkinnanvaraisuus-process-store :as store]
            [clj-time.coerce :as coerce]
            [ataru.haku.haku-service :as haku-service]))

(defn check-harkinnanvaraisuus-step
  [{:keys [last-run-long]}
   {:keys [ohjausparametrit-service]}]
  (try
    (let [now       (time/now)
          processes (store/fetch-unprocessed-harkinnanvaraisuus-processes)
          valid-haku-oids (->> processes
                              (map #(:haku_oid %))
                              (distinct)
                              (filter #(not (haku-service/hakukierros-paattynyt? ohjausparametrit-service now %)))
                              (set))
          processids-where-check-can-be-skipped (->> processes
                                                     (filter #(not (contains? valid-haku-oids (:haku_oid %))))
                                                     (map #(:application_id %)))]
      (prn valid-haku-oids)
      (prn processes)
      (prn processids-where-check-can-be-skipped)
      (store/mark-do-not-check-harkinnanvaraisuus-processes processids-where-check-can-be-skipped)
      {:transition      {:id :to-next :step :initial}
       :updated-state   {:last-run-long (coerce/to-long now)}
       :next-activation (time/plus now (time/hours 1))})
     (catch Exception e
       (prn e)
       )))

(def job-definition {:steps {:initial check-harkinnanvaraisuus-step}
                     :type  "harkinnanvaraisuus-check"})