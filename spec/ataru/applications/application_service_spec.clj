(ns ataru.applications.application-service-spec
  (:require [ataru.applications.application-service :as application-service]
            [ataru.person-service.person-service :as person-service]
            [ataru.applications.application-store :as application-store]
            [speclj.core :refer :all]
  )
)

(defn make-application
  [
    key
    person-oid
  ]
  {
    :key key
    :person-oid person-oid
    :haku nil
    :hakutoiveet nil
    :content nil
    :lang nil
    :state nil
    :submitted nil
    :created nil
    :modified nil
    :application-hakukohde-reviews nil
    :application-hakukohde-attachment-reviews nil
    :application-review-notes nil
  }
)

(describe "Services for TUTU"
  (tags :tutu :unit)

  (describe "get-tutu-application"

    (it "returns an application with user data when application is found"
      (let [
        test-app-key "1.2.3"
        test-person-uid "1.2.3.4.5.303"
        service (assoc (application-service/new-application-service) :person-service (person-service/->FakePersonService))
      ] (with-redefs [
        application-store/get-tutu-application (fn [application-key] (make-application application-key test-person-uid))
      ] (should=
          {
            :key test-app-key
            :person-oid test-person-uid
            :sukunimi "Ihminen"
            :etunimet "Testi"
            :henkilotunnus "020202A0202"
            :haku nil
            :hakutoiveet nil
            :content nil
            :lang nil
            :state nil
            :submitted nil
            :created nil
            :modified nil
            :application-hakukohde-reviews nil
            :application-hakukohde-attachment-reviews nil
            :application-review-notes nil
          }
          (application-service/get-tutu-application service test-app-key)
        ))
      )
    )

    (it "returns nil on when application is not found"
      (let [
        test-app-key "1.2.3"
        service (assoc (application-service/new-application-service) :person-service (person-service/->FakePersonService))
      ] (with-redefs [
        application-store/get-tutu-application (fn [_] nil)
      ] (should=
          nil
          (application-service/get-tutu-application service test-app-key)
        ))
      )
    )
  )

  (describe "get-tutu-applications"

    (it "returns a list of applications with user data when applications are found"
      (let [
        test-app-1-key "1.2.3"
        test-app-2-key "1.2.4"
        test-person-1-uid "1.2.3.4.5.303"
        test-person-2-uid "2.2.2"
        service (assoc (application-service/new-application-service) :person-service (person-service/->FakePersonService))
      ] (with-redefs [
        application-store/get-tutu-applications (fn [_] [
          (make-application test-app-1-key test-person-1-uid)
          (make-application test-app-2-key test-person-2-uid)
        ])
      ] (should=
          [{
            :key test-app-1-key
            :person-oid test-person-1-uid
            :sukunimi "Ihminen"
            :etunimet "Testi"
            :henkilotunnus "020202A0202"
            :haku nil
            :hakutoiveet nil
            :content nil
            :lang nil
            :state nil
            :submitted nil
            :created nil
            :modified nil
            :application-hakukohde-reviews nil
            :application-hakukohde-attachment-reviews nil
            :application-review-notes nil
          } {
            :key test-app-2-key
            :person-oid test-person-2-uid
            :sukunimi "Vatanen"
            :etunimet "Ari"
            :henkilotunnus "141196-933S"
            :haku nil
            :hakutoiveet nil
            :content nil
            :lang nil
            :state nil
            :submitted nil
            :created nil
            :modified nil
            :application-hakukohde-reviews nil
            :application-hakukohde-attachment-reviews nil
            :application-review-notes nil
          }]
          (application-service/get-tutu-applications service [test-app-1-key test-app-2-key])
        ))
      )
    )
  )
)
