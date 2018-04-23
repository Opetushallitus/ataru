(ns ataru.organization-service.session-organizations-spec
  (:require
    [speclj.core :refer [describe it should= tags around]]
    [ataru.organization-service.session-organizations :refer [select-organizations-for-rights]]
    [ataru.organization-service.organization-service :as organization-service]))

(def dummy-session {:identity
                    {:user-right-organizations
                     {:form-edit         [{:name {:fi "Telajärven aikuislukio"}
                                           :oid  "1.2.246.562.10.1234334543"
                                           :type :organization}
                                          {:name {:fi "Omnia"}
                                           :oid  "1.2.246.562.10.22"
                                           :type :organization}]
                      :view-applications [{:name {:fi "Omnia"}
                                           :oid  "1.2.246.562.10.22"
                                           :type :organization}
                                          {:name {:fi "Lasikoulu"}
                                           :oid  "1.2.246.562.10.11"
                                           :type :organization}]}}})

(def expected-organizations-for-rights
  [{:name {:fi "Telajärven aikuislukio"}
    :oid "1.2.246.562.10.1234334543"
    :type :organization}
   {:name {:fi "Omnia"}
    :oid "1.2.246.562.10.22"
    :type :organization}
   {:name {:fi "Lasikoulu"}
    :oid "1.2.246.562.10.11"
    :type :organization}])

(describe
 "Session organizations"
 (tags :unit :session-organizations)

 (it "selects unique list of organizations for multiple rights (no duplicates)"
     (let [results (select-organizations-for-rights
                     (organization-service/new-organization-service)
                     dummy-session
                     [:form-edit :view-applications :edit-applications])]
       (should= expected-organizations-for-rights
                results))))

