(ns ataru.applications.application-access-control-spec
  (:require [speclj.core :refer [describe tags it should should-not should-be should-contain should=]]
            [ataru.applications.application-access-control :as aac]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]
            [ataru.suoritus.suoritus-service :as suoritus-service]))

; These have to be supported by FakeOrganizationService
(def organization-oid-1 "1.2.246.562.10.11")
(def organization-oid-2 "1.2.246.562.10.1234334543")
(def organization-oid-3 "1.2.246.562.10.22")

; These have to be supported by MockTarjontaService
(def hakukohde-oid "hakukohde.oid")
(def hakukohteen-tarjoaja-oid "1.2.246.562.10.10826252479")

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
    (condp = oppilaitos-oid
      organization-oid-1
      [{:person-oid "opiskelija-1-oid"
        :luokka     "9C"}]

      []))

  (oppilaitoksen-luokat
    [this oppilaitos-oid vuosi luokkatasot]
    []))

(def organization-service (organization-service/->FakeOrganizationService))
(def tarjonta-service (tarjonta-service/->MockTarjontaService))
(def suoritus-service (->FakeSuoritusService))

(describe "organization-oid-authorized-by-session-pred"
  (tags :unit)

  (describe "user is ordinary user with organizations"
    (let [session {:identity
                   {:user-right-organizations
                    {:opinto-ohjaaja    [{:oid organization-oid-1}]
                     :view-applications [{:oid organization-oid-2}]
                     :edit-applications [{:oid organization-oid-3}]}}}
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
          result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service session applications)]
      (should-be empty? result)))

  (it "does not return application if user has authorization for different organization"
    (let [session      {:identity
                        {:user-right-organizations
                         {:view-applications [{:oid organization-oid-1}]}}}
          applications [{:oid "application-oid" :hakukohde [hakukohde-oid]}]
          result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service session applications)]
      (should-be empty? (map :oid result))))

  (it "returns application if it includes hakukohde with authorized tarjoaja"
    (let [session      {:identity
                        {:user-right-organizations
                         {:view-applications [{:oid hakukohteen-tarjoaja-oid}]}}}
          applications [{:oid "application-oid" :hakukohde [hakukohde-oid]}]
          result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service session applications)]
      (should-contain "application-oid" (map :oid result))))

  (it "returns application if its form was created by authorized organization"
    (let [session      {:identity
                        {:user-right-organizations
                         {:view-applications [{:oid organization-oid-1}]}}}
          applications [{:organization-oid organization-oid-1 :oid "application-oid" :hakukohde [hakukohde-oid]}]
          result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service session applications)]
      (should-contain "application-oid" (map :oid result))))

  (it "returns application if user has opinto-ohjaaja authorization to applicant's lahtokoulu"
    (let [session      {:identity
                        {:user-right-organizations
                         {:opinto-ohjaaja [{:oid organization-oid-1}]}}}
          applications [{:person-oid "opiskelija-1-oid" :oid "application-oid" :hakukohde [hakukohde-oid]}]
          result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service session applications)]
      (should-contain "application-oid" (map :oid result))))

  (it "does not duplicate application if authorization is given by both tarjoaja and lahtokoulu"
    (let [session      {:identity
                        {:user-right-organizations
                         {:opinto-ohjaaja    [{:oid organization-oid-1}]
                          :view-applications [{:oid hakukohteen-tarjoaja-oid}]}}}
          applications [{:person-oid "opiskelija-1-oid" :oid "application-1-oid" :hakukohde [hakukohde-oid]}
                        {:person-oid "opiskelija-2-oid" :oid "application-2-oid" :hakukohde ["foo"]}]
          result       (aac/filter-authorized-by-session organization-service tarjonta-service suoritus-service session applications)]
      (should= 1 (count result))
      (should-contain "application-1-oid" (map :oid result)))))
