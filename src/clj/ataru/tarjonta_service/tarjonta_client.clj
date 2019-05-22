(ns ataru.tarjonta-service.tarjonta-client
  (:require
   [ataru.config.url-helper :refer [resolve-url]]
   [ataru.schema.form-schema :as schema]
   [ataru.util.http-util :as http-util]
   [cheshire.core :as json]
   [clojure.string :as string]
   [schema.core :as s]
   [taoensso.timbre :refer [warn info]]))

(def koulutus-checker (s/checker schema/Koulutus))
(def hakukohde-checker (s/checker schema/Hakukohde))

(defn- localized-names
  ([names]
   (localized-names names identity))
  ([names key-fn]
   (into {}
         (for [[input-k output-k] [[:kieli_fi :fi]
                                   [:kieli_sv :sv]
                                   [:kieli_en :en]]
               :let               [val (key-fn (or (input-k names) (output-k names)))]
               :when              (not (clojure.string/blank? val))]
           [output-k val]))))

(defn- parse-koulutuskoodi
  [koulutus]
  (-> koulutus
      :koulutuskoodi
      :meta
      (localized-names :nimi)))

(defn- parse-tutkintonimikes
  [koulutus]
  (mapv (fn [[_ name]]
          (-> name
              :meta
              (localized-names :nimi)))
        (-> koulutus :tutkintonimikes :meta)))

(defn- parse-koulutusohjelma
  [koulutus]
  (-> koulutus
      :koulutusohjelma
      :tekstis
      localized-names))

(defn- parse-koulutus
  [response]
  (cond-> {:oid                  (:oid response)
           :koulutuskoodi-name   (parse-koulutuskoodi response)
           :tutkintonimike-names (parse-tutkintonimikes response)
           :koulutusohjelma-name (parse-koulutusohjelma response)}
          (not (clojure.string/blank? (:tarkenne response)))
          (assoc :tarkenne (:tarkenne response))))

(defn- parse-hakukohde-tila
  [hakukohde]
  (case (:tila hakukohde)
    "POISTETTU"     :poistettu
    "LUONNOS"       :luonnos
    "VALMIS"        :valmis
    "JULKAISTU"     :julkaistu
    "PERUTTU"       :peruttu
    "KOPIOITU"      :kopioitu
    "PUUTTEELLINEN" :puutteellinen
    (throw
     (new RuntimeException
          (str "Unknown hakukohteen tila " (:tila hakukohde)
               " in hakukohde " (:oid hakukohde))))))

(defn parse-hakukohde
  [hakukohde]
  (let [kaytetaan-hakukohdekohtaista-hakuaikaa? (boolean (:kaytetaanHakukohdekohtaistaHakuaikaa hakukohde))]
    (merge {:oid                                        (:oid hakukohde)
            :tila                                       (parse-hakukohde-tila hakukohde)
            :haku-oid                                   (:hakuOid hakukohde)
            :koulutus-oids                              (map :oid (:koulutukset hakukohde))
            :name                                       (localized-names (:hakukohteenNimet hakukohde))
            :tarjoaja-name                              (localized-names (:tarjoajaNimet hakukohde))
            :tarjoaja-oids                              (:tarjoajaOids hakukohde)
            :ryhmaliitokset                             (some->> (:ryhmaliitokset hakukohde)
                                                                 (map :ryhmaOid)
                                                                 (distinct))
            :kaytetaan-hakukohdekohtaista-hakuaikaa?    kaytetaan-hakukohdekohtaista-hakuaikaa?
            :hakukelpoisuusvaatimus-uris                (:hakukelpoisuusvaatimusUris hakukohde)
            :ylioppilastutkinto-antaa-hakukelpoisuuden? (boolean (:ylioppilastutkintoAntaaHakukelpoisuuden hakukohde))}
           (if kaytetaan-hakukohdekohtaista-hakuaikaa?
             (merge {:hakuaika-alku (:hakuaikaAlkuPvm hakukohde)}
                    (when (some? (:hakuaikaLoppuPvm hakukohde))
                      {:hakuaika-loppu (:hakuaikaLoppuPvm hakukohde)}))
             {:hakuaika-id (:hakuaikaId hakukohde)}))))

(defn- get-result
  [url]
  (let [{:keys [status body]} (http-util/do-get url)]
    (when (not= 200 status)
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " status ", "
                                        "body: " body))))
    (let [{:keys [status result]} (json/parse-string body true)]
      (when (not= "OK" status)
        (throw (new RuntimeException (str "Could not get " url ", "
                                          "tarjonta status: " status))))
      result)))

(defn- try-get-result
  [url]
  (let [{:keys [status body]} (http-util/do-get url)]
    (when (= 200 status)
      (:result (json/parse-string body true)))))

(s/defn ^:always-validate get-hakukohde :- schema/Hakukohde
  [hakukohde-oid :- s/Str]
  (-> :tarjonta-service.hakukohde
      (resolve-url hakukohde-oid)
      get-result
      parse-hakukohde))

(defn hakukohde-search
  [haku-oid organization-oid]
  (-> :tarjonta-service.hakukohde.search
      (resolve-url (cond-> {"hakuOid"         haku-oid
                            "defaultTarjoaja" organization-oid}
                           (some? organization-oid)
                           (assoc "organisationOid" organization-oid)))
      try-get-result))

(defn get-haku
  [haku-oid]
  (-> :tarjonta-service.haku
      (resolve-url haku-oid)
      try-get-result))

(s/defn ^:always-validate get-koulutus :- schema/Koulutus
  [koulutus-oid :- s/Str]
  (-> :tarjonta-service.koulutus
      (resolve-url koulutus-oid)
      get-result
      parse-koulutus))

(defn get-forms-in-use
  [organization-oid]
  (-> :tarjonta-service.forms-in-use
      (resolve-url {"oid" organization-oid})
      try-get-result))
