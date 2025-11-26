(ns ataru.user-rights-spec
  (:require [ataru.user-rights :refer [virkailija->right-organization-oids
                                       all-organizations-have-only-opinto-ohjaaja-rights?
                                       has-opinto-ohjaaja-right-for-any-organization?]]
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

(def opo-user {:last-name                "Nukettaja"
               :username                 "Ruhtinas"
               :first-name               "Ruhtinas"
               :organizations            {
                                          :1.2.246.562.10.83119092639 {
                                                                       :oid                "1.2.246.562.10.83119092639"
                                                                       :name               {:en "Haagan peruskoulu"
                                                                                            :fi "Haagan peruskoulu"
                                                                                            :sv "Haagan peruskoulu"}
                                                                       :type               "organization",
                                                                       :rights             ["opinto-ohjaaja"]
                                                                       :oppilaitostyyppi   "oppilaitostyyppi_11#1"
                                                                       :organisaatiotyypit ["organisaatiotyyppi_02"]}}
               :lang                     "fi"
               :oid                      "1.2.246.562.24.99546464580"
               :ticket                   "ST-190762-0k4oDtYsCs6TECBNFeYlQrIJDRQ-ip-10-20-73-157"
               :user-right-organizations {
                                          :opinto-ohjaaja [{:oid  "1.2.246.562.10.83119092639"
                                                            :name {:en "Haagan peruskoulu" :fi "Haagan peruskoulu" :sv "Haagan peruskoulu"}
                                                            :type "organization"}]}
               :superuser                false})

(describe "virkailija->right-organization-oids"
          (tags :unit)
          (it "gets correct organization OID for the user right we require"
              (let [right-org-oids (virkailija->right-organization-oids
                                    (:organisaatiot test-user1)
                                     [:form-edit
                                      :view-applications
                                      :edit-applications])]
                (should= {:form-edit         ["1.2.246.562.6.214933"]
                          :view-applications ["1.2.246.562.9.9999"]}
                         right-org-oids)))
          (it "gets empty organization seq when no match is found"
              (let [org-oids (virkailija->right-organization-oids test-user2 [:form-edit])]
                (should= {} org-oids))))

(describe "opo rights"
          (tags :unit)
          (it "has opo rigts"
              (should= true (has-opinto-ohjaaja-right-for-any-organization?
                              {:identity opo-user})))

          (it "all user organizations have only opo rights"
              (should= true (all-organizations-have-only-opinto-ohjaaja-rights?
                              {:identity opo-user})))

          (it "not all user organizations have opo rights"
              (should= false (all-organizations-have-only-opinto-ohjaaja-rights?
                               {:identity
                                (assoc-in opo-user
                                          [:user-right-organizations :view-applications]
                                          [{:oid "1.2.246.562.10.83119092649"}])})))

          (it "not all user organizations have only opo rights"
              (should= false (all-organizations-have-only-opinto-ohjaaja-rights?
                               {:identity
                                (assoc-in opo-user [:user-right-organizations :view-applications]
                                          [{:oid "1.2.246.562.10.83119092639"}])})))

          (it "returns false when there are no organization rights at all"
              (should= false (all-organizations-have-only-opinto-ohjaaja-rights?
                               {:identity {:user-right-organizations {}}}))))
