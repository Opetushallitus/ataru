(ns ataru.virkailija.authentication.virkailija-edit-spec
    (:require [speclj.core :refer :all]
              [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]))

(defmacro with-fake-ldap [& body]
    `(with-redefs [ataru.virkailija.user.ldap-client/get-virkailija-by-username
                   (fn [~'username] {:employeeNumber "1213" :givenName "Hemuli" :sn "Hemuli?"})]
        ~@body))

(describe "virkailija edit"
  (tags :unit :virkailija-edit)
  (it "creates virkailija credentials"
    (let [credentials (with-fake-ldap (virkailija-edit/create-virkailija-credentials {:identity {:name "Hemuli"}} "test-key"))]
      (should== [:application_key :oid :secret :valid :created_time] (keys credentials))
      (should= true (virkailija-edit/virkailija-secret-valid? (:secret credentials)))))

  (it "should invalidate credentials"
    (let [credentials (with-fake-ldap (virkailija-edit/create-virkailija-credentials {:identity {:name "Hemuli"}} "test-key"))]
      (should= true (virkailija-edit/virkailija-secret-valid? (:secret credentials)))
      (virkailija-edit/invalidate-virkailija-credentials (:secret credentials))
      (should= false (virkailija-edit/virkailija-secret-valid? (:secret credentials))))))


