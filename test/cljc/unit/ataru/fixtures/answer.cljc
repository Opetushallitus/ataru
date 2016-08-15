(ns ataru.fixtures.answer)

(def answer
  {:form 866,
   :lang "fi",
   :answers
   [{:key       "address",
     :value     "katu",
     :fieldType "textField",
     :label     {:fi "Katuosoite", :sv "Adress"}}
    {:key       "email",
     :value     "emil@emil.com",
     :fieldType "textField",
     :label     {:fi "Sähköpostiosoite", :sv "E-postadress"}}
    {:key       "preferred-name",
     :value     "emi",
     :fieldType "textField",
     :label     {:fi "Kutsumanimi", :sv "Smeknamn"}}
    {:key       "last-name",
     :value     "lastname",
     :fieldType "textField",
     :label     {:fi "Sukunimi", :sv "Efternamn"}}
    {:key       "phone",
     :value     "0452822274",
     :fieldType "textField",
     :label     {:fi "Matkapuhelin", :sv "Mobiltelefonnummer"}}
    {:key       "nationality",
     :value     "Suomi",
     :fieldType "dropdown",
     :label     {:fi "Kansalaisuus", :sv "Nationalitet"}}
    {:key       "ssn",
     :value     "020202-0202",
     :fieldType "textField",
     :label     {:fi "Henkilötunnus", :sv "Personnummer"}}
    {:key       "first-name",
     :value     "Eemil",
     :fieldType "textField",
     :label     {:fi "Etunimet", :sv "Förnamn"}}
    {:key   "postal-code",
     :value "01234",
     :fieldType "textField",
     :label {:fi "Postinumero", :sv "Postnummer"}}
    {:key       "language",
     :value     "suomi",
     :fieldType "dropdown",
     :label     {:fi "Äidinkieli", :sv "Modersmål"}}
    {:key       "gender",
     :value     "Mies",
     :fieldType "dropdown",
     :label     {:fi "Sukupuoli", :sv "Kön"}}
    {:key       "postal-office",
     :value     "paikka",
     :fieldType "textField",
     :label     {:fi "Postitoimipaikka", :sv "Postkontor"}}
    {:key       "home-town",
     :value     "kunta",
     :fieldType "textField",
     :label     {:fi "Kotikunta", :sv "Bostadsort"}}]})

