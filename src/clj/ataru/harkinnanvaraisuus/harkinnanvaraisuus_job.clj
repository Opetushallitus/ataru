(ns ataru.harkinnanvaraisuus.harkinnanvaraisuus-job
  (:require [clj-time.core :as time]
            [ataru.harkinnanvaraisuus.harkinnanvaraisuus-process-store :as store]
            [clj-time.coerce :as coerce]
            [ataru.haku.haku-service :as haku-service]
            [ataru.applications.application-store :as application-store]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-util :as hutil]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol :as valintalaskentakoostepalvelu]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-types :as hartyp]
            [selmer.parser :as selmer]
            [ataru.translations.translation-util :as translations]
            [taoensso.timbre :as log]
            [ataru.background-job.job :as job]
            [ataru.harkinnanvaraisuus.harkinnanvaraisuus-email-job :as harkinnanvaraisuus-email-job]
            [ataru.db.db :as db]
            [ataru.config.core :refer [config]]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]))

(def MAXIMUM_PROCESSES_TO_HANDLE 1000)
(def DAYS_UNTIL_NEXT_RECHECK (get-in config [:harkinnanvaraisuus :recheck-delay-in-days] 1))

(def KOULUTUSTYYPIT_THAT_MUST_BE_CHECKED #{"koulutustyyppi_26" "koulutustyyppi_2"}) ; 26 = ammatillinen 2 = lukiokoulutus

(defn- assoc-only-harkinnanvarainen-to-application
  [application]
  (assoc application :harkinnanvarainen-only? (hutil/does-application-belong-to-only-harkinnanvarainen-valinta? application)))

(def sure-harkinnanvarainen-only-reasons
  (-> [(:sure-ei-paattotodistusta hartyp/harkinnanvaraisuus-reasons) (:sure-yks-mat-ai hartyp/harkinnanvaraisuus-reasons)]
      set))

(defn- extract-answer-value [answer-key-str application]
  (->> (:answers application)
       (filter (comp (partial = answer-key-str) :key))
       (map :value)
       (first)))

(defn- harkinnanvarainen-email [application template-name]
  (let [lang             (-> application
                             (get :lang "fi")
                             keyword)
        emails           [(extract-answer-value "email" application)
                          (extract-answer-value "guardian-email" application)
                          (extract-answer-value "guardian-email-secondary" application)]
        translations     (translations/get-translations lang)
        subject          (:email-vain-harkinnanvaraisessa-subject translations)
        body             (selmer/render-file template-name translations)]
    (when (not-empty emails)
      {:from       "no-reply@opintopolku.fi"
       :recipients emails
       :body       body
       :subject subject})))

(defn- start-email-job [job-runner connection application]
  (let [job-type (:type harkinnanvaraisuus-email-job/job-definition)
        template-name (if (:sure-harkinnanvarainen-only? application)
                        "templates/email_vain_harkinnanvaraisessa.html"
                        "templates/email_myos_pistevalinnassa.html")
        email (harkinnanvarainen-email application template-name)]
    (if email
      (let [job-id (job/start-job job-runner
                                  connection
                                  job-type
                                  email)]
        (log/info (str "Luotu emailin lähetys tehtävä " job-id
                       " kertomaan hakijalle valintatavan muutoksesta hakemukselle " (:key application))))
      (log/warn (str "Emailin lähetystehtävää hakemukselle " (:key application) " epäonnistui")))))

(defn- assoc-valintalaskentakoostepalvelu-harkinnainen-only
  [application-with-harkinnanvaraisuus applications-from-valintalaskentakoostepalvelu]
  (let [application-key (:key application-with-harkinnanvaraisuus)
        has-only-sure-harkinnanvarainen? (->> (get-in applications-from-valintalaskentakoostepalvelu [application-key :hakutoiveet])
                                              (map #(:harkinnanvaraisuudenSyy %))
                                              (some #(contains? sure-harkinnanvarainen-only-reasons %)))]
    (assoc application-with-harkinnanvaraisuus :sure-harkinnanvarainen-only? (boolean has-only-sure-harkinnanvarainen?))))

(defn- inform-about-harkinnanvarainen
  [job-runner connection app checked-time]
  (store/update-harkinnanvaraisuus-process (:id app) (:sure-harkinnanvarainen-only? app) checked-time)
  (start-email-job job-runner connection app))

(defn- handle-harkinnanvaraisuus-processes-to-save
  [job-runner applications-with-harkinnanvaraisuus checked-time]
  (let [connection {:datasource (db/get-datasource :db)}
        apps-without-email-job (filter #(= (:sure-harkinnanvarainen-only? %) (:harkinnanvarainen-only? %)) applications-with-harkinnanvaraisuus)
        email-job-apps (filter #(not (= (:sure-harkinnanvarainen-only? %) (:harkinnanvarainen-only? %))) applications-with-harkinnanvaraisuus)]
    (when (< 0 (count apps-without-email-job))
      (doall
        (for [app apps-without-email-job]
          (store/update-harkinnanvaraisuus-process (:id app) (:harkinnanvarainen-only? app) checked-time))))
    (when (< 0 (count email-job-apps))
      (doall
        (for [app email-job-apps]
          (inform-about-harkinnanvarainen job-runner connection app checked-time))))))

(defn- processids-where-check-can-be-skipped-due-to-haku
  [ohjausparametrit-service processes now]
  (let [valid-haku-oids (->> processes
                             (map #(:haku_oid %))
                             (distinct)
                             (filter #(not (haku-service/harkinnanvarainen-valinta-paattynyt? ohjausparametrit-service now %)))
                             (set))]
    (->> processes
         (filter #(not (contains? valid-haku-oids (:haku_oid %))))
         (map #(:application_id %))
         (set))))

(defn- mark-do-not-check-processes
  [process-ids]
  (when (< 0 (count process-ids))
    (log/debug (str "Merkataan " (count process-ids) " harkinnanvaraisuus-prosessia skippaamaan harkinnanvaraisuuden tarkistus"))
    (store/mark-do-not-check-harkinnanvaraisuus-processes process-ids)))

(defn- handle-processess-to-save
  [job-runner applications now]
  (when (< 0 (count applications))
    (log/debug (str "Asetetaan " (count applications) " hakemusta tarkistetuksi harkinnanvaraisuuden osalta"))
    (handle-harkinnanvaraisuus-processes-to-save job-runner applications now)))

(defn- can-skip-checking-application-due-to-hakukohteet
  [tarjonta-service application]
  (let [hakukohteet (tarjonta-protocol/get-hakukohteet tarjonta-service (:hakukohde application))]
    (->> hakukohteet
         (map #(:koulutustyyppikoodi %))
         (some #(contains? KOULUTUSTYYPIT_THAT_MUST_BE_CHECKED %))
         not)))

(defn check-harkinnanvaraisuus-step
  [_ {:keys [ohjausparametrit-service valintalaskentakoostepalvelu-service tarjonta-service] :as job-runner}]
  (log/debug "Check harkinnanvaraisuus step starting")
  (let [now       (time/now)
        processes (store/fetch-unprocessed-harkinnanvaraisuus-processes)
        processids-where-check-can-be-skipped (processids-where-check-can-be-skipped-due-to-haku ohjausparametrit-service processes now)
        processes-to-check (filter #(not (contains? processids-where-check-can-be-skipped (:application_id %))) processes)
        applications-to-check (->> processes-to-check
                                   (map #(application-store/get-application (:application_id %)))
                                   (filter #(not (hutil/can-skip-recheck-for-yks-ma-ai %)))
                                   (filter #(not (can-skip-checking-application-due-to-hakukohteet tarjonta-service %))))
        application-keys-to-check (map #(:key %) applications-to-check)
        application-ids-to-check (->> applications-to-check
                                      (map #(:id %))
                                      (set))
        processes-that-can-be-skipped (->> processes
                                           (filter #(not (contains? application-ids-to-check (:application_id %))))
                                           (map #(:application_id %)))
        applications-with-harkinnanvaraisuus (->> applications-to-check
                                                  (map #(assoc-only-harkinnanvarainen-to-application %)))
        harkinnanvaraisuudet-from-koostepalvelu (when (< 0 (count application-keys-to-check))
                                                  (valintalaskentakoostepalvelu/hakemusten-harkinnanvaraisuus-valintalaskennasta valintalaskentakoostepalvelu-service application-keys-to-check))
        applications-to-save (->> applications-with-harkinnanvaraisuus
                                  (map #(assoc-valintalaskentakoostepalvelu-harkinnainen-only % harkinnanvaraisuudet-from-koostepalvelu)))]
    (mark-do-not-check-processes processes-that-can-be-skipped)
    (handle-processess-to-save job-runner applications-to-save now)
    (log/debug "Check harkinnanvaraisuus step finishing")
    {:transition      {:id :to-next :step :initial}
     :updated-state   {:last-run-long (coerce/to-long now)}
     :next-activation (time/plus now (time/minutes 15))}))

(defn recheck-harkinnanvaraisuus-step
  [_ {:keys [ohjausparametrit-service valintalaskentakoostepalvelu-service] :as job-runner}]
  (log/info "Recheck harkinnanvaraisuus step starting")
  (let [now       (time/now)
        processes (store/fetch-checked-harkinnanvaraisuus-processes (-> now
                                                                        (time/minus (time/days DAYS_UNTIL_NEXT_RECHECK))
                                                                        (time/with-time-at-start-of-day)
                                                                        (time/plus (time/hours 23))))
        next-activation (if (< (count processes) MAXIMUM_PROCESSES_TO_HANDLE)
                          (time/with-time-at-start-of-day (time/plus now (time/days DAYS_UNTIL_NEXT_RECHECK)))
                          (time/plus now (time/minutes 1)))
        processids-where-check-can-be-skipped (processids-where-check-can-be-skipped-due-to-haku ohjausparametrit-service processes now)
        processes-to-check (filter #(not (contains? processids-where-check-can-be-skipped (:application_id %))) processes)
        applications-with-harkinnanvaraisuus (map
                                               #(assoc (application-store/get-application (:application_id %)) :harkinnanvarainen-only? (:harkinnanvarainen_only %))
                                               processes-to-check)
        application-keys-to-check (map #(:key %) applications-with-harkinnanvaraisuus)
        harkinnanvaraisuudet-from-koostepalvelu (when (< 0 (count application-keys-to-check))
                                                      (valintalaskentakoostepalvelu/hakemusten-harkinnanvaraisuus-valintalaskennasta-no-cache valintalaskentakoostepalvelu-service application-keys-to-check))
        applications-to-save (->> applications-with-harkinnanvaraisuus
                                  (map #(assoc-valintalaskentakoostepalvelu-harkinnainen-only % harkinnanvaraisuudet-from-koostepalvelu)))]
    (mark-do-not-check-processes (vec processids-where-check-can-be-skipped))
    (handle-processess-to-save job-runner applications-to-save now)
    (log/info (str "Recheck harkinnanvaraisuus step finishing, processed "
                   (count processes)
                   " applications in "
                   (- (coerce/to-long (time/now)) (coerce/to-long now))
                   " ms"))
    {:transition      {:id :to-next :step :initial}
     :updated-state   {:last-run-long (coerce/to-long now)}
     :next-activation next-activation}))

(def job-definition {:steps {:initial check-harkinnanvaraisuus-step}
                     :type  "harkinnanvaraisuus-check"})

(def recheck-job-definition {:steps {:initial recheck-harkinnanvaraisuus-step}
                             :type "harkinnanvaraisuus-recheck"})