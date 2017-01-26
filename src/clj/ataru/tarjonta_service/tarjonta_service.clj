(ns ataru.tarjonta-service.tarjonta-service
  (:require
    [ataru.tarjonta-service.tarjonta-client :as client]
    [ataru.virkailija.user.organization-client :refer [oph-organization]]
    [com.stuartsierra.component :as component]))

(defn forms-in-use
  [organization-service username]
  (let [direct-organizations     (.get-direct-organizations organization-service username)
        all-organization-oids    (map :oid (.get-all-organizations organization-service direct-organizations))
        in-oph-organization?     (some #{oph-organization} all-organization-oids)]
    (reduce (fn [acc1 {:keys [avain haut]}]
              (assoc acc1 avain
                          (reduce (fn [acc2 haku]
                                    (assoc acc2 (:oid haku)
                                                {:haku-oid  (:oid haku)
                                                 :haku-name (get-in haku [:nimi :kieli_fi])}))
                                  {} haut)))
            {}
            (client/get-forms-in-use (if in-oph-organization? nil all-organization-oids)))))

(defprotocol TarjontaService
  (get-hakukohde [this hakukohde-oid])
  (get-haku [this haku-oid]))

(defrecord CachedTarjontaService []
  component/Lifecycle
  TarjontaService

  (start [this] this)
  (stop [this] this)

  (get-hakukohde [this hakukohde-oid]
    (.cache-get-or-fetch (:cache-service this) :hakukohde hakukohde-oid #(client/get-hakukohde hakukohde-oid)))

  (get-haku [this haku-oid]
    (.cache-get-or-fetch (:cache-service this) :haku haku-oid #(client/get-haku haku-oid))))

(defn new-tarjonta-service
  []
  (->CachedTarjontaService))

(defprotocol VirkailijaTarjontaService
  (get-forms-in-use [this username]))

(defrecord VirkailijaTarjontaFormsService []
  component/Lifecycle
  VirkailijaTarjontaService

  (start [this] this)
  (stop [this] this)

  (get-forms-in-use [this username]
    (forms-in-use (:organization-service this) username)))

(defn new-virkailija-tarjonta-service
  []
  (->VirkailijaTarjontaFormsService))


