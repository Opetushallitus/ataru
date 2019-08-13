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
    (merge {:oid                                                         (:oid hakukohde)
            :tila                                                        (parse-hakukohde-tila hakukohde)
            :haku-oid                                                    (:hakuOid hakukohde)
            :koulutus-oids                                               (map :oid (:koulutukset hakukohde))
            :name                                                        (localized-names (:hakukohteenNimet hakukohde))
            :tarjoaja-name                                               (localized-names (:tarjoajaNimet hakukohde))
            :tarjoaja-oids                                               (:tarjoajaOids hakukohde)
            :ryhmaliitokset                                              (some->> (:ryhmaliitokset hakukohde)
                                                                                  (map :ryhmaOid)
                                                                                  (distinct))
            :kaytetaan-hakukohdekohtaista-hakuaikaa?                     kaytetaan-hakukohdekohtaista-hakuaikaa?
            :hakukelpoisuusvaatimus-uris                                 (:hakukelpoisuusvaatimusUris hakukohde)
            :ylioppilastutkinto-antaa-hakukelpoisuuden?                  (boolean (:ylioppilastutkintoAntaaHakukelpoisuuden hakukohde))
            :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja? (boolean (:josYoEiMuitaLiitepyyntoja hakukohde))}
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
      (case status
        "OK"        result
        "NOT_FOUND" nil
        (throw (new RuntimeException (str "Could not get " url ", "
                                          "status: " status ", "
                                          "body: " body)))))))

(s/defn ^:always-validate get-hakukohde :- (s/maybe schema/Hakukohde)
  [hakukohde-oid :- s/Str]
  (some-> :tarjonta-service.hakukohde
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
      get-result))

(defn get-haku
  [haku-oid]
  (-> :tarjonta-service.haku
      (resolve-url haku-oid)
      get-result))

(s/defn ^:always-validate get-koulutus :- (s/maybe schema/Koulutus)
  [koulutus-oid :- s/Str]
  (some-> :tarjonta-service.koulutus
          (resolve-url koulutus-oid)
          get-result
          parse-koulutus))

(defn get-forms-in-use
  [organization-oid]
  (-> :tarjonta-service.forms-in-use
      (resolve-url {"oid" organization-oid})
      get-result))
