(ns ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-service
  (:require [ataru.cache.cache-service :as cache]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol :refer [ValintalaskentakoostepalveluService]]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-client :as valintalaskentakoostepalvelu-client]))

(defrecord CachedValintalaskentakoostepalveluService [valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache
                                                      valintalaskentakoostepalvelu-cas-client]
  ValintalaskentakoostepalveluService

  (hakukohde-uses-valintalaskenta? [this hakukohde-oid]
    (cache/get-from valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache
                    hakukohde-oid))

  (opiskelijan-suoritukset [this haku-oid hakemus-oid]
    (valintalaskentakoostepalvelu-client/opiskelijan-suoritukset
      valintalaskentakoostepalvelu-cas-client
      haku-oid
      hakemus-oid))

  (hakemusten-harkinnanvaraisuus-valintalaskennasta [this hakemukset-with-harkinnanvaraisuus])
  )

(defn new-valintalaskentakoostepalvelu-service []
  (map->CachedValintalaskentakoostepalveluService {}))
