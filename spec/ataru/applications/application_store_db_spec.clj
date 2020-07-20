(ns ataru.applications.application-store-db-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.log.audit-log :as audit-log]
            [ataru.db.db :as db]
            [ataru.fixtures.application :as fixtures]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.util :as util]
            [clojure.java.jdbc :as jdbc]
            [speclj.core :refer [after describe it should= should== tags]]))

(def ^:private form (atom nil))

(def ^:private test-application-key (atom nil))

(def audit-logger (audit-log/new-dummy-audit-logger))

(defn- find-application-key-by-id [id]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (->> (jdbc/query conn ["select key from applications where id = ?"
                                                   id])
                                 first
                                 :key)))

(defn- delete-application! [key]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (->> (jdbc/delete! conn :application_hakukohde_attachment_reviews ["application_key = ?" key]))
                            (->> (jdbc/delete! conn :applications ["key = ?" key]))))

(defn- save-reviews-to-db! [reviews]
  (let [reviews-with-flag (map #(assoc % :updated? true) reviews)]
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                              (store/store-reviews reviews-with-flag {:connection conn}))))

(defn- reset-database! []
  (let [form-with-attachment (update form-fixtures/person-info-form :content concat form-fixtures/attachment-test-form)]
    (println "Tyhjennetään hakemuksia tietokannasta.")
    (delete-application! @test-application-key)
    (reset! form (unit-test-db/init-db-fixture form-with-attachment))))

(describe "creating application attachment reviews in db"
          (tags :unit :attachments :attachments-db)

          (after
            (reset-database!))

          (it "should find attachments with query"
              (let [application            (-> (filter #(= "attachments" (:key %)) fixtures/applications)
                                               (first)
                                               (assoc :form_id (:id form)))
                    flat-form-content      (util/flatten-form-fields (:content form-fixtures/attachment-test-form))
                    answers-by-key         (-> application :content :answers util/answers-by-key)
                    fields-by-id           (util/form-fields-by-id form-fixtures/attachment-test-form)
                    existing-attachment-id "att__1"
                    application-key        (-> (store/add-application
                                                 (dissoc application :key)
                                                 []
                                                 form-fixtures/attachment-test-form
                                                 {}
                                                 audit-logger)
                                               (find-application-key-by-id))
                    new-reviews            (store/create-application-attachment-reviews
                                             application-key
                                             (store/filter-visible-attachments answers-by-key
                                                                               flat-form-content
                                                                               fields-by-id)
                                             answers-by-key
                                             nil
                                             []
                                             false
                                             fields-by-id
                                             #{})
                    query                  {:attachment-review-states [existing-attachment-id '("not-checked")]}
                    sort                   {:order-by "applicant-name" :order "asc"}]
                (reset! test-application-key application-key)
                (should== [{:application_key application-key
                            :attachment_key  "att__1"
                            :state           "not-checked"
                            :updated?        false
                            :hakukohde       "form"}
                           {:application_key application-key
                            :attachment_key  "att__2"
                            :state           "attachment-missing"
                            :updated?        false
                            :hakukohde       "form"}]
                          new-reviews)
                (save-reviews-to-db! new-reviews)
                (let [queried-applications (vec (store/get-application-heading-list query sort))
                      found-application    (first queried-applications)]
                  (should== 1 (count queried-applications))
                  (should= application-key (:key found-application))))))
