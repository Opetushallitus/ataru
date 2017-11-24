(ns ataru.ohjausparametrit.ohjausparametrit-service
  (:require [ataru.config.core :refer [config]]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :refer [OhjausparametritService]]
            [ataru.cache.cache-service :as cache]
            [ataru.ohjausparametrit.mock-ohjausparametrit-service :refer [->MockOhjausparametritService]]
            [ataru.ohjausparametrit.ohjausparametrit-client :as client]))

(defrecord CachedOhjausparametritService [cache-service]
  OhjausparametritService

  (get-parametri [_ haku-oid]
    (cache/cache-get-or-fetch cache-service :ohjausparametrit haku-oid #(client/get-ohjausparametrit haku-oid))))

(defn new-ohjausparametrit-service []
  (if (-> config :dev :fake-dependencies)
    (->MockOhjausparametritService)
    (map->CachedOhjausparametritService {})))
