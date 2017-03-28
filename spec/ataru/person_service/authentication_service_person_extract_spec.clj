(ns ataru.person-service.authentication-service-person-extract-spec
  (:require [ataru.person-service.authentication-service-person-extract :as asp]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.config.core :refer [config]]
            [speclj.core :refer [describe it tags should should=]]))


(def application application-fixtures/application-with-person-info-module)
(def finnish-person {:email          "aku@ankkalinna.com",
                     :personId       "120496-924J",
                     :nativeLanguage "FI",
                     :nationality    "246",
                     :firstName      "Aku",
                     :lastName       "Ankka",
                     :gender         "2",
                     :idpEntitys     [{:idpEntityId "oppijaToken"
                                       :identifier "aku@ankkalinna.com"}]})

(def finnish-person-with-oid (assoc finnish-person :personOid "1.2.246.562.24.56818753409"))

;; Only relevant fields here
(def foreign-application {:answers [{:key "email",:value "roger.moore@ankkalinna.com"}
                                    {:key "first-name" :value "Roger"}
                                    {:key "last-name" :value "Moore"}
                                    {:key "birth-date" :value "29.10.1984"}
                                    {:key "language" :value "SV"}
                                    {:key "nationality" :value "247"}
                                    {:key "gender" :value "1"}]})

(def expected-foreign-person {:email          "roger.moore@ankkalinna.com"
                              :nativeLanguage "SV"
                              :nationality    "247"
                              :firstName      "Roger"
                              :lastName       "Moore"
                              :birthDate      "1984-10-29"
                              :gender         "1"
                              :idpEntitys     [{:idpEntityId "oppijaToken"
                                                :identifier "roger.moore@ankkalinna.com"}]})

(describe
 "extract person"
 (tags :unit :auth-extract-person)
 (it "extracts finnish person correctly"
     (should=
      finnish-person
      (asp/extract-person-from-application  application-fixtures/application-with-person-info-module)))
 (it "extracts foreign person correctly"
     (should=
      expected-foreign-person
      (asp/extract-person-from-application foreign-application))))

