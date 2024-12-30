(ns ataru.koski.koski-service
  (:require [ataru.koski.koski-client :as koski-client]))

(defprotocol KoskiTutkintoService
  (get-tutkinnot-for-oppija [this oppija-oid]
    "Gets all available tutkinnot for oppija"))

(defrecord IntegratedKoskiTutkintoService [koski-cas-client]
  KoskiTutkintoService
  (get-tutkinnot-for-oppija [_ oppija-oid]
    (try
      (koski-client/get-tutkinnot-for-oppija-oid oppija-oid koski-cas-client)
      (catch Exception exp
        (println (str "error " exp))))))
