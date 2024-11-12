(ns ataru.kk-application-payment.kk-application-payment-module-job-spec
  (:require [ataru.forms.form-store :as form-store]
            [speclj.core :refer [it describe tags should= after]]
            [clojure.java.jdbc :as jdbc]
            [ataru.log.audit-log :as audit-log]
            [ataru.db.db :as db]
            [ataru.kk-application-payment.kk-application-payment-module-job :as payment-module-job]
            [ataru.component-data.kk-application-payment-module :refer [kk-application-payment-wrapper-key]]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]))

(def haku-key "payment-info-test-kk-haku-custom-form")
(def form-key tarjonta-service/custom-form-key)

(def audit-logger (audit-log/new-dummy-audit-logger))

(def ts (tarjonta-service/->MockTarjontaKoutaService))

(defn- clean! []
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :forms
                                          ["key = ?" form-key])))

(defn- init []
  (let [content (filter #(not= (:id %) kk-application-payment-wrapper-key) (:content form-fixtures/person-info-form))
        form (assoc form-fixtures/person-info-form :content content)]
    (form-store/create-new-form! form form-key)))

(describe "kk-application-payment-module-job"
          (tags :unit :database)

          (after
            (clean!))

          (it "inserts payment module to form"
              (init)
              (payment-module-job/check-and-update ts [haku-key])
              (should= true (->> (form-store/fetch-by-key form-key)
                                 :content
                                 (some #(= (:id %) kk-application-payment-wrapper-key))
                                 boolean))))




