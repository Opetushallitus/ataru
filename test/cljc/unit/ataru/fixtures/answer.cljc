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
           :value     "020202A0202",
           :fieldType "textField",
           :label     {:fi "Henkilötunnus", :sv "Personnummer"}}
          {:key       "birth-date",
           :value     "02.02.2002",
           :fieldType "textField",
           :label     {:fi "Syntymäaika", :sv "Födelsetid"}}
          {:key       "first-name",
           :value     "Eemil",
           :fieldType "textField",
           :label     {:fi "Etunimet", :sv "Förnamn"}}
          {:key       "postal-code",
           :value     "01234",
           :fieldType "textField",
           :label     {:fi "Postinumero", :sv "Postnummer"}}
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
           :label     {:fi "Kotikunta", :sv "Bostadsort"}}
          {:key       "047da62c-9afe-4e28-bfe8-5b50b21b4277",
           :label     "Ensimmäinen kysymys, toistuvilla arvoilla",
           :value
                      ["Voluptas enim ipsum ut debitis a qui tempor occaecat" "harum laudantium nulla voluptate est in ex sunt ipsum labor"],
           :fieldType "textField"}
          {:key       "c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0",
           :label     "Viides kysymys",
           :value     ["Ensimmäinen vaihtoehto" "Toinen vaihtoehto" "Kolmas vaihtoehto"]
           :fieldType "multipleChoice"}
          {:key       "b05a6057-2c65-40a8-9312-c837429f44bb"
           :fieldType "dropdown"
           :value     ""}]})

