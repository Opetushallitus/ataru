(ns ataru.virkailija.authentication.virkailija-edit-spec
  (:require [speclj.core :refer :all]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
            [yesql.core :as sql]
            [ataru.db.db :as db]))

(sql/defqueries "sql/virkailija-queries.sql")

(describe "virkailija edit"
  (tags :unit :virkailija-edit)

  ; We can do this here, since in reality we can assume that auth/login has been made
  ; for every logged in user, which upserts virkailija.
  (before (db/exec :db yesql-upsert-virkailija<! {:oid        "1213"
                                                  :first_name "Hemuli"
                                                  :last_name  "Hemuli?"}))

  (it "creates virkailija credentials"
    (let [credentials (virkailija-edit/create-virkailija-credentials {:identity {:oid        "1213"
                                                                                 :username   "hhemuli"
                                                                                 :first-name "Hemuli"
                                                                                 :last-name  "Hemuli?"}} "test-key")]
      (should== [:application_key :oid :secret :valid :created_time] (keys credentials))
      (should= true (virkailija-edit/virkailija-secret-valid? (:secret credentials)))))

  (it "should invalidate credentials"
    (let [credentials (virkailija-edit/create-virkailija-credentials {:identity {:oid        "1213"
                                                                                 :username   "hhemuli"
                                                                                 :first-name "Hemuli"
                                                                                 :last-name  "Hemuli?"}} "test-key")]
      (should= true (virkailija-edit/virkailija-secret-valid? (:secret credentials)))
      (virkailija-edit/invalidate-virkailija-credentials (:secret credentials))
      (should= false (virkailija-edit/virkailija-secret-valid? (:secret credentials))))))


