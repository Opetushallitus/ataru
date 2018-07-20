(ns ataru.organization-service.user-rights-spec
  (:require [ataru.organization-service.user-rights :refer [virkailija->right-organization-oids]]
            [speclj.core :refer [describe it should= tags]]))

(def test-user1 {:oidHenkilo    "1.2.246.562.24.23424"
                 :organisaatiot [{:organisaatioOid "1.2.246.562.23.3.3434"
                                  :kayttooikeudet  [{:palvelu "KOODISTO"
                                                     :oikeus  "READ"}]}
                                 {:organisaatioOid "1.2.246.562.6.214933"
                                  :kayttooikeudet  [{:palvelu "ATARU_EDITORI"
                                                     :oikeus  "CRUD"}
                                                    {:palvelu "ORGANISAATIOHALLINTA"
                                                     :oikeus  "READ"}]}
                                 {:organisaatioOid "1.2.246.562.9.9999"
                                  :kayttooikeudet  [{:palvelu "ATARU_HAKEMUS"
                                                     :oikeus  "READ"}]}]})

(def test-user2 {:oidHenkilo    "1.2.246.562.24.29843"
                 :organisaatiot []})


(describe "virkailija->right-organization-oids"
  (tags :unit)
  (it "gets correct organization OID for the user right we require"
    (let [right-org-oids (virkailija->right-organization-oids
                          test-user1
                          [:form-edit
                           :view-applications
                           :edit-applications])]
      (should= {:form-edit         ["1.2.246.562.6.214933"]
                :view-applications ["1.2.246.562.9.9999"]}
               right-org-oids)))
  (it "gets empty organization seq when no match is found"
    (let [org-oids (virkailija->right-organization-oids test-user2 [:form-edit])]
      (should= {} org-oids))))
