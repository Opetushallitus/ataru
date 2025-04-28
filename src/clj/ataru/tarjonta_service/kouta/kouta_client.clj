(ns ataru.tarjonta-service.kouta.kouta-client
  (:require [ataru.cache.cache-service :as cache-service]
            [ataru.cas.client :as cas-client]
            [ataru.config.url-helper :as url-helper]
            [ataru.ohjausparametrit.ohjausparametrit-client :as ohjausparametrit-client]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.hakukohderyhmapalvelu-service.hakukohderyhmapalvelu-service :as hakukohderyhmapalvelu-service]
            [ataru.schema.form-schema :as form-schema]
            [cheshire.core :as json]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [schema.core :as s]
            [clojure.string :as string]
            [taoensso.timbre :as log]
            [ataru.tarjonta-service.hakuaika :as hakuaika]))

(def haku-checker (s/checker form-schema/Haku))
(def hakukohde-checker (s/checker form-schema/Hakukohde))
(def toteutus-checker (s/checker form-schema/Koulutus))
(def hakus-by-checker (s/checker [s/Str]))
(def hakukohde-search-checker (s/checker [s/Str]))
(def hakukohderyhma-settings-checker (s/checker form-schema/HakukohderyhmaSettings))

(def KOUTA_OID_LENGTH 35)

(defn- parse-date-time
  [s]

  (let [tz (t/time-zone-for-id "Europe/Helsinki")
        fmt-with-seconds (f/formatter "yyyy-MM-dd'T'HH:mm:ss" tz)
        fmt (f/formatter "yyyy-MM-dd'T'HH:mm" tz)]
    (try
      (t/to-time-zone (f/parse fmt-with-seconds s) tz)
      (catch Exception _
        (t/to-time-zone (f/parse fmt s) tz)))))

(defn parse-haku
  [haku hakukohteet ohjausparametrit]
  (let [hakuajat (mapv (fn [hakuaika]
                         (merge
                           {:hakuaika-id "kouta-hakuaika-id"
                            :start (parse-date-time (:alkaa hakuaika))}
                           (when-let [paattyy (:paattyy hakuaika)]
                             {:end (parse-date-time paattyy)})))
                       (:hakuajat haku))
        virkailijan-valinta-kaytto-estetty (get ohjausparametrit :PH_OLVVPKE false)]
    (merge
     {:can-submit-multiple-applications           (get ohjausparametrit :useitaHakemuksia false)
      :hakuajat                                   hakuajat
      :alkamiskausi                               (:alkamiskausiKoodiUri haku)
      :alkamisvuosi                               (when (:alkamisvuosi haku)
                                                    (Integer/parseInt (:alkamisvuosi haku)))
      :hakukohteet                                (mapv :oid hakukohteet)
      :hakutapa-uri                               (:hakutapaKoodiUri haku)
      :haun-tiedot-url                            (url-helper/resolve-url :kouta-app.haku (:oid haku))
      :kohdejoukko-uri                            (:kohdejoukkoKoodiUri haku)
      :kohdejoukon-tarkenne-uri                   (:kohdejoukonTarkenneKoodiUri haku)
      :name                                       (:nimi haku)
      :oid                                        (:oid haku)
      :prioritize-hakukohteet                     (get ohjausparametrit :jarjestetytHakutoiveet false)
      :sijoittelu                                 (get ohjausparametrit :sijoittelu false)
      :yhteishaku                                 (string/starts-with?
                                                   (:hakutapaKoodiUri haku)
                                                   "hakutapa_01")
      :ylioppilastutkinto-antaa-hakukelpoisuuden? false
      :maksullinen-kk-haku?                        (or (:maksullinenKkHaku haku) false)}
     (when (get ohjausparametrit :hakutoiveidenMaaraRajoitettu false)
       {:max-hakukohteet (get ohjausparametrit :hakutoiveidenEnimmaismaara 1)})
     (when (seq hakuajat)
       {:hakukausi-vuosi (->> hakuajat
                              (map #(t/year (:start %)))
                              (apply max))})
     (when (some? (:hakulomakeAtaruId haku))
       {:ataru-form-key (:hakulomakeAtaruId haku)})
     (when virkailijan-valinta-kaytto-estetty
       {:valinnat-estetty-time-window virkailijan-valinta-kaytto-estetty}))))

(defn- parse-can-be-applied-to?
  [hakukohde]
  (case (:tila hakukohde)
    "tallennettu" false
    "julkaistu"   true
    "arkistoitu"  false
    (throw
     (new RuntimeException
          (str "Unknown hakukohteen tila " (:tila hakukohde)
               " in hakukohde " (:oid hakukohde))))))

(defn- parse-liite-toimitusosoite
  [toimitusosoite]
  {:osoite      (get-in toimitusosoite [:osoite :osoite])
   :postinumero (get-in toimitusosoite [:osoite :postinumero])
   :verkkosivu  (:verkkosivu toimitusosoite)})

(defn- parse-hakukohde-liitteet
  [hakukohde]
  (let [parse-liite (fn [liite]
                      {:tyyppi               (get-in liite [:tyyppi :koodiUri])
                       :toimitusaika         (when (seq (:toimitusaika liite))
                                               (-> (:toimitusaika liite)
                                                   (hakuaika/basic-date-time-str->date-time)
                                                   (hakuaika/date-time->localized-date-time)))
                       :toimitetaan-erikseen (= "osoite" (:toimitustapa liite))
                       :toimitusosoite       (parse-liite-toimitusosoite (:toimitusosoite liite))})]
  (->> hakukohde
       (:liitteet)
       (map #(parse-liite %)))))

(defn parse-hakukohde
  [hakukohde tarjoajat hakukohderyhmas settings]
  (merge
    {:oid                                                         (:oid hakukohde)
     :hakukohteen-tiedot-url                                      (url-helper/resolve-url :kouta-app.hakukohde (:oid hakukohde))
     :can-be-applied-to?                                          (parse-can-be-applied-to? hakukohde)
     :archived                                                    (= "arkistoitu" (:tila hakukohde))
     :haku-oid                                                    (:hakuOid hakukohde)
     :koulutus-oids                                               [(:toteutusOid hakukohde)]
     :name                                                        (:nimi hakukohde)
     :tarjoaja-name                                               (or (:name (first tarjoajat)) {})
     :tarjoaja-oids                                               (mapv :oid tarjoajat)
     :ryhmaliitokset                                              hakukohderyhmas
     :hakukelpoisuusvaatimus-uris                                 (:pohjakoulutusvaatimusKoodiUrit hakukohde)
     :ylioppilastutkinto-antaa-hakukelpoisuuden?                  false
     :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja? (boolean (some #(:jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja %) settings))
     :yo-amm-autom-hakukelpoisuus                                 (boolean (some #(:yo-amm-autom-hakukelpoisuus %) settings))
     :koulutustyyppikoodi                                         (:koulutustyyppikoodi hakukohde)
     :liitteet                                                    (parse-hakukohde-liitteet hakukohde)
     :liitteet-onko-sama-toimitusosoite?                          (boolean (:liitteetOnkoSamaToimitusosoite hakukohde))
     :liitteiden-toimitusosoite                                   (some-> hakukohde
                                                                          :liitteidenToimitusosoite
                                                                          (parse-liite-toimitusosoite))
     :liitteet-onko-sama-toimitusaika?                            (boolean (:liitteetOnkoSamaToimitusaika hakukohde))
     :liitteiden-toimitusaika                                     (some-> hakukohde
                                                                    :liitteidenToimitusaika
                                                                    (hakuaika/basic-date-time-str->date-time)
                                                                    (hakuaika/date-time->localized-date-time))
     :tutkintoon-johtava?                                         (boolean (:johtaaTutkintoon hakukohde))
     :voiko-hakukohteessa-olla-harkinnanvaraisesti-hakeneita?     (boolean (:voikoHakukohteessaOllaHarkinnanvaraisestiHakeneita hakukohde))
     :opetuskieli-koodi-urit                                      (:opetuskieliKoodiUrit hakukohde)}
   (if (:kaytetaanHaunAikataulua hakukohde)
     {:hakuaika-id "kouta-hakuaika-id"}
     {:hakuajat (mapv (fn [hakuaika]
                        (merge
                         {:start (parse-date-time (:alkaa hakuaika))}
                         (when-let [paattyy (:paattyy hakuaika)]
                           {:end (parse-date-time paattyy)})))
                      (:hakuajat hakukohde))})))

(defn- get-result
  [url cas-client]
  (log/debug "get-result" url)
  (let [{:keys [status body]} (cas-client/cas-authenticated-get
                               cas-client
                               url)]
    (case status
      200 (json/parse-string body true)
      404 nil
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " status ", "
                                        "body: " body))))))

(s/defn ^:always-validate get-haku :- (s/maybe form-schema/Haku)
  [haku-oid :- s/Str
   cas-client]
  (when (= (count haku-oid) KOUTA_OID_LENGTH)
    (when-let [haku (some-> :kouta-internal.haku
                            (url-helper/resolve-url haku-oid)
                            (get-result cas-client))]
      (let [hakukohteet      (some-> :kouta-internal.hakukohde-search
                                     (url-helper/resolve-url {"haku" haku-oid})
                                     (get-result cas-client))
            ohjausparametrit (ohjausparametrit-client/get-ohjausparametrit haku-oid)]
        (parse-haku haku hakukohteet ohjausparametrit)))))

(s/defn ^:always-validate get-hakus-by-form-key :- [s/Str]
  [cas-client form-key]
  (some-> :kouta-internal.haku-search
          (url-helper/resolve-url {"ataruId" form-key})
          (get-result cas-client)
          ((fn [result] (mapv :oid result)))))

(s/defn ^:always-validate get-hakukohde :- (s/maybe form-schema/Hakukohde)
  [hakukohde-oid :- s/Str
   organization-service
   hakukohderyhmapalvelu-service
   cas-client
   hakukohderyhma-settings-cache]
  (when (= (count hakukohde-oid) KOUTA_OID_LENGTH)
    (when-let [hakukohde (some-> :kouta-internal.hakukohde
                                 (url-helper/resolve-url hakukohde-oid)
                                 (get-result cas-client))]
      (let [tarjoajat (some->> (seq [(:tarjoaja hakukohde)])
                               (organization-service/get-organizations-for-oids
                                organization-service))
            hakukohderyhmas (hakukohderyhmapalvelu-service/get-hakukohderyhma-oids-for-hakukohde
                              hakukohderyhmapalvelu-service hakukohde-oid)
            settings (map #(cache-service/get-from hakukohderyhma-settings-cache %) hakukohderyhmas)]
        (parse-hakukohde hakukohde tarjoajat hakukohderyhmas settings)))))

(s/defn ^:always-validate get-hakukohderyhma-settings :- (s/maybe s/Any)
  [hakukohderyhma-oid :- s/Str
   hakukohderyhmapalvelu-service]
  (hakukohderyhmapalvelu-service/get-settings-for-hakukohderyhma hakukohderyhmapalvelu-service hakukohderyhma-oid))


(s/defn ^:always-validate get-hakukohdes-by :- (s/maybe [s/Str])
  [cas-client
   query :- {:haku-oid                      s/Str
             (s/optional-key :tarjoaja-oid) s/Str}]
  (when (= (count (:haku-oid query)) KOUTA_OID_LENGTH)
    (some-> :kouta-internal.hakukohde-search
            (url-helper/resolve-url
             (cond-> {"haku" (:haku-oid query)}
                     (contains? query :tarjoaja-oid)
                     (assoc "tarjoaja" (:tarjoaja-oid query))))
            (get-result cas-client)
            ((fn [result] (mapv :oid result))))))

(s/defn ^:always-validate get-toteutus :- (s/maybe form-schema/Koulutus)
  [toteutus-oid :- s/Str]
  {:oid                  toteutus-oid
   :koulutuskoodi-name   {}
   :koulutusohjelma-name {}
   :tutkintonimike-names []})

(s/defn ^:always-validate get-haku-oids :- (s/maybe [s/Str])
  [cas-client]
  (some-> :kouta-internal.haku-search
          (url-helper/resolve-url)
          (get-result cas-client)
          ((fn [result] (mapv :oid result)))))

(defrecord CacheLoader [cas-client]
  cache-service/CacheLoader

  (load [_ haku-oid]
    (get-haku haku-oid cas-client))

  (load-many [this haku-oids]
    (cache-service/default-load-many this haku-oids))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (haku-checker response)))

(defrecord HakukohdeCacheLoader [cas-client organization-service hakukohderyhmapalvelu-service hakukohderyhma-settings-cache]
  cache-service/CacheLoader

  (load [_ hakukohde-oid]
    (get-hakukohde hakukohde-oid organization-service hakukohderyhmapalvelu-service cas-client hakukohderyhma-settings-cache))

  (load-many [this hakukohde-oids]
    (cache-service/default-load-many this hakukohde-oids))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakukohde-checker response)))

(defrecord HakukohderyhmaSettingsLoader [hakukohderyhmapalvelu-service]
  cache-service/CacheLoader

  (load [_ hakukohderyhma-oid]
    (get-hakukohderyhma-settings hakukohderyhma-oid hakukohderyhmapalvelu-service))

  (load-many [this hakukohderyhma-oids]
    (cache-service/default-load-many this hakukohderyhma-oids))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakukohderyhma-settings-checker response)))

(defrecord HakusByFormKeyCacheLoader [cas-client]
  cache-service/CacheLoader

  (load [_ form-key]
    (get-hakus-by-form-key cas-client form-key))

  (load-many [this form-keys]
    (cache-service/default-load-many this form-keys))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakus-by-checker response)))

(defrecord HakukohdeSearchCacheLoader [cas-client]
  cache-service/CacheLoader

  (load [_ key]
    (let [[haku-oid organization-oid] (string/split key #"#")]
      (get-hakukohdes-by
       cas-client
       (cond-> {:haku-oid haku-oid}
               (some? organization-oid)
               (assoc :tarjoaja-oid organization-oid)))))

  (load-many [this form-keys]
    (cache-service/default-load-many this form-keys))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakukohde-search-checker response)))
