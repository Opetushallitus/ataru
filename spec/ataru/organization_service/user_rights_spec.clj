(ns ataru.organization-service.user-rights-spec
  (:require [ataru.organization-service.user-rights :refer [user->right-organization-oids]]
            [speclj.core :refer [describe it should= tags]]))

(def test-user1 {:employeeNumber "1.2.246.562.24.23424"
                 :description    "[\"USER_testi2editori\", \"VIRKAILIJA\", \"LANG_fi\", \"APP_KOODISTO\", \"APP_KOODISTO_READ\", \"APP_KOODISTO_READ_1.2.246.562.23.3.3434\", \"APP_ORGANISAATIOHALLINTA\", \"APP_ORGANISAATIOHALLINTA_READ\", \"APP_ORGANISAATIOHALLINTA_READ_1.2.246.562.6.234324\", \"APP_OID\", \"APP_OID_READ\", \"APP_OID_READ_1.2.246.562.6.234234\", \"APP_YHTEYSTIETOTYYPPIENHALLINTA\", \"APP_YHTEYSTIETOTYYPPIENHALLINTA_READ\", \"APP_YHTEYSTIETOTYYPPIENHALLINTA_READ_1.2.246.562.6.234238923\", \"APP_OMATTIEDOT\", \"APP_OMATTIEDOT_READ_UPDATE\", \"APP_OMATTIEDOT_READ_UPDATE_1.2.246.562.6.2323423\", \"APP_HENKILONHALLINTA\", \"APP_HENKILONHALLINTA_READ\", \"APP_HENKILONHALLINTA_READ_1.2.246.562.6.234234234\", \"APP_ANOMUSTENHALLINTA\", \"APP_ANOMUSTENHALLINTA_READ\", \"APP_ANOMUSTENHALLINTA_READ_1.2.246.562.6.2234324\", \"APP_KOOSTEROOLIENHALLINTA\", \"APP_KOOSTEROOLIENHALLINTA_READ\", \"APP_KOOSTEROOLIENHALLINTA_READ_1.2.246.562.6.234234234\", \"APP_TARJONTA\", \"APP_TARJONTA_READ\", \"APP_TARJONTA_READ_1.2.246.562.6.4546\", \"APP_HAKEMUS\", \"APP_HAKEMUS_READ_UPDATE\", \"APP_HAKEMUS_READ_UPDATE_1.2.246.562.6.456456\", \"APP_VALINTAPERUSTEKUVAUSTENHALLINTA\", \"APP_VALINTAPERUSTEKUVAUSTENHALLINTA_READ\", \"APP_VALINTAPERUSTEKUVAUSTENHALLINTA_READ_1.2.246.562.6.456346\", \"APP_HAKUJENHALLINTA\", \"APP_HAKUJENHALLINTA_READ\", \"APP_HAKUJENHALLINTA_READ_1.2.246.562.6.213434\", \"APP_HAKULOMAKKEENHALLINTA\", \"APP_ATARU_EDITORI_CRUD\", \"APP_ATARU_EDITORI_CRUD_1.2.246.562.6.214933\", \"APP_ATARU_EDITORI_CRUD_1.2.246.562.6.214933\", \"APP_ATARU_HAKEMUS_READ_1.2.246.562.9.9999\"]"})

(def test-user1-organization-oid "1.2.246.562.6.214933")

(def test-user2 {:employeeNumber "1.2.246.562.24.29843"
                 :description    "[\"USER_kuikka\", \"VIRKAILIJA\", \"LANG_fi\", \"APP_KOODISTO\", \"APP_KOODISTO_READ\"]"})


(describe "user->right-organization-oids"
  (tags :unit :ldap)
  (it "gets correct organization OID for the user right we require"
    (let [right-org-oids (user->right-organization-oids
                          test-user1
                          [:form-edit
                           :view-applications
                           :edit-applications])]
      (should= {:form-edit         [test-user1-organization-oid]
                :view-applications ["1.2.246.562.9.9999"]}
               right-org-oids)))
  (it "gets empty organization seq when no match is found"
    (let [org-oids (user->right-organization-oids test-user2 [:form-edit])]
      (should= {} org-oids))))
