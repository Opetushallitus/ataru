(ns ataru.virkailija.tarjonta
  (:require-macros [ataru.async-macros :as asyncm])
  (:require [cljs.core.async :as async]
            [ajax.core :refer [GET]]
            [cljs-time.core :refer [now after?]]
            [cljs-time.coerce :refer [from-long]]))

(defn- parse-nimi
  [nimi]
  (reduce-kv (fn [names lang s]
               (if (clojure.string/blank? s)
                 names
                 (assoc names lang s)))
             {}
             (clojure.set/rename-keys
              nimi
              {:kieli_fi :fi
               :kieli_sv :sv
               :kieli_en :en})))

(defn- parse-hakuaika
  [hakuaika]
  (cond-> {}
    (contains? hakuaika :alkuPvm)
    (assoc :alku (from-long (:alkuPvm hakuaika)))
    (contains? hakuaika :loppuPvm)
    (assoc :loppu (from-long (:loppuPvm hakuaika)))))

(defn- parse-haku
  [haku]
  {:oid (:oid haku)
   :tila (:tila haku)
   :nimi (parse-nimi (:nimi haku))
   :hakuajat (map parse-hakuaika (:hakuaikas haku))})

(defn- parse-hakukohde
  [hakukohde]
  {:oid (:oid hakukohde)
   :nimi (parse-nimi (:nimi hakukohde))
   :haku-oid (:hakuOid hakukohde)})

(defn- parse-search-response
  [search-response]
  (mapcat :tulokset (:tulokset search-response)))

(defn- parse-response
  [response]
  (:result response))

(defn- active?
  [haku]
  (let [now (now)]
    (some #(or (not (contains? % :loppu))
               (after? (:loppu %) now))
          (:hakuajat haku))))

(defn- fetch-active-haut
  []
  (let [c (async/chan 1)]
    (GET "https://itest-virkailija.oph.ware.fi/tarjonta-service/rest/v1/haku/findAll"
         {:handler (comp #(async/put! c %
                                      (fn [_] (async/close! c)))
                         (partial filter active?)
                         (partial map parse-haku)
                         parse-response)
          :error-handler #(async/put! c (new js/Error %)
                                      (fn [_] (async/close! c)))
          :response-format :json
          :keywords? true
          :timeout 10000})
    c))

(defn- hakukohde-search
  [query-params]
  (let [c (async/chan 1)
        query-str (->> query-params
                       (map #(str (first %) "=" (second %)))
                       (clojure.string/join "&"))]
    (GET (str "https://itest-virkailija.oph.ware.fi"
              "/tarjonta-service/rest/v1/hakukohde/search?"
              query-str)
         {:handler (comp #(async/put! c %
                                      (fn [_] (async/close! c)))
                         (partial map parse-hakukohde)
                         parse-search-response
                         parse-response)
          :error-handler #(async/put! c (new js/Error %)
                                      (fn [_] (async/close! c)))
          :response-format :json
          :keywords? true
          :timeout 60000})
    c))

(defn hakukohteet-of-organization
  [organization-oid]
  (let [by-org (hakukohde-search
                {"defaultTarjoaja" organization-oid
                 "organisationOid" organization-oid})
        by-org-group (hakukohde-search
                      {"organisaatioRyhmaOid" organization-oid})]
    (asyncm/go-try
     (concat (asyncm/<? by-org)
             (asyncm/<? by-org-group)))))

(defn- add-hakukohde
  [haut hakukohde]
  (if (contains? haut (:haku-oid hakukohde))
    (update-in haut [(:haku-oid hakukohde) :hakukohteet]
               (fnil conj []) hakukohde)
    haut))

(defn- add-if-hakukohteet
  [haut [haku-oid haku]]
  (if (contains? haku :hakukohteet)
    (assoc haut haku-oid haku)
    haut))

(defn- haut-by-oid
  [haut]
  (reduce (fn [m haku] (assoc m (:oid haku) haku))
          {}
          haut))

(defn active-haut
  [organization-oids]
  (let [haut-c (fetch-active-haut)
        hakukohteet-c (async/reduce
                       (fn [acc r]
                         (cond (instance? js/Error acc) acc
                               (instance? js/Error r) r
                               :else (concat acc r)))
                       []
                       (async/merge (map hakukohteet-of-organization
                                         organization-oids)
                                    (count organization-oids)))]
    (asyncm/go-try
      (->> (asyncm/<? hakukohteet-c)
           (reduce add-hakukohde (haut-by-oid (asyncm/<? haut-c)))
           (reduce add-if-hakukohteet {})))))
