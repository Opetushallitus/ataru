(ns ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-service
  (:require [ataru.cache.cache-service :as cache]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol :refer [ValintalaskentakoostepalveluService]]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-client :as valintalaskentakoostepalvelu-client]
            [schema.core :as s]
            [ataru.applications.harkinnanvaraisuus.harkinnanvaraisuus-util :as hutil]
            [ataru.application.harkinnanvaraisuus-types :refer [harkinnanvaraisuus-types]]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-client :as client]))

(s/defschema HakukohdeValintalaskentaResponse
             {:kayttaaValintalaskentaa s/Bool})

(s/defschema HakukohdeHarkinnanvaraisuusResponse
             {:hakemusOid s/Str
              :henkiloOid s/Str
              :hakutoiveet [{:hakukohdeOid s/Str
                             :harkinnanvaraisuudenSyy (apply s/enum harkinnanvaraisuus-types)}]})

(def ^:private hakukohde-harkinnanvaraisuus-checker (s/checker HakukohdeHarkinnanvaraisuusResponse))

(def ^:private hakukohde-valintalaskenta-checker (s/checker HakukohdeValintalaskentaResponse))

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

(defrecord HakukohdeHarkinnanvaraisuusCacheLoader [valintalaskentakoostepalvelu-cas-client]
  cache/CacheLoader

  (load [_ application]
    (client/hakemusten-harkinnanvaraisuus-valintalaskennasta valintalaskentakoostepalvelu-cas-client
                                                      [(hutil/assoc-harkinnanvaraisuustieto application)]))

  (load-many [_ applications]
    (client/hakemusten-harkinnanvaraisuus-valintalaskennasta valintalaskentakoostepalvelu-cas-client
                                                      (map #(hutil/assoc-harkinnanvaraisuustieto %) applications)))

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
    (valintalaskentakoostepalvelu-client/opiskelijan-suoritukset
      valintalaskentakoostepalvelu-cas-client
      haku-oid
      hakemus-oid))

  (hakemusten-harkinnanvaraisuus-valintalaskennasta [_ hakemus-oids]
    (cache/get-many-from valintalaskentakoostepalvelu-hakukohde-harkinnanvaraisuus-cache hakemus-oids)))

(defn new-valintalaskentakoostepalvelu-service []
  (map->CachedValintalaskentakoostepalveluService {}))
