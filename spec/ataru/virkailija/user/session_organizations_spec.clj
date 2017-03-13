(ns ataru.virkailija.user.session-organizations-spec
  (:require
   [speclj.core :refer [describe it should= tags around]]
   [ataru.virkailija.user.session-organizations :refer [select-organizations-for-rights]]))

(def dummy-session {:identity
                    {:user-right-organizations
                     {:form-edit [{:name {:fi "Telajärven aikuislukio"}
                                    :oid "1.2.246.562.10.1234334543"
                                   :type :organization}
                                  {:name {:fi "Omnia"}
                                    :oid "1.2.246.562.10.22"
                                   :type :organization}]
                      :view-applications [{:name {:fi "Omnia"}
                                           :oid "1.2.246.562.10.22"
                                           :type :organization}
                                          {:name {:fi "Lasikoulu"}
                                           :oid "1.2.246.562.10.11"
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
     (let [results (select-organizations-for-rights dummy-session [:form-edit :view-applications :edit-applications])]
       (should= expected-organizations-for-rights
                results))))

