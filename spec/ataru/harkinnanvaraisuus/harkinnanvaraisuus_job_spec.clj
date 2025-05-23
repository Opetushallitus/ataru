(ns ataru.harkinnanvaraisuus.harkinnanvaraisuus-job-spec
  (:require [speclj.core :refer [it describe tags should= after]]
            [ataru.harkinnanvaraisuus.harkinnanvaraisuus-process-store :as store]
            [clojure.java.jdbc :as jdbc]
            [ataru.log.audit-log :as audit-log]
            [ataru.db.db :as db]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.harkinnanvaraisuus.harkinnanvaraisuus-job :refer [check-harkinnanvaraisuus-handler recheck-harkinnanvaraisuus-handler]]
            [ataru.applications.application-store :as application-store]
            [ataru.forms.form-store :as form-store]
            [clj-time.core :as time]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :refer [OhjausparametritService]]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol :refer [ValintalaskentakoostepalveluService]]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.cache.cache-service :as cache-service]
            [com.stuartsierra.component :as component]
            [ataru.background-job.job :as job]
            [ataru.person-service.person-service :as person-service]))

(def ^:private test-form-id (atom nil))
(def ^:private test-application-id (atom nil))

(def haku-key "payment-info-test-non-kk-haku")

(def audit-logger (audit-log/new-dummy-audit-logger))

(defn- get-stored-process []
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (->> (jdbc/query conn ["select * from harkinnanvaraisuus_process"])
                                 first)))

(defrecord MockOhjausparametritServiceWithPast []
  OhjausparametritService

  (get-parametri [_ _]
      {:PH_HVVPTP {:date 1506920400000}}))

(defrecord MockOhjausparametritServiceWithFuture []
  OhjausparametritService

  (get-parametri [_ _]
    {:PH_HVVPTP {:date 9506920400000}}))

(def ts (tarjonta-service/->MockTarjontaKoutaService))

(defn- to-hakemus-with-harkinnanvaraisuus [hakemus-oid syy]
  {hakemus-oid {:hakemusOid hakemus-oid :henkiloOid nil :hakutoiveet [{
                                                :hakukohdeOid "1.2.246.562.20.00000000000000024371"
                                                :harkinnanvaraisuudenSyy syy}]}})

(defrecord MockValintalaskentakoostepalveluService []
  ValintalaskentakoostepalveluService

  (hakukohde-uses-valintalaskenta? [_ _] true)
  (opiskelijan-suoritukset [_ _ _] [])
  (opiskelijoiden-suoritukset [_ _ _] [])
  (hakemusten-harkinnanvaraisuus-valintalaskennasta [_ hakemus-oids]
         (to-hakemus-with-harkinnanvaraisuus (first hakemus-oids) "EI_HARKINNANVARAINEN"))
  (hakemusten-harkinnanvaraisuus-valintalaskennasta-no-cache [_ hakemus-oids]
    (to-hakemus-with-harkinnanvaraisuus (first hakemus-oids) "EI_HARKINNANVARAINEN")))

(defrecord MockHarkinnanvarainenValintalaskentakoostepalveluService []
  ValintalaskentakoostepalveluService

  (hakukohde-uses-valintalaskenta? [_ _] true)
  (opiskelijan-suoritukset [_ _ _] [])
  (opiskelijoiden-suoritukset [_ _ _] [])
  (hakemusten-harkinnanvaraisuus-valintalaskennasta [_ hakemus-oids]
    (to-hakemus-with-harkinnanvaraisuus (first hakemus-oids) "SURE_YKS_MAT_AI"))
  (hakemusten-harkinnanvaraisuus-valintalaskennasta-no-cache [_ hakemus-oids]
    (to-hakemus-with-harkinnanvaraisuus (first hakemus-oids) "SURE_YKS_MAT_AI")))

(def vlkp (->MockValintalaskentakoostepalveluService))
(def harvlkp (->MockHarkinnanvarainenValintalaskentakoostepalveluService))

(defn- create-application
  ([] (create-application [{:key "base-education-2nd"
                           :label {:fi "Valitse yksi pohjakoulutus, jolla haet koulutukseen" :sv ""}
                           :value "1"
                           :fieldType "singleChoice"}]))
  ([answers]
  (as-> application-fixtures/person-info-form-application ap
        (assoc ap :haku haku-key)
        (assoc ap :person-oid "2.2.2")
        (assoc ap :hakukohde ["1.2.246.562.20.00000000000000024371"])
        (assoc ap :answers (vec (concat (:answers ap) answers))))))

(defrecord FakeJobRunner []
  component/Lifecycle

  job/JobRunner
  (start-job [_ _ _ _]))

(def fps (person-service/->FakePersonService))

(def os (organization-service/->FakeOrganizationService))

(def fake-koodisto-cache (reify cache-service/Cache
                                (get-from [_ _])
                                (get-many-from [_ _])
                                (remove-from [_ _])
                                (clear-all [_])))

(defn runner-with-deps [vlkp-service]
  (map->FakeJobRunner {:ohjausparametrit-service (->MockOhjausparametritServiceWithFuture) :organization-service os :koodisto-cache fake-koodisto-cache :tarjonta-service ts :valintalaskentakoostepalvelu-service vlkp-service :person-service fps}))

(defn- clean! [id]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :application_hakukohde_attachment_reviews
                                          ["application_key = (select key from applications where id = ?)" id])
                            (jdbc/delete! conn :applications
                                          ["id = ?" id])
                            (jdbc/delete! conn :harkinnanvaraisuus_process
                                          ["application_id = ?" id])
                            (jdbc/delete! conn :forms
                                          ["id = ?" id])))

(defn- init [application]
  (let [form (form-store/create-new-form! form-fixtures/person-info-form)
        stored (application-store/add-application (assoc application :form (:id form)) [] form {} audit-logger nil)]
    (reset! test-form-id (:id form))
    (reset! test-application-id (:id stored))
    (store/upsert-harkinnanvaraisuus-process (:id stored) (:key stored) haku-key)))

(defn- init-with-check [application runner]
  (init application)
  (check-harkinnanvaraisuus-handler {} runner)
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (->> (jdbc/update! conn :harkinnanvaraisuus_process {:last_checked (time/minus (time/now) (time/days 5))} ["last_checked is not null"]))))

(describe "harkinnanvaraisuus-job"
          (tags :unit :database)

          (after
            (clean! @test-application-id)
            (clean! @test-form-id))

          (describe "check-harkinnanvaraisuus-step"
                    (it "skips check if haku is not open"
                        (init (create-application))
                        (let [ops (->MockOhjausparametritServiceWithPast)]
                          (check-harkinnanvaraisuus-handler {} {:ohjausparametrit-service ops :tarjonta-service ts :valintalaskentakoostepalvelu-service vlkp :person-service fps})
                          (should= true (:skip_check (get-stored-process)))))

                    (it "skips check if pohjakoulutus is ulkomailla suoritettu"
                        (init (create-application [{:key "base-education-2nd"
                                                   :label {:fi "Valitse yksi pohjakoulutus, jolla haet koulutukseen" :sv ""}
                                                   :value "0"
                                                   :fieldType "singleChoice"}]))
                        (let [ops (->MockOhjausparametritServiceWithFuture)]
                          (check-harkinnanvaraisuus-handler {} {:ohjausparametrit-service ops :tarjonta-service ts :valintalaskentakoostepalvelu-service vlkp :person-service fps})
                          (should= true (:skip_check (get-stored-process)))))

                    (it "skips check if pohjakoulutus is järjestetty toiminta-alueittain"
                        (init (create-application [{:key "base-education-2nd"
                                                   :label {:fi "Valitse yksi pohjakoulutus, jolla haet koulutukseen" :sv ""}
                                                   :value "3"
                                                   :fieldType "singleChoice"}]))
                        (let [ops (->MockOhjausparametritServiceWithFuture)]
                          (check-harkinnanvaraisuus-handler {} {:ohjausparametrit-service ops :organization-service os :koodisto-cache fake-koodisto-cache :tarjonta-service ts :valintalaskentakoostepalvelu-service vlkp :person-service fps})
                          (should= true (:skip_check (get-stored-process)))))

                    (it "skips check if no lukio or ammatillinen hakukohteita"
                        (init (assoc (create-application) :hakukohde ["1.2.246.562.20.00000000000000024372"]))
                        (let [ops (->MockOhjausparametritServiceWithFuture)]
                          (check-harkinnanvaraisuus-handler {} {:ohjausparametrit-service ops :organization-service os :koodisto-cache fake-koodisto-cache :tarjonta-service ts :valintalaskentakoostepalvelu-service vlkp :person-service fps})
                          (should= true (:skip_check (get-stored-process)))))

                    (it "checks application"
                        (init (create-application))
                        (let [ops (->MockOhjausparametritServiceWithFuture)
                              _   (check-harkinnanvaraisuus-handler {} {:ohjausparametrit-service ops :organization-service os :koodisto-cache fake-koodisto-cache :tarjonta-service ts :valintalaskentakoostepalvelu-service vlkp :person-service fps})
                              process (get-stored-process)]
                          (should= false (:skip_check process))
                          (should= false (:harkinnanvarainen_only process))
                          (should= false (nil? (:last_checked process)))))

                    (it "checks harkinnanvarainen application"
                        (init (create-application [{:key "base-education-2nd"
                                                    :label {:fi "Valitse yksi pohjakoulutus, jolla haet koulutukseen" :sv ""}
                                                    :value "7"
                                                    :fieldType "singleChoice"}]))
                        (let [ops (->MockOhjausparametritServiceWithFuture)
                              _   (check-harkinnanvaraisuus-handler {} {:ohjausparametrit-service ops :organization-service os :koodisto-cache fake-koodisto-cache :tarjonta-service ts :valintalaskentakoostepalvelu-service harvlkp :person-service fps})
                              process (get-stored-process)]
                          (should= false (:skip_check process))
                          (should= true (:harkinnanvarainen_only process))
                          (should= false (nil? (:last_checked process)))))

                    (it "checks application and sets harkinnanvarainen due to result from valintalaskentakoostepalvelu-servie"
                        (init (create-application))
                        (check-harkinnanvaraisuus-handler {} (runner-with-deps harvlkp))
                        (let [process (get-stored-process)]
                          (should= false (:skip_check process))
                          (should= true (:harkinnanvarainen_only process))
                          (should= false (nil? (:last_checked process)))))

                    (it "checks yksiloimaton application and does not use valintalaskentakoostepalvelu-servie"
                        (init (assoc (create-application) :person-oid "1.2.3.4.5.6"))
                        (check-harkinnanvaraisuus-handler {} (runner-with-deps harvlkp))
                        (let [process (get-stored-process)]
                          (should= false (:skip_check process))
                          (should= false (:harkinnanvarainen_only process))
                          (should= false (nil? (:last_checked process)))))

                    (it "checks harkinnanvarainen application and sets harkinnanvarainen false due to result from valintalaskentakoostepalvelu-servie"
                        (init (create-application [{:key "base-education-2nd"
                                                    :label {:fi "Valitse yksi pohjakoulutus, jolla haet koulutukseen" :sv ""}
                                                    :value "7"
                                                    :fieldType "singleChoice"}]))
                        (check-harkinnanvaraisuus-handler {} (runner-with-deps vlkp))
                        (let [process (get-stored-process)]
                          (should= false (:skip_check process))
                          (should= false (:harkinnanvarainen_only process))
                          (should= false (nil? (:last_checked process)))))

                    (it "checks yksiloimaton harkinnanvarainen application and does not use valintalaskentakoostepalvelu-servie"
                        (init (assoc (create-application [{:key "base-education-2nd"
                                                    :label {:fi "Valitse yksi pohjakoulutus, jolla haet koulutukseen" :sv ""}
                                                    :value "7"
                                                    :fieldType "singleChoice"}])
                                      :person-oid "1.2.3.4.5.6"))
                        (check-harkinnanvaraisuus-handler {} (runner-with-deps vlkp))
                        (let [process (get-stored-process)]
                          (should= false (:skip_check process))
                          (should= true (:harkinnanvarainen_only process))
                          (should= false (nil? (:last_checked process))))))

          (describe "recheck-harkinnanvaraisuus-step"
                    (it "skips recheck if haku is not open"
                        (init-with-check (create-application) (runner-with-deps vlkp))
                        (let [ops (->MockOhjausparametritServiceWithPast)]
                          (recheck-harkinnanvaraisuus-handler {} {:ohjausparametrit-service ops :organization-service os :koodisto-cache fake-koodisto-cache :valintalaskentakoostepalvelu-service vlkp :person-service fps})
                          (should= true (:skip_check (get-stored-process)))))

                    (it "rechecks application and sets harkinnanvarainen due to result from valintalaskentakoostepalvelu-servie"
                        (init-with-check (create-application) (runner-with-deps harvlkp))
                        (recheck-harkinnanvaraisuus-handler {} (runner-with-deps harvlkp))
                        (let [process (get-stored-process)]
                          (should= false (:skip_check process))
                          (should= true (:harkinnanvarainen_only process))
                          (should= false (nil? (:last_checked process)))))

                    (it "rechecks yksiloimaton application and does not use valintalaskentakoostepalvelu-servie"
                        (init-with-check (assoc (create-application) :person-oid "1.2.3.4.5.6") (runner-with-deps harvlkp))
                        (recheck-harkinnanvaraisuus-handler {} (runner-with-deps harvlkp))
                        (let [process (get-stored-process)]
                          (should= false (:skip_check process))
                          (should= false (:harkinnanvarainen_only process))
                          (should= false (nil? (:last_checked process)))))

                    (it "rechecks harkinnanvarainen application and sets harkinnanvarainen false due to result from valintalaskentakoostepalvelu-servie"
                        (init-with-check (create-application [{:key "base-education-2nd"
                                                    :label {:fi "Valitse yksi pohjakoulutus, jolla haet koulutukseen" :sv ""}
                                                    :value "7"
                                                    :fieldType "singleChoice"}]) (runner-with-deps harvlkp))
                        (recheck-harkinnanvaraisuus-handler {} (runner-with-deps vlkp))
                        (let [process (get-stored-process)]
                          (should= false (:skip_check process))
                          (should= false (:harkinnanvarainen_only process))
                          (should= false (nil? (:last_checked process)))))

                    (it "rechecks yksiloimaton harkinnanvarainen application and does not use valintalaskentakoostepalvelu-servie"
                        (init-with-check
                          (assoc (create-application [{:key       "base-education-2nd"
                                                       :label     {:fi "Valitse yksi pohjakoulutus, jolla haet koulutukseen" :sv ""}
                                                       :value     "7"
                                                       :fieldType "singleChoice"}])
                                  :person-oid
                                  "1.2.3.4.5.6")
                          (runner-with-deps harvlkp))
                        (recheck-harkinnanvaraisuus-handler {} (runner-with-deps vlkp))
                        (let [process (get-stored-process)]
                          (should= false (:skip_check process))
                          (should= true (:harkinnanvarainen_only process))
                          (should= false (nil? (:last_checked process)))))))
