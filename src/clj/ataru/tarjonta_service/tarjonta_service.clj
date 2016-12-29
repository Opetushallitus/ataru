(ns ataru.tarjonta-service.tarjonta-service
  (:require
    [ataru.tarjonta-service.tarjonta-client :as client]
    [ataru.virkailija.user.organization-client :refer [oph-organization]]))

(defn get-forms-in-use
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
