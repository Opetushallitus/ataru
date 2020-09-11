(ns ataru.tilastokeskus.tilastokeskus-service
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.answer-util :as answer-util]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]
            [ataru.util :as util]
            [taoensso.timbre :as log]))

(defn- hakutoiveet
  [hakukohteet]
  (mapv
   (fn [i h]
     {:hakukohde_oid h
      :sija          i})
   (range 1 7)
   hakukohteet))

(defn- first-string [v]
  (cond
    (string? v) v
    (not (vector? v)) nil
    :else (first-string (first v))))

(defn- enrich-application-data
  [haku application]
  (let [answers (-> application :content :answers util/answers-by-key)]
    (merge application
           {:pohjakoulutus_kk             (answer-util/get-kk-pohjakoulutus haku answers (:hakemus_oid application))
                                          ; This is a vector of vectors where index determines the country for each specific foreign base education
                                          ; This isn't the pretties way to implement this, but it is the easiest for now.
            :pohjakoulutus_kk_ulk_country (some-> (get-in answers [:pohjakoulutus_kk_ulk--country :value]) first-string)
            :hakutoiveet                  (hakutoiveet (:hakukohde_oids application))})))

(defn get-application-info-for-tilastokeskus
  [tarjonta-service haku-oid hakukohde-oid]
  (let [applications (application-store/get-application-info-for-tilastokeskus haku-oid hakukohde-oid)
        haut         (->> (keep :haku_oid applications)
                          distinct
                          (map (fn [oid] [oid (tarjonta-protocol/get-haku tarjonta-service oid)]))
                          (into {}))
        results      (map (fn [application]
                            (try
                              (let [enriched-application (enrich-application-data (get haut (:haku_oid application)) application)]
                                [nil (dissoc enriched-application :content)])  ; remove keys we don't want to expose through API
                              (catch Exception e
                                [[e (:key application)] nil])))
                          applications)]
    (doseq [[e application-key] (keep first results)]
      (log/error e (str "Failed to parse Tilastokeskus data of application " application-key)))
    (if (some (comp some? first) results)
      (throw (new RuntimeException "Failed to parse Tilastokeskus data"))
      (map second results))))