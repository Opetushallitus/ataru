(ns ataru.valintaperusteet.service
  (:require [ataru.cache.cache-service :as cache]))

(defprotocol ValintaperusteetService
  (valintatapajono [this oid]))

(defrecord CachedValintaperusteetService [valintatapajono-cache]
  ValintaperusteetService

  (valintatapajono [_ oid]
    (cache/get-from valintatapajono-cache oid)))
