(ns ataru.tilastokeskus.tilastokeskus-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.answer-util :as answer-util]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]
            [ataru.tarjonta.haku :as h]
            [ataru.util :as util]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol :as valintalaskentakoostepalvelu]
            [taoensso.timbre :as log]))

(defn- hakutoiveet
  [hakukohteet harkinnanvaraisuudet]
  (mapv
   (fn [i h]
     {:hakukohde_oid h
      :sija          i
      :harkinnanvaraisuuden_syy (get harkinnanvaraisuudet h)})
   (range 1 7)
   hakukohteet))

(defn- first-string [v]
  (cond
    (string? v) v
    (not (vector? v)) nil
    :else (first-string (first v))))

(defn- enrich-application-data
  [haku application harkinnanvaraisuudet]
  (let [answers (-> application :content :answers util/answers-by-key)]
    (merge application
           {:pohjakoulutus_kk             (answer-util/get-kk-pohjakoulutus haku answers (:hakemus_oid application))
                                          ; This is a vector of vectors where index determines the country for each specific foreign base education
                                          ; This isn't the pretties way to implement this, but it is the easiest for now.
            :pohjakoulutus_kk_ulk_country (some-> (or (get-in answers [:pohjakoulutus_kk_ulk--country :value])
                                                      (get-in answers [:secondary-completed-base-education–country :value])
                                                      (get-in answers [:893ede6f-998e-4e66-9ca5-b10bc602c944 :value]))
                                                  first-string)
            :hakutoiveet                  (hakutoiveet (:hakukohde_oids application) harkinnanvaraisuudet)})))

(defn get-application-info-for-tilastokeskus
  [tarjonta-service valintalaskentakoostepalvelu-service haku-oid hakukohde-oid]
  (let [applications (application-store/get-application-info-for-tilastokeskus haku-oid hakukohde-oid)
        haut         (->> (keep :haku_oid applications)
                          distinct
                          (map (fn [oid] [oid (tarjonta-protocol/get-haku tarjonta-service oid)]))
                          (into {}))
        toisen-asteen-yhteishaut (into {} (filter #(->> % val h/toisen-asteen-yhteishaku?) haut))
        toisen-asteen-yhteishaku-oids (set (map key toisen-asteen-yhteishaut))
        toisen-asteen-yhteishakujen-hakemukset (filter (fn [application] (contains? toisen-asteen-yhteishaku-oids (:haku_oid application))) applications)
        toisen-asteen-yhteishakujen-hakemusten-oidit (map :hakemus_oid toisen-asteen-yhteishakujen-hakemukset)
        harkinnanvaraisuus-by-hakemus (if (not-empty toisen-asteen-yhteishakujen-hakemusten-oidit)
                                        (valintalaskentakoostepalvelu/hakemusten-harkinnanvaraisuus-valintalaskennasta-no-cache valintalaskentakoostepalvelu-service toisen-asteen-yhteishakujen-hakemusten-oidit)
                                        {})
        results      (map (fn [application]
                            (try
                              (let [hakutoiveiden-harkinnanvaraisuudet (get-in harkinnanvaraisuus-by-hakemus [(:key application) :hakutoiveet] [])
                                     enriched-application (enrich-application-data (get haut (:haku_oid application)) application hakutoiveiden-harkinnanvaraisuudet)]
                                [nil (dissoc enriched-application :content)])  ; remove keys we don't want to expose through API
                              (catch Exception e
                                [[e (:key application)] nil])))
                          applications)]
    (doseq [[e application-key] (keep first results)]
      (log/error e (str "Failed to parse Tilastokeskus data of application " application-key)))
    (if (some (comp some? first) results)
      (throw (new RuntimeException "Failed to parse Tilastokeskus data"))
      (map second results))))