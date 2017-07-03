(ns ataru.tarjonta-service.tarjonta-parser
  (:require [ataru.tarjonta-service.hakuaika :as hakuaika]))

(defn- parse-koulutus
  [response]
  {:oid                  (:oid response)
   :koulutuskoodi-name   (-> response :koulutuskoodi :nimi)
   :tutkintonimike-name  (-> response :tutkintonimike :nimi)
   :koulutusohjelma-name (-> response :koulutusohjelma :nimi)
   :tarkenne             (:tarkenne response)})

(defn parse-hakukohde
  [tarjonta-service hakukohde]
  (when (:oid hakukohde)
    {:oid           (:oid hakukohde)
     :name          (clojure.set/rename-keys
                      (:hakukohteenNimet hakukohde)
                      {:kieli_fi :fi :kieli_en :en :kieli_sv :sv})
     :tarjoaja-name (:tarjoajaNimet hakukohde)
     :form-key      (:ataruLomakeAvain hakukohde)
     :koulutukset   (->> (map :oid (:koulutukset hakukohde))
                         (map #(.get-koulutus tarjonta-service %))
                         (map parse-koulutus))}))

(defn parse-tarjonta-info-by-haku
  [tarjonta-service haku-oid]
  (when haku-oid
    (let [haku          (.get-haku tarjonta-service haku-oid)
          hakukohteet   (->> (:hakukohdeOids haku)
                             (map #(.get-hakukohde tarjonta-service %))
                             (map #(parse-hakukohde tarjonta-service %))
                             (remove nil?))]
      (when (pos? (count hakukohteet))                      ;; If tarjonta doesn't return hakukohde, let's not return a crippled map here
        {:tarjonta
         {:hakukohteet    hakukohteet
          :haku-oid       haku-oid
          :haku-name      (-> haku :nimi :kieli_fi)
          :hakuaika-dates (hakuaika/get-hakuaika-info (first hakukohteet) haku) ; TODO take into account each hakukohde time?
          }}))))

(defn parse-tarjonta-info-by-hakukohde
  [tarjonta-service hakukohde-oid]
  (when hakukohde-oid
    (let [hakukohde     (.get-hakukohde tarjonta-service hakukohde-oid)
          haku-oid      (:hakuOid hakukohde)]
      (parse-tarjonta-info-by-haku tarjonta-service haku-oid))))
