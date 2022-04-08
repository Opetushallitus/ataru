(ns ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-service
  (:require [ataru.cache.cache-service :as cache]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol :refer [ValintalaskentakoostepalveluService]]
            [schema.core :as s]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-util :as hutil]
            [ataru.application.harkinnanvaraisuus.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-types]]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-client :as client]
            [ataru.applications.application-store :as application-store]
            [ataru.util :refer [answers-by-key]]))

(s/defschema HakukohdeValintalaskentaResponse
             {:kayttaaValintalaskentaa s/Bool})

(s/defschema HakukohdeHarkinnanvaraisuusResponse
             {:hakemusOid s/Str
              (s/optional-key :henkiloOid) s/Str
              :hakutoiveet [{:hakukohdeOid s/Str
                             :harkinnanvaraisuudenSyy (apply s/enum harkinnanvaraisuus-types)}]})

(def ^:private hakukohde-harkinnanvaraisuus-checker (s/checker HakukohdeHarkinnanvaraisuusResponse))

(def ^:private hakukohde-valintalaskenta-checker (s/checker HakukohdeValintalaskentaResponse))

(defn- convert-application-to-have-harkinnanvaraisuus-reasons
  [hahkukohde-cache application]
  (let [answers (answers-by-key (:answers application))
        hakukohteet-with-harkinnanvaraisuus (->> (:hakukohde application)
                                                 (cache/get-many-from hahkukohde-cache)
                                                 (map #(hutil/assoc-harkinnanvaraisuustieto-to-hakukohde answers %)))]
    {:hakutoiveet hakukohteet-with-harkinnanvaraisuus
     :hakemusOid (:key application)
     :henkiloOid (:person-oid application)}))

(defn- hakemusten-harkinnanvaraisuus-valintalaskennasta
  [valintalaskentakoostepalvelu-cas-client hahkukohde-cache hakemus-oids]
  (let [applications (application-store/get-applications-by-keys hakemus-oids)
        request-body (map #(convert-application-to-have-harkinnanvaraisuus-reasons hahkukohde-cache %) applications)]
    (client/hakemusten-harkinnanvaraisuus-valintalaskennasta valintalaskentakoostepalvelu-cas-client request-body)))

(defrecord HakukohdeValintalaskentaCacheLoader [valintalaskentakoostepalvelu-cas-client]
  cache/CacheLoader

  (load [_ hakukohde-oid]
    (client/hakukohde-uses-valintalaskenta? valintalaskentakoostepalvelu-cas-client
                                     hakukohde-oid))

  (load-many [_ hakukohde-oids]
    (reduce (fn [acc hakukohde-oid]
              (let [result (client/hakukohde-uses-valintalaskenta? valintalaskentakoostepalvelu-cas-client
                                                            hakukohde-oid)]
                (assoc acc hakukohde-oid result)))
            {}
            hakukohde-oids))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakukohde-valintalaskenta-checker response)))

(defrecord HakukohdeHarkinnanvaraisuusCacheLoader [valintalaskentakoostepalvelu-cas-client hahkukohde-cache]
  cache/CacheLoader

  (load [_ application-oid]
    (hakemusten-harkinnanvaraisuus-valintalaskennasta valintalaskentakoostepalvelu-cas-client
                                                      hahkukohde-cache
                                                      [application-oid]))

  (load-many [_ application-oids]
    (let [applications (hakemusten-harkinnanvaraisuus-valintalaskennasta valintalaskentakoostepalvelu-cas-client
                                                                         hahkukohde-cache
                                                                         application-oids)]
      (reduce (fn [acc application]
                (assoc acc (:hakemusOid application) application)) {} applications)))

  (load-many-size [_]
    1)

  (check-schema [_ response]
    (hakukohde-harkinnanvaraisuus-checker response)))

(defrecord CachedValintalaskentakoostepalveluService [valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache
                                                      valintalaskentakoostepalvelu-hakukohde-harkinnanvaraisuus-cache
                                                      valintalaskentakoostepalvelu-cas-client]
  ValintalaskentakoostepalveluService

  (hakukohde-uses-valintalaskenta? [_ hakukohde-oid]
    (cache/get-from valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache
                    hakukohde-oid))

  (opiskelijan-suoritukset [_ haku-oid hakemus-oid]
    (client/opiskelijan-suoritukset
      valintalaskentakoostepalvelu-cas-client
      haku-oid
      hakemus-oid))

  (hakemusten-harkinnanvaraisuus-valintalaskennasta [_ hakemus-oids]
    (cache/get-many-from valintalaskentakoostepalvelu-hakukohde-harkinnanvaraisuus-cache hakemus-oids)))

(defn new-valintalaskentakoostepalvelu-service []
  (map->CachedValintalaskentakoostepalveluService {}))
