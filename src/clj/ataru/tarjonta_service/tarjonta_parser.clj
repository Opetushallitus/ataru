(ns ataru.tarjonta-service.tarjonta-parser
  (:require [clj-time.core :as t]
            [clojure.string :as string]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [ataru.koodisto.koodisto :refer [get-koodisto-options]]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-protocol]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit-protocol]))

(defn- parse-hakukohde
  [_
   now
   hakukohderyhmat
   haku
   tarjonta-koulutukset
   ohjausparametrit
   pohjakoulutukset-by-vaatimus
   hakukohde]
  (when (:oid hakukohde)
    {:oid                                                         (:oid hakukohde)
     :name                                                        (:name hakukohde)
     :can-be-applied-to?                                          (:can-be-applied-to? hakukohde)
     :archived                                                    (:archived hakukohde)
     :hakukohderyhmat                                             (filter #(contains? hakukohderyhmat %) (:ryhmaliitokset hakukohde))
     :kohdejoukko-korkeakoulu?                                    (string/starts-with?
                                                                   (:kohdejoukko-uri haku)
                                                                   "haunkohdejoukko_12#")
     :tarjoaja-name                                               (:tarjoaja-name hakukohde)
     :form-key                                                    (:ataru-form-key haku)
     :koulutukset                                                 (mapv #(or (get tarjonta-koulutukset %)
                                                                             (throw (new RuntimeException (str "Koulutus " % " not found"))))
                                                                        (:koulutus-oids hakukohde))
     :koulutustyyppikoodi                                         (:koulutustyyppikoodi hakukohde)
     :hakuaika                                                    (hakuaika/hakukohteen-hakuaika now haku ohjausparametrit hakukohde)
     :applicable-base-educations                                  (mapcat pohjakoulutukset-by-vaatimus
                                                                          (map #(first (string/split % #"#")) (:hakukelpoisuusvaatimus-uris hakukohde)))
     :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja? (boolean (:jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja? hakukohde))
     :liitteet                                                    (:liitteet hakukohde)
     :liitteet-onko-sama-toimitusosoite?                          (boolean (:liitteet-onko-sama-toimitusosoite? hakukohde))
     :liitteiden-toimitusosoite                                   (:liitteiden-toimitusosoite hakukohde)
     :liitteet-onko-sama-toimitusaika?                            (boolean (:liitteet-onko-sama-toimitusaika? hakukohde))
     :liitteiden-toimitusaika                                     (:liitteiden-toimitusaika hakukohde)}))

(defn- pohjakoulutukset-by-vaatimus
  [pohjakoulutusvaatimuskorkeakoulut]
  (reduce (fn [m {:keys [uri within]}]
            (assoc m uri (->> within
                              (filter #(string/starts-with? (:uri %) "pohjakoulutuskklomake_"))
                              (map :value))))
          {}
          pohjakoulutusvaatimuskorkeakoulut))

(defn parse-tarjonta-info-by-haku
  ([koodisto-cache tarjonta-service organization-service ohjausparametrit-service haku-oid included-hakukohde-oids]
   {:pre [(some? tarjonta-service)
          (some? organization-service)
          (some? ohjausparametrit-service)]}
   (when haku-oid
     (let [now                               (t/now)
           hakukohderyhmat                   (->> (organization-service/get-hakukohde-groups
                                                   organization-service)
                                                  (map :oid)
                                                  (set))
           haku                              (tarjonta-protocol/get-haku
                                              tarjonta-service
                                              haku-oid)
           ohjausparametrit                  (ohjausparametrit-protocol/get-parametri
                                              ohjausparametrit-service
                                              haku-oid)
           pohjakoulutukset-by-vaatimus      (pohjakoulutukset-by-vaatimus
                                              (concat
                                               (get-koodisto-options koodisto-cache
                                                                     "pohjakoulutusvaatimuskouta"
                                                                     1
                                                                     false)
                                               (get-koodisto-options koodisto-cache
                                                                     "pohjakoulutusvaatimuskorkeakoulut"
                                                                     1
                                                                     false)))
           tarjonta-hakukohteet              (tarjonta-protocol/get-hakukohteet tarjonta-service
                                                                                included-hakukohde-oids)
           tarjonta-koulutukset              (->> tarjonta-hakukohteet
                                                  (mapcat :koulutus-oids)
                                                  distinct
                                                  (tarjonta-protocol/get-koulutukset tarjonta-service))
           hakukohteet                       (map #(parse-hakukohde tarjonta-service
                                                                    now
                                                                    hakukohderyhmat
                                                                    haku
                                                                    tarjonta-koulutukset
                                                                    ohjausparametrit
                                                                    pohjakoulutukset-by-vaatimus
                                                                    %)
                                                  tarjonta-hakukohteet)]
       (when (not-empty hakukohteet)
         {:tarjonta
          {:hakukohteet                      hakukohteet
           :haku-oid                         haku-oid
           :haku-name                        (:name haku)
           :prioritize-hakukohteet           (:prioritize-hakukohteet haku)
           :max-hakukohteet                  (:max-hakukohteet haku)
           :hakuaika                         (hakuaika/haun-hakuaika now haku ohjausparametrit)
           :can-submit-multiple-applications (:can-submit-multiple-applications haku)
           :kohdejoukko-uri                  (:kohdejoukko-uri haku)
           :hakutapa-uri                     (:hakutapa-uri haku)
           :yhteishaku                       (:yhteishaku haku)}}))))
  ([koodisto-cache tarjonta-service organization-service ohjausparametrit-service haku-oid]
   (when haku-oid
     (parse-tarjonta-info-by-haku koodisto-cache
                                  tarjonta-service
                                  organization-service
                                  ohjausparametrit-service
                                  haku-oid
                                  (or (->> haku-oid
                                           (tarjonta-protocol/get-haku tarjonta-service)
                                           :hakukohteet)
                                      [])))))

(defn- parse-excel-hakukohde
  [hakukohderyhmat
   hakukohde]
  (when (:oid hakukohde)
    {:oid (:oid hakukohde)
     :name (:name hakukohde)
     :tarjoaja-name   (:tarjoaja-name hakukohde)
     :hakukohderyhmat (filter #(contains? hakukohderyhmat %) (:ryhmaliitokset hakukohde))}))

(defn parse-excel-tarjonta-info-by-haku
  [tarjonta-service organization-service ohjausparametrit-service haku-oid hakukohde-oids]
  {:pre [(some? tarjonta-service)
         (some? organization-service)]}
  (when haku-oid
    (let [hakukohderyhmat      (->> (organization-service/get-hakukohde-groups
                                     organization-service)
                                    (map :oid)
                                    (set))
          ohjausparametrit     (ohjausparametrit-protocol/get-parametri
                                ohjausparametrit-service
                                haku-oid)
          tarjonta-hakukohteet (tarjonta-protocol/get-hakukohteet tarjonta-service
                                                                  hakukohde-oids)
          hakukohteet          (map #(parse-excel-hakukohde hakukohderyhmat %)
                                    tarjonta-hakukohteet)]
      (when (not-empty hakukohteet)
        {:tarjonta
         {:hakukohteet            hakukohteet
          :prioritize-hakukohteet (get ohjausparametrit :jarjestetytHakutoiveet false)}}))))
  
