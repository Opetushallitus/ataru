(ns ataru.ohjausparametrit.ohjausparametrit-service
  (:require [ataru.config.core :refer [config]]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :refer [OhjausparametritService]]
            [ataru.cache.cache-service :as cache]
            [ataru.ohjausparametrit.mock-ohjausparametrit-service :refer [->MockOhjausparametritService]]))

(defrecord CachedOhjausparametritService [ohjausparametrit-cache]
  OhjausparametritService

  (get-parametri [_ haku-oid]
    (cache/get-from ohjausparametrit-cache haku-oid)))

(defn new-ohjausparametrit-service []
  (if (-> config :dev :fake-dependencies)
    (->MockOhjausparametritService)
    (map->CachedOhjausparametritService {})))
