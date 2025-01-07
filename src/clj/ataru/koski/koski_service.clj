(ns ataru.koski.koski-service
  (:require   [taoensso.timbre :as log]
              [ataru.koski.koski-client :as koski-client]))

(defprotocol KoskiTutkintoService
  (get-tutkinnot-for-oppija [this throw-errors oppija-oid]
    "Gets all available tutkinnot for oppija"))

(defrecord IntegratedKoskiTutkintoService [koski-cas-client]
  KoskiTutkintoService
  (get-tutkinnot-for-oppija [_ throw-errors oppija-oid]
    (try
      (koski-client/get-tutkinnot-for-oppija-oid oppija-oid koski-cas-client)
      (catch Exception exp
        (log/error exp "Failed to fetch tutkinnot from koski")
        (when throw-errors
          (throw exp))))))
