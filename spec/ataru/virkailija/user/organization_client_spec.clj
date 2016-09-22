(ns ataru.virkailija.user.organization-client-spec
  (:require
   [clojure.java.io :as io]
   [cheshire.core :as json]
   [speclj.core :refer [describe it should= tags]]
   [ataru.virkailija.user.organization-client :as org-client]
   [ataru.cas.client :as cas-client]
   [oph.soresu.common.config :refer [config]]
   [clojure.java.io :as io]))

(def organization-hierarchy-data (slurp (io/resource "organisaatio_service/organization-hierarchy1.json")))
(def expected-flat-organizations '({:name {:fi "Telajärven seudun koulutuskuntayhtymä"},
                                    :oid "1.2.246.562.10.3242342"}
                                   {:name {:fi "Telajärven aikuislukio"},
                                    :oid "1.2.246.562.10.1234334543"}
                                   {:name {:fi "Telajärven aikuisopisto"},
                                    :oid "1.2.246.562.10.932489234"}
                                   {:name {:fi "Telajärven aikuisopisto, Äyhtävä"},
                                    :oid "1.2.246.562.10.123943342"}
                                   {:name {:fi "Telajärven aikuisopisto, Prunkila"},
                                    :oid "1.2.246.562.10.938234"}
                                   {:name {:fi "Telajärven työväenopisto"},
                                    :oid "1.2.246.562.10.9239423"}
                                   {:name {:sv "Telajärven hierontaopisto"},
                                    :oid "1.2.246.562.10.423834"}
                                   {:name {:fi "Telajärven kaupungin työväenopisto"},
                                    :oid "1.2.246.562.10.323412"}))
(def oph-oid "1.2.246.562.10.00000000001")
(defn fake-cas-auth-no-organization [cas-client url]
  {:status 200 :body (io/resource "organisaatio_service/organization-response2.json")})
(defn fake-cas-auth-organization [cas-client url]
  {:status 200 :body (io/resource "organisaatio_service/organization-response1.json")})
(def fake-config {:organization-service {:base-address "dummy"} :cas {}})

(describe "organization client"
          (tags :unit)
          (it "transforms organization hierarchy into a flat sequence"
              (let [parsed-hierarchy (json/parse-string organization-hierarchy-data true)
                    organizations (org-client/get-all-organizations-as-seq parsed-hierarchy)]
                (should= expected-flat-organizations organizations)))
          (it "Returns the hard-coded OPH organization for the known OID"
              (with-redefs [config fake-config]
                (should= {:oid oph-oid :name {:fi "OPH"}}
                         (org-client/get-organization nil oph-oid))))
          (it "Returns nil if numHits is zero"
              (with-redefs [config                            fake-config
                            cas-client/cas-authenticated-get  fake-cas-auth-no-organization]
                (should= nil
                         (org-client/get-organization nil "1.2.246.562.10.2.445.3"))))
          (it "Returns the organization in normal case (numHits 1)"
              (with-redefs [config                            fake-config
                            cas-client/cas-authenticated-get  fake-cas-auth-organization]
                (should= {:name {:fi "Telajärven seudun koulutuskuntayhtymä"}, :oid "1.2.246.562.10.3242342"}
                         (org-client/get-organization nil "1.2.246.562.10.3242342")))))
