(ns ataru.fixtures.synthetic-application)

(def synthetic-application-basic
  {:hakuOid          "1.2.246.562.29.93102260101"
   :hakukohdeOid     "1.2.246.562.20.49028196522"
   :sukunimi         "Ankka"
   :etunimet         "Aku Fauntleroy"
   :kutsumanimi      "Aku"
   :kansalaisuus     ["246"]
   :syntymaaika      nil
   :syntymapaikka    nil
   :hetu             "010105A923H"
   :sukupuoli        nil
   :passinNumero     nil
   :idTunnus         nil
   :email            "aku.ankka@example.com"
   :matkapuhelin     "050123"
   :asuinmaa         "246"
   :lahiosoite       "Paratiisitie 13"
   :postinumero      "00013"
   :postitoimipaikka "Ankkalinna"
   :kotikunta        "273"
   :kaupunkiJaMaa    nil
   :aidinkieli       "FI"
   :asiointikieli    "1"
   :toisenAsteenKoulutus "1"
   :toisenAsteenKoulutusMaa nil
   })

(def synthetic-application-foreign
  {:hakuOid          "1.2.246.562.29.93102260101"
   :hakukohdeOid     "1.2.246.562.20.49028196522"
   :sukunimi         "Duck"
   :etunimet         "Donald Fauntleroy"
   :kutsumanimi      "Donald"
   :kansalaisuus     ["840"]
   :syntymaaika      "1.1.2001"
   :syntymapaikka    "Duckburg, USA"
   :hetu             nil
   :sukupuoli        "1"
   :passinNumero     "1234"
   :idTunnus         "333"
   :email            "donald.duck@example.com"
   :matkapuhelin     "050123"
   :asuinmaa         "840"
   :lahiosoite       "1313 Webfoot Street"
   :postinumero      "00013"
   :postitoimipaikka nil
   :kotikunta        nil
   :kaupunkiJaMaa    "Duckburg, USA"
   :aidinkieli       "EN"
   :asiointikieli    "3"
   :toisenAsteenKoulutus "0"
   :toisenAsteenKoulutusMaa "840"})

(def synthetic-application-malformed
  {:hakuOid          "1.2.246.562.29.93102260101"
   :hakukohdeOid     "1.2.246.562.20.49028196522"
   :sukunimi         "Duck"
   :etunimet         "Donald Fauntleroy"
   :kutsumanimi      "Donald"
   :kansalaisuus     ["840"]
   :syntymaaika      nil
   :syntymapaikka    "Duckburg, USA"
   :hetu             nil
   :sukupuoli        "1"
   :passinNumero     "1234"
   :idTunnus         "333"
   :email            "donald.duck@example.com"
   :matkapuhelin     "050123"
   :asuinmaa         "840"
   :lahiosoite       "1313 Webfoot Street"
   :postinumero      "00013"
   :postitoimipaikka nil
   :kotikunta        nil
   :kaupunkiJaMaa    "Duckburg, USA"
   :aidinkieli       "EN"
   :asiointikieli    "3"
   :toisenAsteenKoulutus "0"
   :toisenAsteenKoulutusMaa "840"})
