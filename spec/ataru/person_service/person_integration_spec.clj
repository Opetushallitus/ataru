(ns ataru.person-service.person-integration-spec
  (:require [ataru.person-service.person-integration :as person-integration]
            [ataru.person-service.person-service :as person-service]
            [ataru.applications.application-store :as application-store]
            [speclj.core :refer
             [around describe it tags should= with-stubs stub should-have-invoked should-not-have-invoked]]))

(def fake-person-service (person-service/->FakePersonService))

(def test-application-ids
  {:normal "111111"
   :matched-person "122221"
   :conflicting-person "133331"})

(def test-person-oids 
  {:normal "1.2.3.4.5.6"
   :matched-person "2.3.4.5.6.7"
   :conflicting-person "3.4.5.6.7.8"})

(def application-key "9d24af7d-f672-4c0e-870f-3c6999f105e1")

(defn test-application-with-id [id]
  {:id id :key application-key})

(describe "Person integration"
          (tags :unit :person-integration)
          (with-stubs)

          (around [spec]
                  (with-redefs [person-integration/muu-person-info-module?    (stub :muu-person-info
                                                                                    {:return false})
                                application-store/add-application-event       (stub :add-application-event)
                                application-store/add-person-oid              (stub :add-person-oid)
                                person-integration/start-jobs-for-person      (stub :start-jobs-for-person)
                                person-integration/start-jobs-for-kk-application-payments (stub :start-jobs-for-application)]
                    (spec)))

          (it "upserts a person and updates application but does not add an event for a normal created person"
              (with-redefs [application-store/get-application (stub :get-application
                                                                    {:return (test-application-with-id (:normal test-application-ids))})]
                (should= "1.2.3.4.5.6"
                         (person-integration/upsert-person {:application-id (:normal test-application-ids)} {:person-service fake-person-service}))
                (should-have-invoked :start-jobs-for-person {:with [{:person-service fake-person-service} (:normal test-person-oids)]})
                (should-have-invoked :start-jobs-for-application {:with [{:person-service fake-person-service} (:normal test-application-ids)]})
                (should-have-invoked :add-person-oid {:with [(:normal test-application-ids) (:normal test-person-oids)]})
                (should-not-have-invoked :add-application-event)))

          (it "upserts a person, updates application and adds an event for person with matching email, date of birth and gender"
              (with-redefs [application-store/get-application (stub :get-application
                                                                    {:return (test-application-with-id (:matched-person test-application-ids))})]
                (should= "2.3.4.5.6.7"
                         (person-integration/upsert-person {:application-id (:matched-person test-application-ids)} {:person-service fake-person-service}))
                (should-have-invoked :start-jobs-for-person {:with [{:person-service fake-person-service} (:matched-person test-person-oids)]})
                (should-have-invoked :start-jobs-for-application {:with [{:person-service fake-person-service} (:matched-person test-application-ids)]})
                (should-have-invoked :add-person-oid {:with [(:matched-person test-application-ids) (:matched-person test-person-oids)]})
                (should-have-invoked :add-application-event {:with [{:application-key application-key :event-type "person-found-matching"} nil]})))

          (it "upserts a person, updates application and adds an event for person with matching email, but conflicting date of birth or gender"
              (with-redefs [application-store/get-application (stub :get-application
                                                                    {:return (test-application-with-id (:conflicting-person test-application-ids))})]
                (should= "3.4.5.6.7.8"
                         (person-integration/upsert-person {:application-id (:conflicting-person test-application-ids)} {:person-service fake-person-service}))
                (should-have-invoked :start-jobs-for-person {:with [{:person-service fake-person-service} (:conflicting-person test-person-oids)]})
                (should-have-invoked :start-jobs-for-application {:with [{:person-service fake-person-service} (:conflicting-person test-application-ids)]})
                (should-have-invoked :add-person-oid {:with [(:conflicting-person test-application-ids) (:conflicting-person test-person-oids)]})
                (should-have-invoked :add-application-event {:with [{:application-key application-key :event-type "person-dob-or-gender-conflict"} nil]}))))
