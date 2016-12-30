(ns ataru.virkailija.user.organization-client-spec
  (:require
   [clojure.java.io :as io]
   [cheshire.core :as json]
   [speclj.core :refer [describe it should= tags around]]
   [ataru.virkailija.user.organization-client :as org-client]
   [ataru.cas.client :as cas-client]
   [oph.soresu.common.config :refer [config]]
   [clojure.java.io :as io]))

(def organization-hierarchy-data (slurp (io/resource "organisaatio_service/organization-hierarchy1.json")))
(def expected-flat-organizations '({:name {:fi "Telajärven seudun koulutuskuntayhtymä"},
                                    :oid "1.2.246.562.10.3242342"
                                    :type :organization}
                                   {:name {:fi "Telajärven aikuislukio"}
                                    :oid "1.2.246.562.10.1234334543"
                                    :type :organization}
                                   {:name {:fi "Telajärven aikuisopisto"}
                                    :oid "1.2.246.562.10.932489234"
                                    :type :organization}
                                   {:name {:fi "Telajärven aikuisopisto, Äyhtävä"}
                                    :oid "1.2.246.562.10.123943342"
                                    :type :organization}
                                   {:name {:fi "Telajärven aikuisopisto, Prunkila"}
                                    :oid "1.2.246.562.10.938234"
                                    :type :organization}
                                   {:name {:fi "Telajärven työväenopisto"}
                                    :oid "1.2.246.562.10.9239423"
                                    :type :organization}
                                   {:name {:sv "Telajärven hierontaopisto"}
                                    :oid "1.2.246.562.10.423834"
                                    :type :organization}
                                   {:name {:fi "Telajärven kaupungin työväenopisto"}
                                    :oid "1.2.246.562.10.323412"
                                    :type :organization}))
(def oph-oid "1.2.246.562.10.00000000001")
(defn fake-cas-auth-no-organization [cas-client url]
  {:status 200 :body (slurp (io/resource "organisaatio_service/organization-response2.json"))})
(defn fake-cas-auth-organization [cas-client url]
  {:status 200 :body (slurp (io/resource "organisaatio_service/organization-response1.json"))})
(defn fake-cas-auth-groups [cas-client url]
  {:status 200 :body (slurp (io/resource "organisaatio_service/organization-response-groups.json"))})
(def fake-config {:organization-service {:base-address "dummy"} :cas {}})

(describe "organization client"
          (around [spec]
                  (with-redefs [config                            fake-config
                                cas-client/cas-authenticated-get  fake-cas-auth-organization]
                    (spec)))
          (tags :unit :organization)
          (it "transforms organization hierarchy into a flat sequence"
              (let [parsed-hierarchy (json/parse-string organization-hierarchy-data true)
                    organizations (org-client/get-all-organizations-as-seq parsed-hierarchy)]
                (should= expected-flat-organizations organizations)))
          (it "Returns the hard-coded OPH organization for the known OID"
              (should= {:oid oph-oid :name {:fi "OPH"} :type :organization}
                       (org-client/get-organization nil oph-oid)))
          (it "Returns nil if numHits is zero"
              (with-redefs [cas-client/cas-authenticated-get  fake-cas-auth-no-organization]
                (should= nil
                         (org-client/get-organization nil "1.2.246.562.10.2.445.3"))))
          (it "Returns the organization in normal case (numHits 1)"
              (should= {:name {:fi "Telajärven seudun koulutuskuntayhtymä"}
                        :oid "1.2.246.562.10.3242342"
                        :type :organization}
                       (org-client/get-organization nil "1.2.246.562.10.3242342")))
          (it "Returns groups"
              (with-redefs [cas-client/cas-authenticated-get fake-cas-auth-groups]
              (should= [{:name {:fi "Yhteiskäyttöryhmä"}
                         :oid "1.2.246.562.28.1.2"
                         :type :group}
                        {:name {:fi "Toinen ryhmä"}
                         :oid "1.2.246.562.28.1.3"
                         :type :group}]
                       (org-client/get-groups nil)))))
