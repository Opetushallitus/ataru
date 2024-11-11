(ns ataru.kk-application-payment.kk-application-payment-module-job
  (:require [ataru.forms.form-store :as form-store]
            [ataru.kk-application-payment.kk-application-payment :as kk-application-payment]
            [ataru.kk-application-payment.utils :refer [has-payment-module?]]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [taoensso.timbre :as log]
            [ataru.component-data.kk-application-payment-module :refer [kk-application-payment-module]]
            [ataru.tarjonta-service.kouta.kouta-client :as kouta-client]))

(defonce payment-module-session {:user-agent "payment-module"})
(def audit-logger (atom nil))

(defn- add-payment-module-to-form
  [form]
  (let [sections (:content form)
        payment-section (kk-application-payment-module)
        updated-content (concat (take-nth 2 sections) [payment-section] (drop 2 sections))
        updated-form (assoc form :content updated-content)]
    (log/info "adding kk-application-payment-module to form " (:key form) " with id " (:id form))
    (form-store/create-form-or-increment-version! form payment-module-session @audit-logger)
    updated-form))

(defn check-need-for-application-payment-module
  [_ {:keys [tarjonta-service cas-client]}]
  (log/info "Check need for application payment module step starting")
  (let [haut (->> (kouta-client/get-haku-oids cas-client)
                  (keep #(tarjonta/get-haku tarjonta-service %))
                  (filter #(some? (:ataru-form-key %)))
                  (kk-application-payment/filter-haut-for-update tarjonta-service))
        forms-updated (->> haut
                           (map :ataru-form-key)
                           (form-store/fetch-latest-version)
                           (filter #(not (has-payment-module? %)))
                           (add-payment-module-to-form)
                           count)]
    (log/info "Check need for application payment module step finishing, amount of haku checked: " (count haut) ", updated " forms-updated)))

(def job-definition {:handler check-need-for-application-payment-module
                     :type    "application-payment-module-check"
                     :schedule "0 3 30 * *"})
