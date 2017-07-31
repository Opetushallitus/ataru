(ns ataru.tarjonta-service.tarjonta-parser
  (:require [taoensso.timbre :as log]
            [ataru.tarjonta-service.hakuaika :as hakuaika]))

(def ^:private lang-key-renames {:kieli_fi :fi :kieli_en :en :kieli_sv :sv})

(defn- localized-names
  [names]
  (into {}
        (for [lang [:fi :sv :en]
              :when (contains? names lang)]
          [lang (get-in names [lang :nimi])])))

(defn- parse-koulutuskoodi
  [koulutus]
  (-> koulutus
      :koulutuskoodi
      :meta
      (clojure.set/rename-keys lang-key-renames)
      localized-names))

(defn- parse-tutkintonimike
  [koulutus]
  (if-let [ms (-> koulutus :tutkintonimikes :meta)]
    (do (when (< 1 (count ms))
          (log/warn (format "Koulutuksella %s useita tutkintonimikekoodistoja"
                            (:oid koulutus))))
        (-> ms
            first
            val
            :meta
            (clojure.set/rename-keys lang-key-renames)
            localized-names))
    {}))

(defn- parse-koulutus
  [response]
  {:oid                  (:oid response)
   :koulutuskoodi-name   (parse-koulutuskoodi response)
   :tutkintonimike-name  (parse-tutkintonimike response)
   :koulutusohjelma-name (-> response :koulutusohjelma :nimi)
   :tarkenne             (:tarkenne response)})

(defn parse-hakukohde
  [tarjonta-service hakukohde]
  (when (:oid hakukohde)
    {:oid           (:oid hakukohde)
     :name          (clojure.set/rename-keys
                      (:hakukohteenNimet hakukohde)
                      lang-key-renames)
     :tarjoaja-name (:tarjoajaNimet hakukohde)
     :form-key      (:ataruLomakeAvain hakukohde)
     :koulutukset   (->> (map :oid (:koulutukset hakukohde))
                         (map #(.get-koulutus tarjonta-service %))
                         (map parse-koulutus))}))

(defn parse-tarjonta-info-by-haku
  ([tarjonta-service haku-oid included-hakukohde-oids]
   (when haku-oid
     (let [haku        (.get-haku tarjonta-service haku-oid)
           hakukohteet (->> included-hakukohde-oids
                            (map #(.get-hakukohde tarjonta-service %))
                            (map #(parse-hakukohde tarjonta-service %))
                            (remove nil?))
           max-hakukohteet (:maxHakukohdes haku)]
       (when (pos? (count hakukohteet))                     ;; If tarjonta doesn't return hakukohde, let's not return a crippled map here
         {:tarjonta
          {:hakukohteet     hakukohteet
           :haku-oid        haku-oid
           :haku-name       (-> haku :nimi :kieli_fi)
           :max-hakukohteet (when (and max-hakukohteet (pos? max-hakukohteet))
                              max-hakukohteet)
           :hakuaika-dates  (hakuaika/get-hakuaika-info
                             (first hakukohteet)
                             haku) ; TODO take into account each hakukohde time?
           }}))))
  ([tarjonta-service haku-oid]
   (when haku-oid
     (parse-tarjonta-info-by-haku tarjonta-service haku-oid (or (->> haku-oid
                                                                     (.get-haku tarjonta-service)
                                                                     :hakukohdeOids)
                                                                [])))))
