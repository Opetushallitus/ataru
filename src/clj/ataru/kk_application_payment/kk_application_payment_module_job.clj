(ns ataru.kk-application-payment.kk-application-payment-module-job
  (:require [ataru.forms.form-store :as form-store]
            [ataru.kk-application-payment.kk-application-payment :as kk-application-payment]
            [ataru.kk-application-payment.utils :refer [has-payment-module? inject-payment-module-to-form]]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [taoensso.timbre :as log]
            [ataru.config.core :refer [config]]
            [ataru.log.audit-log :refer [new-dummy-audit-logger]]
            [clojure.set :as set]))

(defonce payment-module-session {:user-agent "payment-module"})

(defn- add-payment-module-to-form
  [form]
  (let [updated-form (inject-payment-module-to-form form)]
    (log/info "adding kk-application-payment-module to form " (:key form) " with id " (:id form))
    (form-store/create-form-or-increment-version! updated-form payment-module-session (new-dummy-audit-logger))
    updated-form))

(defn check-and-update
  [tarjonta-service haku-oids]
  (let [existing-haut     (keep #(tarjonta/get-haku tarjonta-service %) haku-oids)
        non-existing-oids (set/difference (set haku-oids) (set (map :oid existing-haut)))
        maksulliset (filter #(:maksullinen-kk-haku? %) existing-haut)
        haut (->> existing-haut
                  (filter #(some? (:ataru-form-key %)))
                  (kk-application-payment/filter-haut-for-update)
                  seq)
        forms (->> haut
                   (map :ataru-form-key)
                   (map form-store/fetch-by-key-for-kk-payment-module-job))]
    (log/info "Found: " (count haku-oids)
              ", special (1.2.246.562.29.00000000000000070336) detected: " (some? (some #{"1.2.246.562.29.00000000000000070336"} haku-oids))
              ", special (1.2.246.562.29.00000000000000070336) contents: " (some #(when (= "1.2.246.562.29.00000000000000070336" (:oid %)) %) existing-haut)
              ", non-existing: " non-existing-oids
              ", maksulliset: " (map :oid maksulliset)
              ", maksulliset with formkey: " (map :oid (filter #(some? (:ataru-form-key %)) maksulliset))
              ", matching: " (map :oid haut)
              ", without paymentmodule: " (map :id (filter #(not (or (nil? %) (has-payment-module? %))) forms)))

    (->> forms
         (filter #(not (or (nil? %) (has-payment-module? %))))
         (map add-payment-module-to-form)
         count)))

(defn check-need-for-application-payment-module
  [_ {:keys [tarjonta-service]}]
  (log/info "Check need for application payment module step starting")
  (let [haku-oids (tarjonta/get-haku-oids tarjonta-service)
        forms-updated (check-and-update tarjonta-service haku-oids)]
    (log/info "Check need for application payment module step finishing, amount of haku checked: " (count haku-oids) ", updated " forms-updated)))

(def job-definition {:handler check-need-for-application-payment-module
                     :type    "application-payment-module-check"
                     :schedule (get-in config [:jobs :application-payment-module-cron] "0 4 * * *")})

