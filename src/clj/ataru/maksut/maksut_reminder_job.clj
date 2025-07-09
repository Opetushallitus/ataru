(ns ataru.maksut.maksut-reminder-job
  (:require [ataru.applications.application-store :as application-store]
            [ataru.background-job.email-job :as email-job]
            [ataru.background-job.job :as job]
            [ataru.config.url-helper :as url-helper]
            [ataru.db.db :as db]
            [ataru.config.core :refer [config]]
            [ataru.email.application-email :as application-email]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            [ataru.maksut.maksut-store :as maksut-store]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(declare connection)

(defn- handle-reminder [reminder {:keys [maksut-service] :as job-runner}]
  (log/info "Handling reminder for information request" (:id reminder))
  (let [lasku (maksut-protocol/list-laskut-by-application-key maksut-service (:application-key reminder))]
    (if (= :active (:status lasku))
      (let [lang (:lang reminder)
            metadata (:metadata lasku)
            email (application-email/create-decision-email
                    {:origin (:origin lasku)
                     :message (:message reminder)
                     :form-name (get-in metadata [:form-name (keyword lang)])
                     :payment-url (url-helper/resolve-url :maksut-service.hakija-get-by-secret (:secret lasku) lang)
                     :amount (:amount lasku)
                     :vat (:vat lasku)
                     :due-date (->> (str/split (:due_date lasku) #"-")
                                    (reverse)
                                    (str/join \.))
                     :order-id-prefix (:order-id-prefix metadata)})]
        (jdbc/with-db-transaction
          [connection {:datasource (db/get-datasource :db)}]
          (job/start-job job-runner
                         connection
                         (:type email-job/job-definition)
                         email)
          (application-store/add-application-event-in-tx
            connection
            {:application-key (:application-key reminder)
             :event-type      "payment-reminder-sent"
             :review-key      (str (:order_id lasku))}
            nil)
          (maksut-store/set-reminder-handled-in-tx connection (:id reminder) :sent)))
      (maksut-store/set-reminder-handled (:id reminder) (:status lasku)))))

(defn handler [_ job-runner]
  (let [payment-reminders (maksut-store/get-payment-reminders)]
    (doseq [reminder payment-reminders]
      (handle-reminder reminder job-runner))))

(def job-cron (get-in config [:public-config :payment-reminder-cron]))

(def job-definition {:handler  handler
                     :type     (-> *ns* ns-name str)
                     :schedule job-cron})
