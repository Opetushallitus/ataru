(ns ataru.person-service.person-service-spec
  (:require [ataru.person-service.person-service :as person-service]
            [ataru.person-service.person-client :as person-client]
            [speclj.core :refer
             [describe it tags should= with-stubs stub should-have-invoked should-not-have-invoked]]))

(def email "match@match.com")

(def eidas-id "eidas123")

(def email-answer
  {:key       "email",
   :value     email,
   :fieldType "textField",
   :label     {:fi "Sähköpostiosoite", :sv "E-postadress"}})

(def bd-answer
  {:key       "birth-date",
   :value     "02.02.2002",
   :fieldType "textField",
   :label     {:fi "Syntymäaika", :sv "Födelsetid"}})

(def gender-answer
  {:key       "gender",
   :value     "1",
   :fieldType "dropdown",
   :label     {:fi "Sukupuoli", :sv "Kön"}})

(def ssn-answer
  {:key       "ssn",
   :value     "020202A0202",
   :fieldType "textField",
   :label     {:fi "Henkilötunnus", :sv "Personnummer"}})

(def language-answer
  {:key       "language",
   :value     "fi",
   :fieldType "dropdown",
   :label     {:fi "Äidinkieli", :sv "Modersmål"}})

(def preferred-name-answer
  {:key       "preferred-name",
   :value     "emi",
   :fieldType "textField",
   :label     {:fi "Kutsumanimi", :sv "Smeknamn"}})

(def last-name-answer
  {:key       "last-name",
   :value     "lastname",
   :fieldType "textField",
   :label     {:fi "Sukunimi", :sv "Efternamn"}})

(def first-name-answer
  {:key       "first-name",
   :value     "Eemil",
   :fieldType "textField",
   :label     {:fi "Etunimet", :sv "Förnamn"}})

(def shared-answers
  [email-answer bd-answer gender-answer language-answer preferred-name-answer last-name-answer first-name-answer])

(def application-no-ssn
  {:lang "fi"
   :answers shared-answers})

(def application-ssn
  {:lang "fi"
   :answers (concat shared-answers [ssn-answer])})

(def application-with-eidas (assoc application-no-ssn :eidas-id eidas-id))

(def service (person-service/map->IntegratedPersonService {}))

(describe "Person service"

          (tags :unit :person-service)
          (with-stubs)

          (it "finds or creates person with ssn"
              (with-redefs [person-client/create-or-find-person (stub :create-or-find
                                                                      {:return {:status :created :oid "1.2.3.4.5"}})
                            person-client/get-person-by-identification (stub :get-by-identification)]
                (should= {:status :created :oid "1.2.3.4.5"}
                         (person-service/create-or-find-person service application-ssn))
                (should-not-have-invoked :get-by-identification)))

          (it "finds person without ssn with email and uses that person if gender and date of birth match"
              (with-redefs [person-client/create-or-find-person (stub :create-or-find)
                            person-client/add-identification-to-person (stub :add-identification)
                            person-client/get-person-by-identification (stub :get-by-identification
                                                                             {:return
                                                                              {:status :found
                                                                               :body {:oidHenkilo "1.2.3.4.5"
                                                                                      :sukupuoli "1"
                                                                                      :syntymaaika "2002-02-02"}}})
                            person-client/create-person (stub :create-person)]
                (should= {:status :found-matching :oid "1.2.3.4.5"}
                         (person-service/create-or-find-person service application-no-ssn))
                (should-have-invoked :get-by-identification
                                     {:with [:* {:idpEntityId "oppijaToken" :identifier email}]})
                (should-not-have-invoked :create-or-find)
                (should-not-have-invoked :create-person)
                (should-not-have-invoked :add-identification)))

          (it "finds person without ssn with email but if date of birth does not match creates new person"
              (with-redefs [person-client/create-or-find-person (stub :create-or-find)
                            person-client/add-identification-to-person (stub :add-identification)
                            person-client/get-person-by-identification (stub :get-by-identification
                                                                             {:return
                                                                              {:status :found
                                                                               :body {:oidHenkilo "1.2.3.4.5"
                                                                                      :sukupuoli "1"
                                                                                      :syntymaaika "2001-01-01"}}})
                            person-client/create-person (stub :create-person
                                                              {:return {:oid "5.4.3.2.1" :status :created}})]
                (should= {:status :dob-or-gender-conflict :oid "5.4.3.2.1"}
                         (person-service/create-or-find-person service application-no-ssn))
                (should-have-invoked :get-by-identification
                                     {:with [:* {:idpEntityId "oppijaToken" :identifier email}]})
                (should-not-have-invoked :create-or-find)
                (should-not-have-invoked :add-identification)))

          (it "finds person without ssn with email but if gender does not match creates new person"
              (with-redefs [person-client/create-or-find-person (stub :create-or-find)
                            person-client/add-identification-to-person (stub :add-identification)
                            person-client/get-person-by-identification (stub :get-by-identification
                                                                             {:return
                                                                              {:status :found
                                                                               :body {:oidHenkilo "1.2.3.4.5"
                                                                                      :sukupuoli "2"
                                                                                      :syntymaaika "2002-02-02"}}})
                            person-client/create-person (stub :create-person
                                                              {:return {:oid "5.4.3.2.1" :status :created}})]
                (should= {:status :dob-or-gender-conflict :oid "5.4.3.2.1"}
                         (person-service/create-or-find-person service application-no-ssn))
                (should-have-invoked :get-by-identification
                                     {:with [:* {:idpEntityId "oppijaToken" :identifier email}]})
                (should-not-have-invoked :create-or-find)
                (should-not-have-invoked :add-identification)))

          (it "creates new person and adds email identification if no ssn and no person found with email"
              (with-redefs [person-client/create-or-find-person (stub :create-or-find)
                            person-client/add-identification-to-person (stub :add-identification)
                            person-client/get-person-by-identification (stub :get-by-identification
                                                                             {:return {:status :not-found :body nil}})
                            person-client/create-person (stub :create-person
                                                              {:return {:oid "5.4.3.2.1" :status :created}})]
                (should= {:status :created-with-email-id :oid "5.4.3.2.1"}
                         (person-service/create-or-find-person service application-no-ssn))
                (should-have-invoked :get-by-identification
                                     {:with [:* {:idpEntityId "oppijaToken" :identifier email}]})
                (should-have-invoked :add-identification
                                     {:with [:* "5.4.3.2.1" {:idpEntityId "oppijaToken" :identifier email}]})
                (should-not-have-invoked :create-or-find)))

          (it "finds person without ssn with eidas and uses that person"
              (with-redefs [person-client/create-or-find-person (stub :create-or-find)
                            person-client/add-identification-to-person (stub :add-identification)
                            person-client/get-person-by-identification (stub :get-by-identification
                                                                             {:return
                                                                              {:status :found
                                                                               :body {:oidHenkilo "1.2.3.4.6"}}})
                            person-client/create-person (stub :create-person)]
                (should= {:status :found-matching :oid "1.2.3.4.6"}
                         (person-service/create-or-find-person service application-with-eidas))
                (should-have-invoked :get-by-identification
                                     {:with [:* {:idpEntityId "eidas" :identifier eidas-id}]})
                (should-not-have-invoked :create-or-find)
                (should-not-have-invoked :create-person)
                (should-not-have-invoked :add-identification)))

          (it "creates new person and adds eidas identification if no ssn and no person found with eidas"
              (with-redefs [person-client/create-or-find-person (stub :create-or-find)
                            person-client/add-identification-to-person (stub :add-identification)
                            person-client/get-person-by-identification (stub :get-by-identification
                                                                             {:return {:status :not-found :body nil}})
                            person-client/create-person (stub :create-person
                                                              {:return {:oid "5.4.3.2.2" :status :created}})]
                (should= {:status :created-with-eidas-id :oid "5.4.3.2.2"}
                         (person-service/create-or-find-person service application-with-eidas))
                (should-have-invoked :get-by-identification
                                     {:with [:* {:idpEntityId "eidas" :identifier eidas-id}]})
                (should-have-invoked :add-identification
                                     {:with [:* "5.4.3.2.2" {:idpEntityId "eidas" :identifier eidas-id}]})
                (should-not-have-invoked :create-or-find))))
