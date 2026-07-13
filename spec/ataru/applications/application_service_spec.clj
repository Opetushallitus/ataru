(ns ataru.applications.application-service-spec
  (:require [ataru.applications.application-service :as application-service]
            [ataru.applications.application-access-control :as aac]
            [ataru.person-service.person-service :as person-service]
            [ataru.applications.application-store :as application-store]
            [speclj.core :refer [describe tags it should-not should== should=]]
  )
)

(def blank-application {
  :key nil
  :form_id nil
  :person-oid nil
  :sukunimi nil
  :etunimet nil
  :henkilotunnus nil
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
})

(defn make-application
  [
    key
    person-oid
  ]
  (assoc blank-application
    :key key
    :person-oid person-oid
  )
)

(defn should-match-application [expected actual]
  (let [
    {
      key :key
      person-oid :person-oid
      sukunimi :sukunimi
      etunimet :etunimet
      henkilotunnus :henkilotunnus
    } expected]
    (should==
      (assoc blank-application
        :key key
        :person-oid person-oid
        :sukunimi sukunimi
        :etunimet etunimet
        :henkilotunnus henkilotunnus
      )
      actual
    )
  )
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
        ] (should-match-application
            {
              :key test-app-key
              :person-oid test-person-uid
              :sukunimi "Ihminen"
              :etunimet "Testi"
              :henkilotunnus "020202A0202"
            }
            (application-service/get-tutu-application service test-app-key)
          )
        )
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
          )
        )
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
        ] (let [results (application-service/get-tutu-applications service [test-app-1-key test-app-2-key])]
            (should-match-application
              {
                :key test-app-1-key
                :person-oid test-person-1-uid
                :sukunimi "Ihminen"
                :etunimet "Testi"
                :henkilotunnus "020202A0202"
              }
              (first results)
            )
            (should-match-application
              {
                :key test-app-2-key
                :person-oid test-person-2-uid
                :sukunimi "Vatanen"
                :etunimet "Ari"
                :henkilotunnus "141196-933S"
              }
              (second results)
            )
          )
        )
      )
    )
  )
)

(describe "Mass update application states"
  (tags :unit)

  (it "returns the actual number of updated application states"
    (let [session {:identity {:oid "1.2.3"}}
          service (application-service/map->CommonApplicationService
                   {:organization-service nil
                    :tarjonta-service     nil
                    :audit-logger         nil})]
      (with-redefs [aac/applications-access-authorized? (constantly true)
                    application-store/mass-update-application-states (fn [passed-session application-keys hakukohde-oids from-state to-state audit-logger]
                                                                       (should= session passed-session)
                                                                       (should= ["application-1" "application-2"] application-keys)
                                                                       (should= ["hakukohde-1"] hakukohde-oids)
                                                                       (should= "unprocessed" from-state)
                                                                       (should= "processing" to-state)
                                                                       (should= nil audit-logger)
                                                                       0)]
        (should=
          0
          (application-service/mass-update-application-states
           service
           session
           ["application-1" "application-2"]
           ["hakukohde-1"]
           "unprocessed"
           "processing"))))))

(describe "save-attachment-hakukohde-reviews"
  (tags :unit)

  (it "syncs a changed payment module attachment review to other hakukohde the attachment applies to, even though the request also carries other hakukohde's unchanged values"
    (let [application-key "app-1"
          saved (atom [])]
      (with-redefs [application-store/get-application-attachment-reviews
                    (constantly [{:hakukohde "hk-1" :attachment-key "passport-attachment" :state "not-checked"}
                                 {:hakukohde "hk-2" :attachment-key "passport-attachment" :state "not-checked"}])
                    application-store/save-attachment-hakukohde-review
                    (fn [_ hakukohde attachment-key state _ _]
                      (swap! saved conj [hakukohde attachment-key state]))]
        (should=
          true
          (#'application-service/save-attachment-hakukohde-reviews
           application-key
           {:hk-1 {:passport-attachment "checked"}
            :hk-2 {:passport-attachment "not-checked"}}
           nil nil))
        (should== #{["hk-1" "passport-attachment" "checked"]
                    ["hk-2" "passport-attachment" "checked"]}
                  (set @saved)))))

  (it "syncs a changed passport-attachment variant key (e.g. eu-passport-attachment) the same way as the base passport-attachment key"
    (let [application-key "app-1"
          saved (atom [])]
      (with-redefs [application-store/get-application-attachment-reviews
                    (constantly [{:hakukohde "hk-1" :attachment-key "eu-passport-attachment" :state "not-checked"}
                                 {:hakukohde "hk-2" :attachment-key "eu-passport-attachment" :state "not-checked"}])
                    application-store/save-attachment-hakukohde-review
                    (fn [_ hakukohde attachment-key state _ _]
                      (swap! saved conj [hakukohde attachment-key state]))]
        (should=
          true
          (#'application-service/save-attachment-hakukohde-reviews
           application-key
           {:hk-1 {:eu-passport-attachment "checked"}
            :hk-2 {:eu-passport-attachment "not-checked"}}
           nil nil))
        (should== #{["hk-1" "eu-passport-attachment" "checked"]
                    ["hk-2" "eu-passport-attachment" "checked"]}
                  (set @saved)))))

  (it "does not sync a non-payment-module attachment review to other hakukohde"
    (let [application-key "app-1"
          saved (atom [])]
      (with-redefs [application-store/get-application-attachment-reviews
                    (constantly [{:hakukohde "hk-1" :attachment-key "other-attachment" :state "not-checked"}
                                 {:hakukohde "hk-2" :attachment-key "other-attachment" :state "not-checked"}])
                    application-store/save-attachment-hakukohde-review
                    (fn [_ hakukohde attachment-key state _ _]
                      (swap! saved conj [hakukohde attachment-key state]))]
        (should=
          false
          (#'application-service/save-attachment-hakukohde-reviews
           application-key
           {:hk-1 {:other-attachment "checked"}
            :hk-2 {:other-attachment "not-checked"}}
           nil nil))
        ; hk-2:n arvo on muuttumaton verrattuna jo tallennettuun, joten sitä ei pidä
        ; tallentaa (uudelleen) - application-store/save-attachment-hakukohde-review avaa
        ; oman tietokantatransaktionsa jokaista kutsua kohden, joten muuttumattomien tilojen
        ; ohittaminen välttää turhat edestakaiset tietokantakutsut
        (should== [["hk-1" "other-attachment" "checked"]] @saved))))

  (it "only syncs to hakukohde the payment module attachment actually applies to, not every hakukohde in the application"
    (let [application-key "app-1"
          saved (atom [])]
      (with-redefs [application-store/get-application-attachment-reviews
                    (constantly [{:hakukohde "hk-1" :attachment-key "passport-attachment" :state "not-checked"}
                                 {:hakukohde "hk-2" :attachment-key "passport-attachment" :state "not-checked"}
                                 {:hakukohde "hk-3" :attachment-key "other-attachment" :state "not-checked"}])
                    application-store/save-attachment-hakukohde-review
                    (fn [_ hakukohde attachment-key state _ _]
                      (swap! saved conj [hakukohde attachment-key state]))]
        ; hk-3 ei ole niiden hakukohteiden joukossa joita liitepyyntö koskee (sillä ei ole
        ; olemassa olevaa tarkastusmerkintäriviä passport-attachmentille), joten sen ei pidä
        ; saada periytynyttä arvoa vaikka se onkin osa samaa hakemusta
        (should=
          true
          (#'application-service/save-attachment-hakukohde-reviews
           application-key
           {:hk-1 {:passport-attachment "checked"}
            :hk-2 {:passport-attachment "not-checked"}}
           nil nil))
        (should== #{["hk-1" "passport-attachment" "checked"]
                    ["hk-2" "passport-attachment" "checked"]}
                  (set @saved))
        (should-not (some #(= "hk-3" (first %)) @saved)))))

  (it "does not sync when the payment module attachment review value is unchanged"
    (let [application-key "app-1"
          saved (atom [])]
      (with-redefs [application-store/get-application-attachment-reviews
                    (constantly [{:hakukohde "hk-1" :attachment-key "passport-attachment" :state "checked"}
                                 {:hakukohde "hk-2" :attachment-key "passport-attachment" :state "checked"}])
                    application-store/save-attachment-hakukohde-review
                    (fn [_ hakukohde attachment-key state _ _]
                      (swap! saved conj [hakukohde attachment-key state]))]
        (should=
          false
          (#'application-service/save-attachment-hakukohde-reviews
           application-key
           {:hk-1 {:passport-attachment "checked"}
            :hk-2 {:passport-attachment "checked"}}
           nil nil))
        (should== [] @saved))))

  (it "does not call save-attachment-hakukohde-review at all when nothing in the request differs from the stored state"
    (let [application-key "app-1"
          save-call-count (atom 0)]
      (with-redefs [application-store/get-application-attachment-reviews
                    (constantly [{:hakukohde "hk-1" :attachment-key "passport-attachment" :state "checked"}
                                 {:hakukohde "hk-2" :attachment-key "passport-attachment" :state "not-checked"}
                                 {:hakukohde "hk-3" :attachment-key "other-attachment" :state "not-checked"}])
                    application-store/save-attachment-hakukohde-review
                    (fn [_ _ _ _ _ _] (swap! save-call-count inc))]
        (should=
          false
          (#'application-service/save-attachment-hakukohde-reviews
           application-key
           {:hk-1 {:passport-attachment "checked"}
            :hk-2 {:passport-attachment "not-checked"}
            :hk-3 {:other-attachment "not-checked"}}
           nil nil))
        (should= 0 @save-call-count)))))
