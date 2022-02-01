(ns ataru.applications.suoritus-filter-spec
  (:require [speclj.core :refer [it describe tags should= should-contain should-not-contain]]
            [ataru.applications.suoritus-filter :as sf]
            [clj-time.format :as time-format]))

(defn- fake-get-oppilaitoksen-opiskelijat
  [oppilaitos-oid]
  (case oppilaitos-oid
    "oppilaitos-1"
    [{:person-oid "oppilaitos-1-luokka-9A-opiskelija"
      :luokka     "9A"}
     {:person-oid "oppilaitos-1-luokka-9B-opiskelija"
      :luokka     "9B"}]))

(describe "filter-applications-by-oppilaitos-and-luokat"
  (tags :unit)

  (it "should return input applications if no filters are provided"
    (let [applications [:dummy :dummy2]
          result       (sf/filter-applications-by-oppilaitos-and-luokat applications nil nil nil)]
      (should= applications result)))

  (it "should only return applications whose applicants are in given school"
    (let [application-1 {:person {:oid "oppilaitos-1-luokka-9A-opiskelija"}}
          application-2 {:person {:oid "other-person-oid"}}
          applications  [application-1 application-2]
          result        (sf/filter-applications-by-oppilaitos-and-luokat
                          applications
                          fake-get-oppilaitoksen-opiskelijat
                          "oppilaitos-1"
                          nil)]
      (should-contain application-1 result)
      (should-not-contain application-2 result)))

  (it "should only return applications whose applicants are in given school and class"
    (let [application-1 {:person {:oid "oppilaitos-1-luokka-9A-opiskelija"}}
          application-2 {:person {:oid "other-person-oid"}}
          applications  [application-1 application-2]
          result        (sf/filter-applications-by-oppilaitos-and-luokat
                          applications
                          fake-get-oppilaitoksen-opiskelijat
                          "oppilaitos-1"
                          ["9A"])]
      (should-contain application-1 result)
      (should-not-contain application-2 result)))

  (it "should only return applications whose applicants are in given school and one of given classes"
    (let [application-1 {:person {:oid "oppilaitos-1-luokka-9A-opiskelija"}}
          application-2 {:person {:oid "oppilaitos-1-luokka-9B-opiskelija"}}
          application-3 {:person {:oid "other-person-oid"}}
          applications  [application-1 application-2]
          result        (sf/filter-applications-by-oppilaitos-and-luokat
                          applications
                          fake-get-oppilaitoksen-opiskelijat
                          "oppilaitos-1"
                          ["9A" "9B"])]
      (should-contain application-1 result)
      (should-contain application-2 result)
      (should-not-contain application-3 result))))

(describe "year-for-suoritus-filter"
  (tags :unit)

  (it "returns the current year"
    (let [now (time-format/parse-local-date "2042-07-03")
          result (sf/year-for-suoritus-filter now)]
      (should= 2042 result))))
