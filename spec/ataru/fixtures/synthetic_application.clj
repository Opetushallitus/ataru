(ns ataru.fixtures.synthetic-application)

(def synthetic-application-initial
  {:hakuOid          "1.2.246.562.29.93102260101"
   :hakukohdeOid     "1.2.246.562.20.49028196522"
   :sukunimi         "Ankka"
   :etunimet         "Aku Fauntleroy"
   :kutsumanimi      "Aku"
   :kansalaisuus     ["246"]
   :syntymaaika      "1.1.2001"
   :syntymaPaikka    "Paikka"
   :hetu             "010101A123N"
   :sukupuoli        "1"
   :passinNumero     nil
   :idTunnus         nil
   :email            "aku.ankka@example.com"
   :matkapuhelin     "050123"
   :asuinmaa         "246"
   :lahiosoite       "Paratiisitie 13"
   :postinumero      "00013"
   :postitoimipaikka "Ankkalinna"
   :kotikunta        "273"
   :aidinkieli       "FI"
   :asiointikieli    "1"
   :toisenAsteenKoulutus "1"
   :toisenAsteenKoulutusMaa nil
   })

(def synthetic-application-initial-answers
  [{:key "address" :value "Paratiisitie 13" :fieldType "textField" :label {:fi "Katuosoite" :sv "Adress"}}
   {:key "email" :value "aku.ankka@example.com" :fieldType "textField" :label {:fi "Sähköpostiosoite" :sv "E-postadress"}}
   {:key "preferred-name" :value "Aku" :fieldType "textField" :label {:fi "Kutsumanimi" :sv "Smeknamn"}}
   {:key "last-name" :value "Ankka" :fieldType "textField" :label {:fi "Sukunimi" :sv "Efternamn"}}
   {:key "phone" :value "050123" :fieldType "textField"  :label {:fi "Matkapuhelin" :sv "Mobiltelefonnummer"}}
   {:key "nationality" :value [["246"]] :fieldType "dropdown" :label {:fi "Kansalaisuus" :sv "Nationalitet"}}
   {:key "country-of-residence" :value "246" :fieldType "dropdown" :label {:fi "Asuinmaa" :sv "Boningsland"}}
   {:key "ssn" :value "010101A123N" :fieldType "textField" :label {:fi "Henkilötunnus" :sv "Personnummer"}}
   {:key "first-name" :value "Aku Fauntleroy" :fieldType "textField" :label {:fi "Etunimet" :sv "Förnamn"}}
   {:key "postal-code" :value "00013" :fieldType "textField" :label {:fi "Postinumero" :sv "Postnummer"}}
   {:key "postal-office" :value "Ankkalinna" :fieldType "textField" :label {:fi "Postitoimipaikka"}}
   {:key "home-town" :value "273" :fieldType "dropdown" :label {:fi "Kotikunta"}}
   {:key "language" :value "FI" :fieldType "dropdown" :label {:fi "Äidinkieli" :sv "Modersmål"}}
   {:key "gender" :value "1" :fieldType "dropdown" :label {:fi "Sukupuoli" :sv "Kön"}}
   {:key "birth-date" :value "1.1.2001" :fieldType "textField" :label {:fi "Syntymäaika"}}
   {:key "asiointikieli" :value "1" :fieldType "dropdown" :fieldClass "formField" :label {:fi "Asiointikieli" :sv "Ärendespråk"}}
   {:key "secondary-completed-base-education" :value "0" :fieldType "singleChoice" :label {:fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon?"}}])
