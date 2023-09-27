(ns ataru.fixtures.synthetic-application)

(def synthetic-application-basic
  {:hakuOid          "1.2.246.562.29.93102260101"
   :hakukohdeOid     "1.2.246.562.20.49028196522"
   :sukunimi         "Ankka"
   :etunimi          "Aku Fauntleroy"
   :kutsumanimi      "Aku"
   :kansalaisuus     "246"
   :syntymaAika      nil
   :syntymapaikka    nil
   :henkilotunnus    "010105A923H"
   :sukupuoli        nil
   :passinNumero     nil
   :idTunnus         nil
   :sahkoposti       "aku.ankka@example.com"
   :puhelinnumero    "050123"
   :asuinmaa         "246"
   :osoite           "Paratiisitie 13"
   :postinumero      "00013"
   :postitoimipaikka "Ankkalinna"
   :kotikunta        "273"
   :kaupunkiJaMaa    nil
   :aidinkieli       "FI"
   :asiointikieli    "1"
   :toisenAsteenSuoritus "1"
   :toisenAsteenSuoritusmaa nil
   })

; Mock ohjausparametrit service returns false / empty synthetic application data with this haku OID
(def synthetic-application-with-disabled-haku
  (merge synthetic-application-basic {:hakuOid "1.2.246.562.29.12345678910"}))

(def synthetic-application-foreign
  {:hakuOid          "1.2.246.562.29.93102260101"
   :hakukohdeOid     "1.2.246.562.20.49028196522"
   :sukunimi         "Duck"
   :etunimi          "Donald Fauntleroy"
   :kutsumanimi      "Donald"
   :kansalaisuus     "840"
   :syntymaAika      "1.1.2001"
   :syntymapaikka    "Duckburg, USA"
   :henkilotunnus    nil
   :sukupuoli        "1"
   :passinNumero     "1234"
   :idTunnus         "333"
   :sahkoposti       "donald.duck@example.com"
   :puhelinnumero    "050123"
   :asuinmaa         "840"
   :osoite           "1313 Webfoot Street"
   :postinumero      "00013"
   :postitoimipaikka nil
   :kotikunta        nil
   :kaupunkiJaMaa    "Duckburg, USA"
   :aidinkieli       "EN"
   :asiointikieli    "3"
   :toisenAsteenSuoritus "0"
   :toisenAsteenSuoritusmaa "840"})

(def synthetic-application-malformed
  {:hakuOid          "1.2.246.562.29.93102260101"
   :hakukohdeOid     "1.2.246.562.20.49028196522"
   :sukunimi         "Duck"
   :etunimi          "Donald Fauntleroy"
   :kutsumanimi      "Donald"
   :kansalaisuus     "840"
   :syntymaAika      nil
   :syntymapaikka    "Duckburg, USA"
   :henkilotunnus    nil
   :sukupuoli        "1"
   :passinNumero     "1234"
   :idTunnus         "333"
   :sahkoposti       "donald.duck@example.com"
   :puhelinnumero    "050123"
   :asuinmaa         "840"
   :osoite           "1313 Webfoot Street"
   :postinumero      "00013"
   :postitoimipaikka nil
   :kotikunta        nil
   :kaupunkiJaMaa    "Duckburg, USA"
   :aidinkieli       "EN"
   :asiointikieli    "3"
   :toisenAsteenSuoritus "0"
   :toisenAsteenSuoritusmaa "840"})
