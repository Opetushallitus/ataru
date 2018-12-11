(ns ataru.tarjonta-service.tarjonta-parser
  (:require [taoensso.timbre :as log]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [ataru.tarjonta-service.tarjonta-service :refer [yhteishaku?]]
            [ataru.koodisto.koodisto :refer [get-koodisto-options]]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit-protocol]))

(def ^:private lang-key-renames {:kieli_fi :fi :kieli_en :en :kieli_sv :sv})

(defn- localized-names
  ([names]
   (localized-names names identity))
  ([names key-fn]
  (into {}
        (for [lang [:fi :sv :en]
              :when (contains? names lang)]
          [lang (-> names lang key-fn)]))))

(defn- parse-koulutuskoodi
  [koulutus]
  (-> koulutus
      :koulutuskoodi
      :meta
      (clojure.set/rename-keys lang-key-renames)
      (localized-names :nimi)))

(defn- parse-tutkintonimikes
  [koulutus]
  (mapv (fn [[_ name]]
          (-> name
              :meta
              (clojure.set/rename-keys lang-key-renames)
              (localized-names :nimi)))
        (-> koulutus :tutkintonimikes :meta)))

(defn- parse-koulutusohjelma
  [koulutus]
  (-> koulutus
      :koulutusohjelma
      :tekstis
      (clojure.set/rename-keys lang-key-renames)
      localized-names))

(defn- parse-koulutus
  [response]
  {:oid                  (:oid response)
   :koulutuskoodi-name   (parse-koulutuskoodi response)
   :tutkintonimike-names (parse-tutkintonimikes response)
   :tarkenne             (:tarkenne response)
   :koulutusohjelma-name (parse-koulutusohjelma response)})

(defn- applicable-base-educations
  [hakukohde pohjakoulutusvaatimuskorkeakoulut]
  (mapcat (fn [uri]
            (->> pohjakoulutusvaatimuskorkeakoulut
                 (filter #(= uri (:uri %)))
                 (mapcat :within)
                 (filter #(clojure.string/starts-with? (:uri %) "pohjakoulutuskklomake_"))
                 (map :value)))
          (:hakukelpoisuusvaatimusUris hakukohde)))

(defn- parse-hakukohde
  [tarjonta-service
   hakukohderyhmat
   haku
   ohjausparametrit
   pohjakoulutusvaatimuskorkeakoulut
   hakukohde]
  (when (:oid hakukohde)
    {:oid                        (:oid hakukohde)
     :name                       (->> (clojure.set/rename-keys (:hakukohteenNimet hakukohde)
                                                               lang-key-renames)
                                      (remove (comp clojure.string/blank? second))
                                      (into {}))
     :hakukohderyhmat            (->> (:ryhmaliitokset hakukohde)
                                      (map :ryhmaOid)
                                      (filter #(contains? hakukohderyhmat %)))
     :kohdejoukko-korkeakoulu?   (clojure.string/starts-with?
                                  (:kohdejoukkoUri haku)
                                  "haunkohdejoukko_12#")
     :tarjoaja-name              (:tarjoajaNimet hakukohde)
     :form-key                   (:ataruLomakeAvain hakukohde)
     :koulutukset                (->> (map :oid (:koulutukset hakukohde))
                                      (map #(tarjonta-protocol/get-koulutus tarjonta-service %))
                                      (map parse-koulutus))
     :hakuaika                   (hakuaika/get-hakuaika-info haku ohjausparametrit hakukohde)
     :applicable-base-educations (applicable-base-educations hakukohde
                                                             pohjakoulutusvaatimuskorkeakoulut)}))

(defn parse-tarjonta-info-by-haku
  ([koodisto-cache tarjonta-service organization-service ohjausparametrit-service haku-oid included-hakukohde-oids]
   {:pre [(some? tarjonta-service)
          (some? organization-service)
          (some? ohjausparametrit-service)]}
   (when haku-oid
     (let [hakukohderyhmat                   (->> (organization-service/get-hakukohde-groups
                                                    organization-service)
                                                  (map :oid)
                                                  (set))
           haku                              (tarjonta-protocol/get-haku
                                               tarjonta-service
                                               haku-oid)
           ohjausparametrit                  (ohjausparametrit-protocol/get-parametri
                                               ohjausparametrit-service
                                               haku-oid)
           pohjakoulutusvaatimuskorkeakoulut (get-koodisto-options koodisto-cache "pohjakoulutusvaatimuskorkeakoulut" 1)
           hakukohteet                       (->> included-hakukohde-oids
                                                  (keep #(tarjonta-protocol/get-hakukohde
                                                           tarjonta-service
                                                           %))
                                                  (map #(parse-hakukohde tarjonta-service
                                                          hakukohderyhmat
                                                          haku
                                                          ohjausparametrit
                                                          pohjakoulutusvaatimuskorkeakoulut
                                                          %)))
           max-hakukohteet                   (:maxHakukohdes haku)]
       (when (not-empty hakukohteet)
         {:tarjonta
          {:hakukohteet                      hakukohteet
           :haku-oid                         haku-oid
           :haku-name                        (-> haku :nimi (clojure.set/rename-keys lang-key-renames) localized-names)
           :prioritize-hakukohteet           (:usePriority haku)
           :max-hakukohteet                  (when (and max-hakukohteet (pos? max-hakukohteet))
                                               max-hakukohteet)
           :hakuaika                         (hakuaika/select-hakuaika (map :hakuaika hakukohteet))
           :can-submit-multiple-applications (:canSubmitMultipleApplications haku)
           :yhteishaku                       (yhteishaku? haku)}}))))
  ([koodisto-cache tarjonta-service organization-service ohjausparametrit-service haku-oid]
   (when haku-oid
     (parse-tarjonta-info-by-haku koodisto-cache
                                  tarjonta-service
                                  organization-service
                                  ohjausparametrit-service
                                  haku-oid
                                  (or (->> haku-oid
                                           (.get-haku tarjonta-service)
                                           :hakukohdeOids)
                                      [])))))
