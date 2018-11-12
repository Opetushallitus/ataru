(ns ataru.person-service.oppijanumerorekisteri-person-extract-spec
  (:require
    [speclj.core :refer [describe it tags should should=]]
    [ataru.fixtures.application :as application-fixtures]
    [ataru.person-service.oppijanumerorekisteri-person-extract :refer [extract-person-from-application]]))

(def finnish-person {:hetu               "120496-924J"
                     :etunimet           "Aku"
                     :kutsumanimi        "Aku"
                     :sukunimi           "Ankka"
                     :aidinkieli         {:kieliKoodi "fi"}
                     :asiointiKieli      {:kieliKoodi "fi"}
                     :kansalaisuus       [{:kansalaisuusKoodi "246"}]
                     :eiSuomalaistaHetua false
                     :sukupuoli          "2"
                     :yhteystieto        [{:yhteystietoTyyppi "YHTEYSTIETO_SAHKOPOSTI"
                                           :yhteystietoArvo   "aku@ankkalinna.com"}]
                     :henkiloTyyppi  "OPPIJA"})

;; Only relevant fields here
(def foreign-application {:answers [{:key "email", :value "roger.moore@ankkalinna.com"}
                                    {:key "first-name" :value "Roger"}
                                    {:key "preferred-name" :value "Roger"}
                                    {:key "last-name" :value "Moore"}
                                    {:key "birth-date" :value "29.10.1984"}
                                    {:key "language" :value "SV"}
                                    {:key "nationality" :value [["247"]]}
                                    {:key "gender" :value "1"}]
                          :lang    "sv"})

(def dual-citizenship-application {:answers [{:key "email", :value "roger.moore@ankkalinna.com"}
                                             {:key "first-name" :value "Roger"}
                                             {:key "preferred-name" :value "Roger"}
                                             {:key "last-name" :value "Moore"}
                                             {:key "birth-date" :value "29.10.1984"}
                                             {:key "language" :value "SV"}
                                             {:key "nationality" :value [["247"] ["528"]]}
                                             {:key "gender" :value "1"}]
                                   :lang    "sv"})

(def asiointikieli-application {:answers [{:key "email", :value "roger.moore@ankkalinna.com"}
                                          {:key "first-name" :value "Roger"}
                                          {:key "preferred-name" :value "Roger"}
                                          {:key "last-name" :value "Moore"}
                                          {:key "birth-date" :value "29.10.1984"}
                                          {:key "language" :value "SV"}
                                          {:key "nationality" :value [["247"]]}
                                          {:key "gender" :value "1"}
                                          {:key "asiointikieli" :value "3"}]
                                :lang    "sv"})

(def expected-foreign-person {:etunimet           "Roger"
                              :kutsumanimi        "Roger"
                              :sukunimi           "Moore"
                              :aidinkieli         {:kieliKoodi "sv"}
                              :asiointiKieli      {:kieliKoodi "sv"}
                              :syntymaaika        "1984-10-29"
                              :eiSuomalaistaHetua true
                              :kansalaisuus       [{:kansalaisuusKoodi "247"}]
                              :sukupuoli          "1"
                              :yhteystieto        [{:yhteystietoTyyppi "YHTEYSTIETO_SAHKOPOSTI"
                                                    :yhteystietoArvo   "roger.moore@ankkalinna.com"}]
                              :henkiloTyyppi      "OPPIJA"
                              :identifications    [{:idpEntityId "oppijaToken"
                                                    :identifier "roger.moore@ankkalinna.com"}]})

(def expected-dual-citizenship-person {:etunimet           "Roger"
                                       :kutsumanimi        "Roger"
                                       :sukunimi           "Moore"
                                       :aidinkieli         {:kieliKoodi "sv"}
                                       :asiointiKieli      {:kieliKoodi "sv"}
                                       :syntymaaika        "1984-10-29"
                                       :eiSuomalaistaHetua true
                                       :kansalaisuus       [{:kansalaisuusKoodi "247"} {:kansalaisuusKoodi "528"}]
                                       :sukupuoli          "1"
                                       :yhteystieto        [{:yhteystietoTyyppi "YHTEYSTIETO_SAHKOPOSTI"
                                                             :yhteystietoArvo   "roger.moore@ankkalinna.com"}]
                                       :henkiloTyyppi      "OPPIJA"
                                       :identifications    [{:idpEntityId "oppijaToken"
                                                             :identifier  "roger.moore@ankkalinna.com"}]})

(def expected-asiointikieli-person {:etunimet           "Roger"
                                    :kutsumanimi        "Roger"
                                    :sukunimi           "Moore"
                                    :aidinkieli         {:kieliKoodi "sv"}
                                    :asiointiKieli      {:kieliKoodi "en"}
                                    :syntymaaika        "1984-10-29"
                                    :eiSuomalaistaHetua true
                                    :kansalaisuus       [{:kansalaisuusKoodi "247"}]
                                    :sukupuoli          "1"
                                    :yhteystieto        [{:yhteystietoTyyppi "YHTEYSTIETO_SAHKOPOSTI"
                                                          :yhteystietoArvo   "roger.moore@ankkalinna.com"}]
                                    :henkiloTyyppi      "OPPIJA"
                                    :identifications    [{:idpEntityId "oppijaToken"
                                                          :identifier  "roger.moore@ankkalinna.com"}]})

(describe "person extract"
  (tags :unit  :person-extract)
  (it "extracts finnish person correctly"
    (should=
     finnish-person
     (extract-person-from-application application-fixtures/application-with-person-info-module)))
  (it "extracts foreign person correctly"
    (should=
     expected-foreign-person
     (extract-person-from-application foreign-application)))
  (it "extracts dual citizenship person correctly"
    (should=
     expected-dual-citizenship-person
     (extract-person-from-application dual-citizenship-application)))
  (it "extracts asiointikieli correctly"
    (should=
     expected-asiointikieli-person
     (extract-person-from-application asiointikieli-application))))
