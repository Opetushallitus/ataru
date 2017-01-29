(ns ataru.person-service.person-extract-spec
  (:require
   [speclj.core :refer [describe it tags should should=]]
   [ataru.fixtures.application :as application-fixtures]
   [ataru.person-service.person-extract :refer [extract-person-from-application]]))

(def finnish-person {:hetu               "120496-924J"
                     :etunimet           "Aku"
                     :kutsumanimi        "Aku"
                     :sukunimi           "Ankka"
                     :aidinkieli         {:kielikoodi "FI"}
                     :kansalaisuus       {:kansalaisuusKoodi "246"}
                     :eiSuomalaistaHetua false
                     :sukupuoli          "2"
                     :yhteystieto        [{:yhteystietoTyyppi "YHTEYSTIETO_SAHKOPOSTI"
                                           :yhteystietoArvo   "aku@ankkalinna.com"}]})

;; Only relevant fields here
(def foreign-application {:answers [{:key "email",:value "roger.moore@ankkalinna.com"}
                                    {:key "first-name" :value "Roger"}
                                    {:key "preferred-name" :value "Roger"}
                                    {:key "last-name" :value "Moore"}
                                    {:key "birth-date" :value "29.10.1984"}
                                    {:key "language" :value "SV"}
                                    {:key "nationality" :value "247"}
                                    {:key "gender" :value "1"}]})

(def expected-foreign-person {:etunimet           "Roger"
                              :kutsumanimi        "Roger"
                              :sukunimi           "Moore"
                              :aidinkieli         {:kielikoodi "SV"}
                              :syntymaaika        "1984-10-29"
                              :eiSuomalaistaHetua true
                              :kansalaisuus       {:kansalaisuusKoodi "247"}
                              :sukupuoli          "1"
                              :yhteystieto        [{:yhteystietoTyyppi "YHTEYSTIETO_SAHKOPOSTI"
                                                    :yhteystietoArvo   "roger.moore@ankkalinna.com"}]})

(describe
 "person extract"
 (tags :unit  :person-extract)
 (it "extracts finnish person correctly"
     (should=
      finnish-person
      (extract-person-from-application application-fixtures/application-with-person-info-module)))
 (it "extracts foreign person correctly"
     (should=
      expected-foreign-person
      (extract-person-from-application foreign-application))))
