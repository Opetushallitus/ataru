(ns ataru.kk-application-payment.kk-application-payment-module-job-spec
  (:require [ataru.forms.form-store :as form-store]
            [speclj.core :refer [it describe tags should= after]]
            [clojure.java.jdbc :as jdbc]
            [ataru.log.audit-log :as audit-log]
            [ataru.db.db :as db]
            [ataru.kk-application-payment.kk-application-payment-module-job :as payment-module-job]
            [ataru.component-data.kk-application-payment-module :refer [kk-application-payment-wrapper-key]]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.component-data.person-info-module :refer [person-info-module-keys]]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]))

(def haku-key "payment-info-test-kk-haku-custom-form")
(def non-kk-haku-key "payment-info-test-non-kk-haku-custom-form")
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

          (it "inserts payment module to form for applicable haku"
              (init)
              (payment-module-job/check-and-update ts [haku-key])
              (let [form  (form-store/fetch-by-key form-key)]
                (should= 6 (count (:content form)))
                (should= (:onr-kk-application-payment person-info-module-keys)
                         (->> form
                              :content
                              (filter #(= (:module %) "person-info"))
                              first
                              :id))
                (should= true (->> form
                                 :content
                                 (some #(= (:id %) kk-application-payment-wrapper-key))
                                 boolean))))

          (it "does not insert payment module to form as haku does not need it"
              (init)
              (payment-module-job/check-and-update ts [non-kk-haku-key])
              (let [form  (form-store/fetch-by-key form-key)]
                (should= 5 (count (:content form)))
                (should= false (->> form
                                   :content
                                   (some #(= (:id %) kk-application-payment-wrapper-key))
                                   boolean))))

          (it "does not reinsert payment module to form"
              (init)
              (payment-module-job/check-and-update ts [haku-key])
              (payment-module-job/check-and-update ts [haku-key])
              (let [form  (form-store/fetch-by-key form-key)]
                (should= 6 (count (:content form)))
                (should= (:onr-kk-application-payment person-info-module-keys)
                         (->> form
                              :content
                              (filter #(= (:module %) "person-info"))
                              first
                              :id))
                (should= true (->> form
                                    :content
                                    (some #(= (:id %) kk-application-payment-wrapper-key))
                                    boolean)))))




