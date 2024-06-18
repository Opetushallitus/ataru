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
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]
            [ataru.person-service.person-service :as person-service]
            [clojure.java.jdbc :as jdbc]
            [chime.core :as chime])
  (:import java.time.Instant))

(def MAXIMUM_PROCESSES_TO_HANDLE 1000)
(def DAYS_UNTIL_NEXT_RECHECK (get-in config [:harkinnanvaraisuus :recheck-delay-in-days] 1))

(def KOULUTUSTYYPIT_THAT_MUST_BE_CHECKED #{"koulutustyyppi_26" "koulutustyyppi_2"}) ; 26 = ammatillinen 2 = lukiokoulutus

(defn- assoc-only-harkinnanvarainen-to-application
  [application]
  (assoc application :harkinnanvarainen-only? (hutil/does-application-belong-to-only-harkinnanvarainen-valinta? application)))

(def valintalaskentakoostepalvelu-harkinnanvarainen-only-reasons
  (-> [(:sure-ei-paattotodistusta hartyp/harkinnanvaraisuus-reasons) (:sure-yks-mat-ai hartyp/harkinnanvaraisuus-reasons)
       (:ataru-ei-paattotodistusta hartyp/harkinnanvaraisuus-reasons) (:ataru-yks-mat-ai hartyp/harkinnanvaraisuus-reasons)
       (:ataru-ulkomailla-opiskelu hartyp/harkinnanvaraisuus-reasons)]
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
        guardian-emails  (distinct
                           (flatten
                             (filter some?
                                     [(extract-answer-value "guardian-email" application)
                                      (extract-answer-value "guardian-email-secondary" application)])))
        emails           (->> [(extract-answer-value "email" application)]
                              (concat guardian-emails)
                              (remove empty?))
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
                                              (some #(contains? valintalaskentakoostepalvelu-harkinnanvarainen-only-reasons %)))]
    (assoc application-with-harkinnanvaraisuus :sure-harkinnanvarainen-only? (boolean has-only-sure-harkinnanvarainen?))))

(defn- inform-about-harkinnanvarainen
  [job-runner connection app checked-time]
  (store/update-harkinnanvaraisuus-process (:id app) (:sure-harkinnanvarainen-only? app) checked-time)
  (jdbc/with-db-transaction [conn connection]
                            (start-email-job job-runner conn app)))

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

(defn- handle-yksiloimattomat-processes
  [applications checked-time]
  (when (< 0 (count applications))
    (log/debug (str "Asetetaan " (count applications) " yksilöimatonta hakemusta käsitellyksi harkinnanvaraisuuden osalta, tarkistus tehdään kun hakemukset on yksilöity"))
    (doall
      (for [app applications]
        (store/update-harkinnanvaraisuus-process (:id app) (:harkinnanvarainen-only? app) checked-time)))))

(defn- can-skip-checking-application-due-to-hakukohteet
  [tarjonta-service application]
  (let [hakukohteet (tarjonta-protocol/get-hakukohteet tarjonta-service (:hakukohde application))]
    (->> hakukohteet
         (map #(:koulutustyyppikoodi %))
         (some #(contains? KOULUTUSTYYPIT_THAT_MUST_BE_CHECKED %))
         not)))

(defn- yksiloimattomat-applications
  [person-service applications]
  (let [yksiloimattomat-persons (->> applications
                                    (map :person-oid)
                                    (remove nil?)
                                    distinct
                                    (person-service/get-persons person-service)
                                    vals
                                    (remove #(or (:yksiloity %)
                                                 (:yksiloityVTJ %)))
                                    (map :oidHenkilo)
                                    distinct
                                    set)]
    (filter #(or (nil? (:person-oid %))
                 (contains? yksiloimattomat-persons (:person-oid %))) applications)))

(defn- remove-yksiloimattomat-applications
  [applications-not-yksiloity application-keys]
  (remove (fn [application-key]
              (boolean (first (filter #(= application-key (:key %)) applications-not-yksiloity))))
          application-keys))

(defn check-harkinnanvaraisuus-handler
  [_ {:keys [ohjausparametrit-service valintalaskentakoostepalvelu-service tarjonta-service person-service] :as job-runner}]
  (log/info "Check harkinnanvaraisuus step starting")
  (let [now       (time/now)
        processes (store/fetch-unprocessed-harkinnanvaraisuus-processes)
        processids-where-check-can-be-skipped (processids-where-check-can-be-skipped-due-to-haku ohjausparametrit-service processes now)
        processes-to-check (filter #(not (contains? processids-where-check-can-be-skipped (:application_id %))) processes)
        applications-to-check (->> processes-to-check
                                   (map #(application-store/get-not-inactivated-application (:application_id %)))
                                   (filter some?)
                                   (filter #(not (hutil/can-skip-recheck-for-yks-ma-ai %)))
                                   (filter #(not (can-skip-checking-application-due-to-hakukohteet tarjonta-service %))))
        application-ids-to-check (->> applications-to-check
                                      (map #(:id %))
                                      (set))
        processes-that-can-be-skipped (->> processes
                                           (filter #(not (contains? application-ids-to-check (:application_id %))))
                                           (map #(:application_id %)))
        applications-with-harkinnanvaraisuus (->> applications-to-check
                                                  (map #(assoc-only-harkinnanvarainen-to-application %)))
        applications-not-yksiloity (yksiloimattomat-applications person-service applications-with-harkinnanvaraisuus)
        application-keys-to-check (->> applications-to-check
                                       (map :key)
                                       (remove-yksiloimattomat-applications applications-not-yksiloity))
        application-keys-to-check-set (set application-keys-to-check)
        harkinnanvaraisuudet-from-koostepalvelu (when (< 0 (count application-keys-to-check))
                                                  (valintalaskentakoostepalvelu/hakemusten-harkinnanvaraisuus-valintalaskennasta-no-cache
                                                    valintalaskentakoostepalvelu-service
                                                    application-keys-to-check))
        applications-to-save (->> applications-with-harkinnanvaraisuus
                                  (remove #(not (contains? application-keys-to-check-set (:key %))))
                                  (map #(assoc-valintalaskentakoostepalvelu-harkinnainen-only % harkinnanvaraisuudet-from-koostepalvelu)))]
    (mark-do-not-check-processes processes-that-can-be-skipped)
    (handle-processess-to-save job-runner applications-to-save now)
    (handle-yksiloimattomat-processes applications-not-yksiloity now)
    (log/info "Check harkinnanvaraisuus step finishing")))

(defn recheck-harkinnanvaraisuus-handler
  [_ {:keys [ohjausparametrit-service valintalaskentakoostepalvelu-service person-service] :as job-runner}]
  (log/info "Recheck harkinnanvaraisuus step starting")
  (let [now       (time/now)
        processes (store/fetch-checked-harkinnanvaraisuus-processes (-> now
                                                                        (time/minus (time/days DAYS_UNTIL_NEXT_RECHECK))))
        processed-all (<= (count processes) MAXIMUM_PROCESSES_TO_HANDLE)
        processids-where-check-can-be-skipped (processids-where-check-can-be-skipped-due-to-haku ohjausparametrit-service processes now)
        processes-to-check (filter #(not (contains? processids-where-check-can-be-skipped (:application_id %))) processes)
        applications-to-check (->> processes-to-check
                                   (map #(application-store/get-not-inactivated-application (:application_id %)))
                                   (filter some?))
        application-ids-to-check (->> applications-to-check
                                      (map #(:id %))
                                      (set))
        processes-that-can-be-skipped (->> processes
                                           (filter #(not (contains? application-ids-to-check (:application_id %))))
                                           (map #(:application_id %)))
        assoc-harkinnanvarainen-fn (fn [application]
                                     (let [matching-process (->> processes-to-check
                                                                 (filter #(= (:application_id %) (:id application)))
                                                                 first)]
                                       (assoc application :harkinnanvarainen-only? (:harkinnanvarainen_only matching-process))))
        applications-with-harkinnanvaraisuus (map assoc-harkinnanvarainen-fn applications-to-check)
        applications-not-yksiloity (yksiloimattomat-applications person-service applications-with-harkinnanvaraisuus)
        application-keys-to-check (->> applications-with-harkinnanvaraisuus
                                       (map :key)
                                       (remove-yksiloimattomat-applications applications-not-yksiloity))
        application-keys-to-check-set (set application-keys-to-check)
        harkinnanvaraisuudet-from-koostepalvelu (when (< 0 (count application-keys-to-check))
                                                      (valintalaskentakoostepalvelu/hakemusten-harkinnanvaraisuus-valintalaskennasta-no-cache
                                                        valintalaskentakoostepalvelu-service
                                                        application-keys-to-check))
        applications-to-save (->> applications-with-harkinnanvaraisuus
                                  (remove #(not (contains? application-keys-to-check-set (:key %))))
                                  (map #(assoc-valintalaskentakoostepalvelu-harkinnainen-only % harkinnanvaraisuudet-from-koostepalvelu)))]
    (mark-do-not-check-processes processes-that-can-be-skipped)
    (handle-processess-to-save job-runner applications-to-save now)
    (handle-yksiloimattomat-processes applications-not-yksiloity now)
    (log/info (str "Recheck harkinnanvaraisuus step finishing, processed "
                   (count processes)
                   " applications in "
                   (- (coerce/to-long (time/now)) (coerce/to-long now))
                   " ms"))
    (when-not processed-all
      (chime/chime-at [(.plusSeconds (Instant/now) 60)]
                      (fn [_]
                        (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                                                  (job/start-job job-runner connection "harkinnanvaraisuus-recheck" nil)))))))

(def job-definition {:handler check-harkinnanvaraisuus-handler
                     :type    "harkinnanvaraisuus-check"
                     :schedule "*/15 * * * *"})

(def recheck-job-definition {:handler recheck-harkinnanvaraisuus-handler
                             :type    "harkinnanvaraisuus-recheck"
                             :schedule "10 8 * * *"})