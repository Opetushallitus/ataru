(ns ataru.kk-application-payment.kk-application-payment-module-job
  (:require [ataru.forms.form-store :as form-store]
            [ataru.kk-application-payment.kk-application-payment :as kk-application-payment]
            [ataru.kk-application-payment.utils :refer [has-payment-module? inject-payment-module-to-form]]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [taoensso.timbre :as log]
            [ataru.config.core :refer [config]]
            [ataru.log.audit-log :refer [new-dummy-audit-logger]]))

(defonce payment-module-session {:user-agent "payment-module"})

(defn- add-payment-module-to-form
  [form]
  (let [updated-form (inject-payment-module-to-form form)]
    (log/info "adding kk-application-payment-module to form " (:key form) " with id " (:id form))
    (form-store/create-form-or-increment-version! updated-form payment-module-session (new-dummy-audit-logger))
    updated-form))

(defn check-and-update
  [tarjonta-service haku-oids]
  (let [haut (->> haku-oids
                  (keep #(tarjonta/get-haku tarjonta-service %))
                  (filter #(some? (:ataru-form-key %)))
                  (kk-application-payment/filter-haut-for-update tarjonta-service)
                  seq)]
    (->> haut
         (map :ataru-form-key)
         (map form-store/fetch-by-key)
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

