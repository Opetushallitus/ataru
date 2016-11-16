(ns ataru.tarjonta-service.tarjonta-service
  (:require
    [ataru.tarjonta-service.tarjonta-client :as client]))

(defn get-forms-in-use
  []
  (reduce (fn [acc1 {:keys [avain haut]}]
            (assoc acc1 avain
                        (reduce (fn [acc2 haku]
                                  (assoc acc2 (:oid haku)
                                              {:haku-oid  (:oid haku)
                                               :haku-name (get-in haku [:nimi :kieli_fi])}))
                                {} haut)))
          {}
          (client/get-forms-in-use)))