(ns ataru.tilastokeskus.tilastokeskus-service
  (:require [clj-time.core :as t]
            [ataru.applications.application-store :as application-store]
            [ataru.applications.answer-util :as answer-util]
            [ataru.applications.suoritus-filter :as suoritus-filter]
            [ataru.person-service.person-service :as person-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]
            [ataru.odw.odw-service :as odw-service]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [ataru.tarjonta.haku :as h]
            [ataru.util :as util]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol :as valintalaskentakoostepalvelu]
            [clojure.set]
            [taoensso.timbre :as log]))

(defn- hakutoiveet
  [hakukohteet harkinnanvaraisuudet]
  (let [harkinnanvaraisuudet-map (into {} (map (fn [{:keys [hakukohdeOid harkinnanvaraisuudenSyy]}]
                                                 [hakukohdeOid harkinnanvaraisuudenSyy])
                                               harkinnanvaraisuudet))]
    (mapv
    (fn [i h]
      {:hakukohde_oid h
       :sija          i
       :harkinnanvaraisuuden_syy (get harkinnanvaraisuudet-map h)})
    (range 1 7)
    hakukohteet)))

(defn- first-string [v]
  (cond
    (string? v) v
    (not (vector? v)) nil
    :else (first-string (first v))))

(defn- filter-applications-for-koostedata [toinen-aste-applications persons]
  (let [yksiloimaton-tai-aidinkieleton-henkilo-oids (odw-service/get-yksiloimaton-tai-aidinkieleton-henkilo-oids persons)
        persons-from-applications (set (map :henkilo_oid toinen-aste-applications))
        dropped-oids (clojure.set/intersection yksiloimaton-tai-aidinkieleton-henkilo-oids persons-from-applications)
        wanted-applications (filter #(not (contains? yksiloimaton-tai-aidinkieleton-henkilo-oids (:person-oid %))) toinen-aste-applications)]
    (when (not-empty dropped-oids)
      (log/info (str "Tilastokeskus: Ei haeta koosteDataa yksilöimättömille tai äidinkielettömille hakijoille: " dropped-oids)))
    wanted-applications))

(defn- get-koostedata-for-applications
  [toisen-asteen-yhteishaku-oids applications-for-koostedata valintalaskentakoostepalvelu-service]
  (into {} (map (fn [hakuOid] (let [hakemus-oids (vec (doall (map :hakemus_oid (filter (fn [application] (= hakuOid (:haku_oid application)))
                                                                               applications-for-koostedata))))]
                                (if (not-empty hakemus-oids)
                                  (do (log/info "Tilastokeskus: Haetaan koosteData haulle" hakuOid ",   hakemusOids " hakemus-oids)
                                    (valintalaskentakoostepalvelu/opiskelijoiden-suoritukset valintalaskentakoostepalvelu-service hakuOid hakemus-oids))
                                  (do (log/warn "Tilastokeskus: Ei haeta koosteDataa haulle" hakuOid "koska on vain passiivisia tai yksilöimättömiä hakemuksia")
                                    {}))))
                toisen-asteen-yhteishaku-oids)))

(defn- enrich-application-data
  [haku application harkinnanvaraisuudet koostedata suoritus-service]
  (let [answers (-> application :content :answers util/answers-by-key)
        toinen-aste? (h/toisen-asteen-yhteishaku? haku)
        person-oid (:henkilo_oid application)
        hakuvuosi (-> application :submitted t/year)]
    (merge application
           (when toinen-aste?
             (let [pohjakoulutus (:POHJAKOULUTUS koostedata)
                   opetuskieli (:perusopetuksen_kieli koostedata)
                   luokkatieto (suoritus-service/opiskelijan-luokkatieto suoritus-service person-oid (vector hakuvuosi) (suoritus-filter/luokkatasot-for-suoritus-filter) nil)
                   lahtokoulu-oid (:oppilaitos-oid luokkatieto)]
               {:pohjakoulutus_2aste               pohjakoulutus
                :pohjakoulutus_2aste_suorituskieli   opetuskieli
                :pohjakoulutus_2aste_lahtokoulu_oid  lahtokoulu-oid}))
           {:pohjakoulutus_kk             (answer-util/get-kk-pohjakoulutus haku answers (:hakemus_oid application))
                                          ; This is a vector of vectors where index determines the country for each specific foreign base education
                                          ; This isn't the pretties way to implement this, but it is the easiest for now.
            :pohjakoulutus_kk_ulk_country (some-> (or (get-in answers [:pohjakoulutus_kk_ulk--country :value])
                                                      (get-in answers [:secondary-completed-base-education–country :value])
                                                      (get-in answers [:893ede6f-998e-4e66-9ca5-b10bc602c944 :value]))
                                                  first-string)
            :hakutoiveet                  (hakutoiveet (:hakukohde_oids application) harkinnanvaraisuudet)}
            )))

(defn get-application-info-for-tilastokeskus
  [person-service tarjonta-service valintalaskentakoostepalvelu-service suoritus-service haku-oid hakukohde-oid]
  (let [applications (application-store/get-application-info-for-tilastokeskus haku-oid hakukohde-oid)
        haut         (->> (keep :haku_oid applications)
                          distinct
                          (map (fn [oid] [oid (tarjonta-protocol/get-haku tarjonta-service oid)]))
                          (into {}))
        persons (person-service/get-persons person-service (distinct (keep :henkilo_oid applications)))
        toisen-asteen-yhteishaut (into {} (filter #(->> % val h/toisen-asteen-yhteishaku?) haut))
        toisen-asteen-yhteishaku-oids (set (map key toisen-asteen-yhteishaut))
        toisen-asteen-yhteishakujen-hakemukset (filter (fn [application] (contains? toisen-asteen-yhteishaku-oids (:haku_oid application))) applications)
        toisen-asteen-yhteishakujen-hakemusten-oidit (map :hakemus_oid toisen-asteen-yhteishakujen-hakemukset)
        harkinnanvaraisuus-by-hakemus (if (not-empty toisen-asteen-yhteishakujen-hakemusten-oidit)
                                        (valintalaskentakoostepalvelu/hakemusten-harkinnanvaraisuus-valintalaskennasta valintalaskentakoostepalvelu-service toisen-asteen-yhteishakujen-hakemusten-oidit)
                                        {})
        applications-for-koostedata (filter-applications-for-koostedata toisen-asteen-yhteishakujen-hakemukset persons)
        kooste-data-toinen-aste (get-koostedata-for-applications toisen-asteen-yhteishaku-oids applications-for-koostedata valintalaskentakoostepalvelu-service)
        results      (map (fn [application]
                            (try
                              (let [hakutoiveiden-harkinnanvaraisuudet (get-in harkinnanvaraisuus-by-hakemus [(:hakemus_oid application) :hakutoiveet] [])
                                    haku (get haut (:haku_oid application))
                                    application-koostedata (get kooste-data-toinen-aste (keyword (:henkilo_oid application)))
                                     enriched-application (enrich-application-data haku application hakutoiveiden-harkinnanvaraisuudet application-koostedata suoritus-service)]
                                [nil (dissoc enriched-application :content)])  ; remove keys we don't want to expose through API
                              (catch Exception e
                                [[e (:key application)] nil])))
                          applications)]
    (doseq [[e application-key] (keep first results)]
      (log/error e (str "Failed to parse Tilastokeskus data of application " application-key)))
    (if (some (comp some? first) results)
      (throw (new RuntimeException "Failed to parse Tilastokeskus data"))
      (map second results))))