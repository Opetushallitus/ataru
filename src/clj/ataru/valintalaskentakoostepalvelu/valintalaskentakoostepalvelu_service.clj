(ns ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-service
  (:require [ataru.cache.cache-service :as cache]
            [ataru.valintalaskentakoostepalvelu.valintalaskentakoostepalvelu-protocol :refer [ValintalaskentakoostepalveluService]]))

(defrecord CachedValintalaskentakoostepalveluService [valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache]
  ValintalaskentakoostepalveluService

  (hakukohde-uses-valintalaskenta? [this hakukohde-oid]
    (cache/get-from valintalaskentakoostepalvelu-hakukohde-valintalaskenta-cache
                    hakukohde-oid)))

(defn new-valintalaskentakoostepalvelu-service []
  (map->CachedValintalaskentakoostepalveluService {}))
