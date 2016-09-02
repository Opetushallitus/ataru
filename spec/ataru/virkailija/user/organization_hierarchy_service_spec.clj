(ns ataru.virkailija.user.organization-hierarchy-service-spec
  (:require
   [clojure.java.io :as io]
   [cheshire.core :as json]
   [speclj.core :refer [describe it should= tags]]
   [ataru.virkailija.user.organization-service :as org-service]))

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


(describe "organization hierarchy"
          (tags :unit)
          (it "Returns suborganizations"
              (let [parsed-hierarchy (json/parse-string organization-hierarchy-data true)
                    organizations (org-service/get-all-organizations-as-seq parsed-hierarchy)]
                (should= expected-flat-organizations organizations))))
