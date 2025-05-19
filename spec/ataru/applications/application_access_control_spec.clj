(ns ataru.applications.application-access-control-spec
  (:require [speclj.core :refer [describe tags it should should-not should-be should-contain should=]]
            [ataru.applications.application-access-control :as aac]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]
            [ataru.suoritus.suoritus-service :as suoritus-service]
            [ataru.applications.application-store :as application-store]
            [ataru.person-service.person-service :as person-service]))

; These have to be supported by FakeOrganizationService
(def organization-oid-1 "1.2.246.562.10.11")
(def organization-oid-2 "1.2.246.562.10.1234334543")
(def organization-oid-3 "1.2.246.562.10.22")

; These have to be supported by MockTarjontaService
(def hakukohde-oid "hakukohde.oid")
(def hakukohteen-tarjoaja-oid "1.2.246.562.10.10826252479")

(defn- oppilaitoksen-opiskelijat
  [oppilaitos-oid vuosi]
  (let [alkupvm   (str vuosi "-01-01T00:00:00.000Z")
        loppupvm1 (str vuosi "-05-31T00:00:00.000Z")
        loppupvm2 (str vuosi "-12-31T00:00:00.000Z")]
  (condp = oppilaitos-oid
    organization-oid-1
    [{:person-oid "opiskelija-1-oid"
      :luokka     "9C"
      :alkupaiva  alkupvm
      :loppupaiva loppupvm1}
     {:person-oid "opiskelija-2-oid"
      :luokka     "9D"
      :alkupaiva  alkupvm
      :loppupaiva loppupvm2}]
    organization-oid-2
    [{:person-oid (str "opiskelija-3-oid-" vuosi)
      :luokka     "9E"
      :alkupaiva  alkupvm
      :loppupaiva loppupvm1}
     {:person-oid (str "opiskelija-4-oid-" vuosi)
      :luokka     "9F"
      :alkupaiva  alkupvm
      :loppupaiva loppupvm2}]
    [])))

(defrecord FakeSuoritusService []
  suoritus-service/SuoritusService

  (ylioppilas-ja-ammatilliset-suoritukset-modified-since
    [this modified-since]
    nil)

  (ylioppilas-tai-ammatillinen?
    [this person-oid]
    nil)

  (oppilaitoksen-opiskelijat
    [this oppilaitos-oid vuosi luokkatasot]
    (oppilaitoksen-opiskelijat oppilaitos-oid vuosi))

  (oppilaitoksen-opiskelijat-useammalle-vuodelle [this oppilaitos-oid vuodet luokkatasot]
    (mapcat #(oppilaitoksen-opiskelijat oppilaitos-oid %) vuodet))


  (oppilaitoksen-luokat
    [this oppilaitos-oid vuosi luokkatasot]
    []))

(def organization-service (organization-service/->FakeOrganizationService))
(def tarjonta-service (tarjonta-service/->MockTarjontaService))
(def suoritus-service (->FakeSuoritusService))
(def person-service (person-service/->FakePersonService))

(defn- session-with-rights
  [& args]
  (let [user-right-organizations
        (->> args
          (partition 2)
          (map (fn [[right organization-oids]]
                 {right (mapv (fn [oid] {:oid oid}) organization-oids)}))
          (apply merge))]
    {:identity {:user-right-organizations user-right-organizations}}))

(describe "organization-oid-authorized-by-session-pred"
  (tags :unit)

  (describe "user is ordinary user with organizations"
    (let [session (session-with-rights
                    :opinto-ohjaaja [organization-oid-1]
                    :view-applications [organization-oid-2]
                    :edit-applications [organization-oid-3])
          pred    (aac/organization-oid-authorized-by-session-pred organization-service session)]
      (it "returns false for nil"
        (should-not (pred nil)))
      (it "returns false for organization with opinto-ohjaaja rights"
        (should-not (pred organization-oid-1)))
      (it "returns true for organizations with view or edit rights"
        (should (pred organization-oid-2))
        (should (pred organization-oid-3))))))

(describe "filter-authorized-by-session"
  (tags :unit)

  (it "does not return application if user has no rights"
    (let [session      {}
          applications [{:organization-oid organization-oid-1}]
          result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service person-service session applications)]
      (should-be empty? result)))

  (it "does not return application if user has authorization for different organization"
    (let [session      (session-with-rights :view-applications [organization-oid-1])
          applications [{:oid "application-oid" :hakukohde [hakukohde-oid]}]
          result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service person-service session applications)]
      (should-be empty? (map :oid result))))

  (it "returns application if it includes hakukohde with authorized tarjoaja"
    (let [session      (session-with-rights :view-applications [hakukohteen-tarjoaja-oid])
          applications [{:oid "application-oid" :hakukohde [hakukohde-oid]}]
          result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service person-service session applications)]
      (should-contain "application-oid" (map :oid result))))

  (it "returns application if its form was created by authorized organization"
    (let [session      (session-with-rights :view-applications [organization-oid-1])
          applications [{:organization-oid organization-oid-1 :oid "application-oid" :hakukohde [hakukohde-oid]}]
          result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service person-service session applications)]
      (should-contain "application-oid" (map :oid result))))

  (it "returns application if user has opinto-ohjaaja authorization to applicant's lahtokoulu"
    (let [session      (session-with-rights :opinto-ohjaaja [organization-oid-1])
          applications [{:person-oid "opiskelija-1-oid" :oid "application-oid" :hakukohde [hakukohde-oid]}]
          result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service person-service session applications)]
      (should-contain "application-oid" (map :oid result))))

  (it "does not duplicate application if authorization is given by both tarjoaja and lahtokoulu"
    (let [session      (session-with-rights
                         :opinto-ohjaaja [organization-oid-1]
                         :view-applications [hakukohteen-tarjoaja-oid])
          applications [{:person-oid "opiskelija-1-oid" :oid "application-1-oid" :hakukohde [hakukohde-oid]}]
          result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service person-service session applications)]
      (should= 1 (count result))
      (should-contain "application-1-oid" (map :oid result))))
  (it "returns application if user has opinto-ohjaaja authorization to lahtokoulu and period matches in jatkuva-haku"
    (let [session      (session-with-rights :opinto-ohjaaja [organization-oid-2])
          applications [{:created-time "2024-02-20T00:00:00.000Z" :oid "application-1-oid" :person-oid "opiskelija-3-oid-2024" :haku "jatkuva-haku"}
                        {:created-time "2024-02-20T00:00:00.000Z" :oid "application-2-oid" :person-oid "opiskelija-4-oid-2024" :haku "jatkuva-haku"}]
          result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service person-service session applications)]
      (should= 1 (count result))
      (should-contain "application-1-oid" (map :oid result))))
  (it "returns application if user has opinto-ohjaaja authorization to lahtokoulu and period matches in jatkuva-haku, application made in next year january"
      (let [session      (session-with-rights :opinto-ohjaaja [organization-oid-2])
            applications [{:created-time "2025-01-20T00:00:00.000Z" :oid "application-1-oid" :person-oid "opiskelija-4-oid-2024" :haku "jatkuva-haku"}
                          {:created-time "2025-01-20T00:00:00.000Z" :oid "application-2-oid" :person-oid "opiskelija-3-oid-2024" :haku "jatkuva-haku"}]
            result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service person-service session applications)]
        (should= 1 (count result))
        (should-contain "application-1-oid" (map :oid result)))))

(describe "application-edit-authorized?"
  (tags :unit)

  (it "returns true if user has edit-applications authorization for application's form's organization"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:organization-oid organization-oid-1 :person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :edit-applications [organization-oid-1])
            result  (aac/application-edit-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= true result))))

  (it "returns false if user has view-applications authorization for application's form's organization"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:organization-oid organization-oid-1 :person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :view-applications [organization-oid-1])
            result  (aac/application-edit-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= false result))))

  (it "returns true if user has edit-applications authorization for application's hakukohde's organization"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:organization-oid organization-oid-1 :person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :edit-applications [hakukohteen-tarjoaja-oid])
            result  (aac/application-edit-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= true result))))

  (it "returns false if user has view-applications authorization for application's hakukohde's organization"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:organization-oid organization-oid-1 :person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :view-applications [hakukohteen-tarjoaja-oid])
            result  (aac/application-edit-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= false result))))

  (it "returns false if user has edit-applications authorization for unrelated organization"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:organization-oid organization-oid-1 :person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :edit-applications [organization-oid-2])
            result  (aac/application-edit-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= false result))))

  (it "returns true if user is opinto-ohjaaja for applicant"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :opinto-ohjaaja [organization-oid-1])
            result  (aac/application-edit-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= true result))))

  (it "returns false if user is opinto-ohjaaja for another school"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :opinto-ohjaaja [organization-oid-2])
            result  (aac/application-edit-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= false result))))

  (it "returns true if user is superuser"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session {:identity {:superuser true}}
            result  (aac/application-edit-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= true result)))))

(describe "application-view-authorized?"
  (tags :unit)

  (it "returns true if user has edit-applications authorization for application's form's organization"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:organization-oid organization-oid-1 :person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :edit-applications [organization-oid-1])
            result  (aac/application-view-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= true result))))

  (it "returns true if user has view-applications authorization for application's form's organization"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:organization-oid organization-oid-1 :person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :view-applications [organization-oid-1])
            result  (aac/application-view-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= true result))))

  (it "returns true if user has edit-applications authorization for application's hakukohde's organization"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:organization-oid organization-oid-1 :person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :edit-applications [hakukohteen-tarjoaja-oid])
            result  (aac/application-view-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= true result))))

  (it "returns true if user has view-applications authorization for application's hakukohde's organization"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:organization-oid organization-oid-1 :person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :view-applications [hakukohteen-tarjoaja-oid])
            result  (aac/application-view-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= true result))))

  (it "returns false if user has edit-applications authorization for unrelated organization"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:organization-oid organization-oid-1 :person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :edit-applications [organization-oid-2])
            result  (aac/application-view-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= false result))))

  (it "returns false if user has view-applications authorization for unrelated organization"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:organization-oid organization-oid-1 :person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :view-applications [organization-oid-2])
            result  (aac/application-view-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= false result))))

  (it "returns true if user is opinto-ohjaaja for applicant"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :opinto-ohjaaja [organization-oid-1])
            result  (aac/application-view-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= true result))))

  (it "returns false if user is opinto-ohjaaja for another school"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session (session-with-rights :opinto-ohjaaja [organization-oid-2])
            result  (aac/application-view-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= false result))))

  (it "returns true if user is superuser"
    (with-redefs [application-store/applications-authorization-data
                  (fn [_]
                    [{:person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}])]
      (let [session {:identity {:superuser true}}
            result  (aac/application-view-authorized? organization-service tarjonta-service suoritus-service person-service session "application-1-oid")]
        (should= true result)))))

(describe "applications-access-authorized-including-opinto-ohjaaja?"
  (describe "when given two organizations"
    (it "returns true if user has correct authorization for both applications' forms' organizations"
      (with-redefs [application-store/applications-authorization-data
                    (fn [_]
                      [{:organization-oid organization-oid-1 :person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}
                       {:organization-oid organization-oid-2 :person-oid "opiskelija-2-oid" :hakukohde [hakukohde-oid]}])]
        (let [session (session-with-rights :view-applications [organization-oid-1 organization-oid-2])
              result  (aac/applications-access-authorized-including-opinto-ohjaaja?
                        organization-service
                        tarjonta-service
                        suoritus-service
                        person-service
                        session
                        ["application-1-oid" "application-2-oid"]
                        [:view-applications])]
          (should= true result))))

    (it "returns false if user has correct authorization only for one application's form's organization"
      (with-redefs [application-store/applications-authorization-data
                    (fn [_]
                      [{:organization-oid organization-oid-1 :person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}
                       {:organization-oid organization-oid-2 :person-oid "opiskelija-2-oid" :hakukohde [hakukohde-oid]}])]
        (let [session (session-with-rights :view-applications [organization-oid-1])
              result  (aac/applications-access-authorized-including-opinto-ohjaaja?
                        organization-service
                        tarjonta-service
                        suoritus-service
                        person-service
                        session
                        ["application-1-oid" "application-2-oid"]
                        [:view-applications])]
          (should= false result))))

    (it "returns true if user has opinto-ohjaaja for both applications' applicants"
      (with-redefs [application-store/applications-authorization-data
                    (fn [_]
                      [{:person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}
                       {:person-oid "opiskelija-2-oid" :hakukohde [hakukohde-oid]}])]
        (let [session (session-with-rights :opinto-ohjaaja [organization-oid-1])
              result  (aac/applications-access-authorized-including-opinto-ohjaaja?
                        organization-service
                        tarjonta-service
                        suoritus-service
                        person-service
                        session
                        ["application-1-oid" "application-2-oid"]
                        [:view-applications])]
          (should= true result))))

    (it "returns true if user has authorization for one applications' forms' organization and opinto-ohjaaja for another"
        (with-redefs [application-store/applications-authorization-data
                      (fn [_]
                        [{:person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}
                         {:organization-oid organization-oid-2 :person-oid "opiskelija-2-oid" :hakukohde [hakukohde-oid]}])]
          (let [session (session-with-rights :view-applications [organization-oid-2] :opinto-ohjaaja [organization-oid-1])
                result  (aac/applications-access-authorized-including-opinto-ohjaaja?
                          organization-service
                          tarjonta-service
                          suoritus-service
                          person-service
                          session
                          ["application-1-oid" "application-2-oid"]
                          [:view-applications])]
            (should= true result))))

    (it "returns false if user has opinto-ohjaaja for only one applications' applicants"
      (with-redefs [application-store/applications-authorization-data
                    (fn [_]
                      [{:person-oid "opiskelija-1-oid" :hakukohde [hakukohde-oid]}
                       {:person-oid "opiskelija-3-oid" :hakukohde [hakukohde-oid]}])]
        (let [session (session-with-rights :opinto-ohjaaja [organization-oid-1])
              result  (aac/applications-access-authorized-including-opinto-ohjaaja?
                        organization-service
                        tarjonta-service
                        suoritus-service
                        person-service
                        session
                        ["application-1-oid" "application-2-oid"]
                        [:view-applications])]
          (should= false result))))))


(describe "applications-review-authorized?"
          (tags :unit)
          (it "returns true if user has edit rights to all hakukohteet"
              (let [session (session-with-rights :edit-applications
                                                 ["1.2.246.562.10.10826252480" "1.2.246.562.10.10826252479"])
                    result  (aac/applications-review-authorized?
                             organization-service
                             tarjonta-service
                             session
                             [:1.2.246.562.20.49028100004 :1.2.246.562.20.49028196522]
                             [:edit-applications])]
                (should= true result)))

          (it "returns false if user has edit rights to only some of hakukohteet"
              (let [session (session-with-rights :edit-applications ["1.2.246.562.10.10826252480"]
                                                 :view-applications ["1.2.246.562.10.10826252479"])
                    result  (aac/applications-review-authorized?
                             organization-service
                             tarjonta-service
                             session
                             [:1.2.246.562.20.49028100004 :1.2.246.562.20.49028196522]
                             [:edit-applications])]
                (should= false result)))

          (it "returns true if user has edit rights to hakukohderyhma"
              (let [session (session-with-rights :edit-applications ["1.2.246.562.28.00000000001"]
                                                 :view-applications ["1.2.246.562.10.10826252479"])
                    result  (aac/applications-review-authorized?
                             organization-service
                             tarjonta-service
                             session
                             [:1.2.246.562.20.49028196522]
                             [:edit-applications])]
                (should= false result))))

