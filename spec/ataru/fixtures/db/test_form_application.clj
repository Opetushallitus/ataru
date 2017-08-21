(ns ataru.fixtures.db.test-form-application)

(def test-form-application
  {:id           4,
   :key          "751d204a-5094-46d4-9da5-6a16231719f7",
   :lang         "fi",
   :form         8,
   :person-oid   nil,
   :secret       "i3DjpQqqIcK9KKpbPQbj9i0DKt5Rce2qqp1Uzjo8mvaghg",
   :answers      [{:key       "329f186a-92d6-46e6-a60f-874bb7940a74",
                   :label     "Jatkokysymys C",
                   :value     ["C1" "C2"],
                   :fieldType "textField"}
                  {:key "b4b29b8d-ed9b-45c3-ac2a-dd88c6ca4212", :label "Viimeinen kysymys", :value "", :fieldType "dropdown"}
                  {:key "address", :label "Katuosoite", :value "Katutie 12 B", :fieldType "textField"}
                  {:key       "83614490-0345-44a7-aa3a-18cf72f5aef1",
                   :label     "Jatkokysymys A",
                   :value     ["A1" "A2"],
                   :fieldType "textField"}
                  {:key "email", :label "Sähköpostiosoite", :value "test@example.com", :fieldType "textField"}
                  {:key "preferred-name", :label "Kutsumanimi", :value "Etunimi", :fieldType "textField"}
                  {:key "last-name", :label "Sukunimi", :value "Sukunimi", :fieldType "textField"}
                  {:key "country-of-residence", :label "Asuinmaa", :value "246", :fieldType "dropdown"}
                  {:key       "dbca8687-e8ff-4d21-b306-3b927018080f",
                   :label     "Päätason pudotusvalikko",
                   :value     "Pudotusvalikon 1. kysymys",
                   :fieldType "dropdown"}
                  {:key       "2c3c05f5-e088-49ca-9853-71e77cbfafa8",
                   :label     "Lyhyen listan kysymys",
                   :value     "Ensimmäinen vaihtoehto",
                   :fieldType "singleChoice"}
                  {:key       "5f85651c-94b8-42da-95c8-b374defbf916",
                   :label     "Jatkokysymys B",
                   :value     ["B1" ""],
                   :fieldType "textField"}
                  {:key       "7762041c-51ba-4a61-9891-bb24cb2409ca",
                   :label     "Ensimmäinen kysymys toistuvilla arvoilla",
                   :value     ["Toistuva vastaus 1" "Toistuva vastaus 2" "Toistuva vastaus 3"],
                   :fieldType "textField"}
                  {:key "phone", :label "Matkapuhelin", :value "0123456789", :fieldType "textField"}
                  {:key       "216bb100-97f7-47f9-a1f7-51763e99ff5f",
                   :label     "Toinen kysymys",
                   :value     "Pakollisen tekstialueen vastaus",
                   :fieldType "textArea"}
                  {:key "nationality", :label "Kansalaisuus", :value "246", :fieldType "dropdown"}
                  {:key "ssn", :label "Henkilötunnus", :value "020202A0202", :fieldType "textField"}
                  {:key       "def3efb6-46ce-4983-a288-9e39528aff4d",
                   :label     "Tekstikenttä 2",
                   :value     ["Oikea vierekkäinen"],
                   :fieldType "textField"}
                  {:key       "5956a500-1a29-4904-877f-f5f049516434",
                   :label     "Viides kysymys",
                   :value     ["Toinen vaihtoehto"],
                   :fieldType "multipleChoice"}
                  {:key       "d3a065f0-daf7-4599-a5b4-d50ab2254e43",
                   :label     "Oletko punavihervärisokea?",
                   :value     "En",
                   :fieldType "singleChoice"}
                  {:key       "7a6568a7-3ad5-4ac3-a9cb-7b09404e1063",
                   :label     "Monivalinta jatkokysymyksenä",
                   :value     ["Jatkokysymys A" "Jatkokysymys B"],
                   :fieldType "multipleChoice"}
                  {:key "first-name", :label "Etunimet", :value "Etunimi Tokanimi", :fieldType "textField"}
                  {:key "birth-date", :label "Syntymäaika", :value "02.02.2002", :fieldType "textField"}
                  {:key "postal-code", :label "Postinumero", :value "40100", :fieldType "textField"}
                  {:key       "b9f0a526-38ca-4560-aee4-aee03c6ca4e4",
                   :label     "Jatkokysymys",
                   :value     "Jatkokysymyksen vastaus",
                   :fieldType "textField"}
                  {:key       "b00638c6-8b47-4fb4-b790-c424ffb283b5",
                   :label     "Ensimmäinen kysymys",
                   :value     "Tekstikentän vastaus",
                   :fieldType "textField"}
                  {:key       "d55aa422-4bed-4a05-aa35-b07b5179324b",
                   :label     "Tekstikenttä 1",
                   :value     ["Vasen vierekkäinen"],
                   :fieldType "textField"}
                  {:key       "3bf5392b-7f27-4edc-a695-71d80deb8d70",
                   :label     "Kolmas kysymys",
                   :value     "Kolmas vaihtoehto",
                   :fieldType "dropdown"}
                  {:key       "727a5848-751c-4d17-aab4-2d2145621791",
                   :label     "Jatkokysymys A",
                   :value     ["A1" "A2"],
                   :fieldType "textField"}
                  {:key "language", :label "Äidinkieli", :value "FI", :fieldType "dropdown"}
                  {:key       "d758edeb-9391-4bc4-95ee-4f1d83c4cd5b",
                   :label     "Jatkokysymys C",
                   :value     ["C1" "C2"],
                   :fieldType "textField"}
                  {:key       "89642a00-df35-4c72-8181-badd5ea2046d",
                   :label     "Jatkokysymys B",
                   :value     ["B1" ""],
                   :fieldType "textField"}
                  {:key       "a4d77f64-7324-4fcb-9f34-4e3e8c1e27d8",
                   :label     "Kuudes kysymys",
                   :value     ["139"],
                   :fieldType "multipleChoice"}
                  {:key       "a4a03d55-ebdd-45ab-b292-1e9a0ed3a209",
                   :label     "Jatkokysymys A",
                   :value     ["A1" "A2"],
                   :fieldType "textField"}
                  {:key       "7192099b-07c6-4529-8735-7fe126f732f6",
                   :label     "Jatkokysymys B",
                   :value     ["B1" ""],
                   :fieldType "textField"}
                  {:key "gender", :label "Sukupuoli", :value "2", :fieldType "dropdown"}
                  {:key "postal-office", :label "Postitoimipaikka", :value "JYVÄSKYLÄ", :fieldType "textField"}
                  {:key "home-town", :label "Kotikunta", :value "Jyväskylä", :fieldType "textField"}
                  {:key "6bac2c85-7868-4697-8d42-bb8b9c85b8ef", :label "Neljäs kysymys", :value "120", :fieldType "dropdown"}
                  {:key       "960cfe2a-50d6-442f-82a6-64cdc532e181",
                   :label     "Osiokysymys",
                   :value     "Toisen pakollisen tekstialueen vastaus",
                   :fieldType "textArea"}
                  {:key       "ab5b249f-ae4f-4072-b238-6bca577687fc",
                   :label     "Jatkokysymys C",
                   :value     ["C1" "C2"],
                   :fieldType "textField"}]})
