(ns ataru.fixtures.person-info-form)

(def form
  {:id         866,
   :name       {:fi "Uusi lomake torstainaaaaaaa"},
   :created-by "DEVELOPER",
   :content
               [{:id              "1e350fce-aee4-4bde-a47e-1d2eabd49b1b",
                 :label           {:fi "Henkilötiedot", :sv "Personlig information"},
                 :module          "person-info",
                 :params          {},
                 :children
                                  [{:id         "a0bc8de6-a1b7-4974-b853-9d7b018cb656",
                                    :params     {},
                                    :children
                                                [{:id         "first-name",
                                                  :label      {:fi "Etunimet", :sv "Förnamn"},
                                                  :params     {:size "M"},
                                                  :fieldType  "textField",
                                                  :fieldClass "formField",
                                                  :validators ["required"]}
                                                 {:id         "preferred-name",
                                                  :label      {:fi "Kutsumanimi", :sv "Smeknamn"},
                                                  :params     {:size "S"},
                                                  :fieldType  "textField",
                                                  :fieldClass "formField",
                                                  :validators ["required"]}],
                                    :fieldType  "rowcontainer",
                                    :fieldClass "wrapperElement"}
                                   {:id         "last-name",
                                    :label      {:fi "Sukunimi", :sv "Efternamn"},
                                    :params     {:size "M"},
                                    :fieldType  "textField",
                                    :fieldClass "formField",
                                    :validators ["required"]}
                                   {:id         "e872c8d2-e3f4-487a-8a21-65c4436b14bb",
                                    :params     {},
                                    :children
                                                [{:id         "nationality",
                                                  :label      {:fi "Kansalaisuus", :sv "Nationalitet"},
                                                  :rules      {:swap-ssn-birthdate-based-on-nationality ["ssn" "birth-date"]},
                                                  :params     {},
                                                  :options
                                                              [{:label {:fi "", :sv ""}, :value ""}
                                                               {:label {:fi "Afganistan"}, :value "AFG"}
                                                               {:label {:fi "Ahvenanmaa"}, :value "ALA"}
                                                               {:label {:fi "Alankomaat"}, :value "NLD"}
                                                               {:label {:fi "Albania"}, :value "ALB"}
                                                               {:label {:fi "Algeria"}, :value "DZA"}
                                                               {:label {:fi "Amerikan Samoa"}, :value "ASM"}
                                                               {:label {:fi "Andorra"}, :value "AND"}
                                                               {:label {:fi "Angola"}, :value "AGO"}
                                                               {:label {:fi "Anguilla"}, :value "AIA"}
                                                               {:label {:fi "Antarktis"}, :value "ATA"}
                                                               {:label {:fi "Antigua ja Barbuda"}, :value "ATG"}
                                                               {:label {:fi "Arabiemiirikunnat"}, :value "ARE"}
                                                               {:label {:fi "Argentiina"}, :value "ARG"}
                                                               {:label {:fi "Armenia"}, :value "ARM"}
                                                               {:label {:fi "Aruba"}, :value "ABW"}
                                                               {:label {:fi "Australia"}, :value "AUS"}
                                                               {:label {:fi "Azerbaidzan"}, :value "AZE"}
                                                               {:label {:fi "Bahama"}, :value "BHS"}
                                                               {:label {:fi "Bahrain"}, :value "BHR"}
                                                               {:label {:fi "Bangladesh"}, :value "BGD"}
                                                               {:label {:fi "Barbados"}, :value "BRB"}
                                                               {:label {:fi "Belgia"}, :value "BEL"}
                                                               {:label {:fi "Belize"}, :value "BLZ"}
                                                               {:label {:fi "Benin"}, :value "BEN"}
                                                               {:label {:fi "Bermuda"}, :value "BMU"}
                                                               {:label {:fi "Bhutan"}, :value "BTN"}
                                                               {:label {:fi "Bolivia"}, :value "BOL"}
                                                               {:label {:fi "Bonaire,Sint Eustatius ja Saba"}, :value "BES"}
                                                               {:label {:fi "Bosnia ja Hertsegovina"}, :value "BIH"}
                                                               {:label {:fi "Botswana"}, :value "BWA"}
                                                               {:label {:fi "Bouvet&#039;nsaari"}, :value "BVT"}
                                                               {:label {:fi "Brasilia"}, :value "BRA"}
                                                               {:label {:fi "Britannia"}, :value "GBR"}
                                                               {:label {:fi "Brittiläinen Intian valtameren alue"}, :value "IOT"}
                                                               {:label {:fi "Brittiläiset Neitsytsaaret"}, :value "VGB"}
                                                               {:label {:fi "Brunei"}, :value "BRN"}
                                                               {:label {:fi "Bulgaria"}, :value "BGR"}
                                                               {:label {:fi "Burkina Faso"}, :value "BFA"}
                                                               {:label {:fi "Burundi"}, :value "BDI"}
                                                               {:label {:fi "Caymansaaret"}, :value "CYM"}
                                                               {:label {:fi "Chile"}, :value "CHL"}
                                                               {:label {:fi "Cookinsaaret"}, :value "COK"}
                                                               {:label {:fi "Costa Rica"}, :value "CRI"}
                                                               {:label {:fi "Curacao"}, :value "CUW"}
                                                               {:label {:fi "Djibouti"}, :value "DJI"}
                                                               {:label {:fi "Dominica"}, :value "DMA"}
                                                               {:label {:fi "Dominikaaninen tasavalta"}, :value "DOM"}
                                                               {:label {:fi "Ecuador"}, :value "ECU"}
                                                               {:label {:fi "Egypti"}, :value "EGY"}
                                                               {:label {:fi "El Salvador"}, :value "SLV"}
                                                               {:label {:fi "Eritrea"}, :value "ERI"}
                                                               {:label {:fi "Espanja"}, :value "ESP"}
                                                               {:label {:fi "Etelä-Afrikka"}, :value "ZAF"}
                                                               {:label {:fi "Etelä-Georgia ja Eteläiset Sandwichsaaret"}, :value "SGS"}
                                                               {:label {:fi "Etelä-Sudan"}, :value "SSD"}
                                                               {:label {:fi "Etiopia"}, :value "ETH"}
                                                               {:label {:fi "Falklandinsaaret"}, :value "FLK"}
                                                               {:label {:fi "Fidzi"}, :value "FJI"}
                                                               {:label {:fi "Filippiinit"}, :value "PHL"}
                                                               {:label {:fi "Färsaaret"}, :value "FRO"}
                                                               {:label {:fi "Gabon"}, :value "GAB"}
                                                               {:label {:fi "Gambia"}, :value "GMB"}
                                                               {:label {:fi "Georgia"}, :value "GEO"}
                                                               {:label {:fi "Ghana"}, :value "GHA"}
                                                               {:label {:fi "Gibraltar"}, :value "GIB"}
                                                               {:label {:fi "Grenada"}, :value "GRD"}
                                                               {:label {:fi "Grönlanti"}, :value "GRL"}
                                                               {:label {:fi "Guadeloupe"}, :value "GLP"}
                                                               {:label {:fi "Guam"}, :value "GUM"}
                                                               {:label {:fi "Guatemala"}, :value "GTM"}
                                                               {:label {:fi "Guernsey"}, :value "GGY"}
                                                               {:label {:fi "Guinea"}, :value "GIN"}
                                                               {:label {:fi "Guinea-Bissau"}, :value "GNB"}
                                                               {:label {:fi "Guyana"}, :value "GUY"}
                                                               {:label {:fi "Haiti"}, :value "HTI"}
                                                               {:label {:fi "Heard ja McDonaldinsaaret"}, :value "HMD"}
                                                               {:label {:fi "Honduras"}, :value "HND"}
                                                               {:label {:fi "Hongkong"}, :value "HKG"}
                                                               {:label {:fi "Ilman kansalaisuutta"}, :value "YYY"}
                                                               {:label {:fi "Indonesia"}, :value "IDN"}
                                                               {:label {:fi "Intia"}, :value "IND"}
                                                               {:label {:fi "Irak"}, :value "IRQ"}
                                                               {:label {:fi "Iran"}, :value "IRN"}
                                                               {:label {:fi "Irlanti"}, :value "IRL"}
                                                               {:label {:fi "Islanti"}, :value "ISL"}
                                                               {:label {:fi "Israel"}, :value "ISR"}
                                                               {:label {:fi "Italia"}, :value "ITA"}
                                                               {:label {:fi "Itä-Timor"}, :value "TMP"}
                                                               {:label {:fi "Itävalta"}, :value "AUT"}
                                                               {:label {:fi "Jamaika"}, :value "JAM"}
                                                               {:label {:fi "Japani"}, :value "JPN"}
                                                               {:label {:fi "Jemen"}, :value "YEM"}
                                                               {:label {:fi "Jersey"}, :value "JEY"}
                                                               {:label {:fi "Jordania"}, :value "JOR"}
                                                               {:label {:fi "Joulusaari"}, :value "CXR"}
                                                               {:label {:fi "Kambodza"}, :value "KHM"}
                                                               {:label {:fi "Kamerun"}, :value "CMR"}
                                                               {:label {:fi "Kanada"}, :value "CAN"}
                                                               {:label {:fi "Kap Verde"}, :value "CPV"}
                                                               {:label {:fi "Kazakstan"}, :value "KAZ"}
                                                               {:label {:fi "Kenia"}, :value "KEN"}
                                                               {:label {:fi "Keski-Afrikkan tasavalta"}, :value "CAF"}
                                                               {:label {:fi "Kiina"}, :value "CHN"}
                                                               {:label {:fi "Kirgisia"}, :value "KGZ"}
                                                               {:label {:fi "Kiribati"}, :value "KIR"}
                                                               {:label {:fi "Kolumbia"}, :value "COL"}
                                                               {:label {:fi "Komorit"}, :value "COM"}
                                                               {:label {:fi "Kongo (Kongo-Brazzaville)"}, :value "COG"}
                                                               {:label {:fi "Kongo (Kongo-Kinshasa)"}, :value "ZAR"}
                                                               {:label {:fi "Kookossaaret"}, :value "CCK"}
                                                               {:label {:fi "Korean Tasavalta (Etelä-Korea)"}, :value "KOR"}
                                                               {:label {:fi "Korean demokraattinen kansantasavalta (Pohjois-Korea)"}, :value "PRK"}
                                                               {:label {:fi "Kosovo"}, :value "XKK"}
                                                               {:label {:fi "Kreikka"}, :value "GRC"}
                                                               {:label {:fi "Kroatia"}, :value "HRV"}
                                                               {:label {:fi "Kuuba"}, :value "CUB"}
                                                               {:label {:fi "Kuwait"}, :value "KWT"}
                                                               {:label {:fi "Kypros"}, :value "CYP"}
                                                               {:label {:fi "Laos"}, :value "LAO"}
                                                               {:label {:fi "Latvia"}, :value "LVA"}
                                                               {:label {:fi "Lesotho"}, :value "LSO"}
                                                               {:label {:fi "Libanon"}, :value "LBN"}
                                                               {:label {:fi "Liberia"}, :value "LBR"}
                                                               {:label {:fi "Libya"}, :value "LBY"}
                                                               {:label {:fi "Liechtenstein"}, :value "LIE"}
                                                               {:label {:fi "Liettua"}, :value "LTU"}
                                                               {:label {:fi "Luxemburg"}, :value "LUX"}
                                                               {:label {:fi "Länsi-Sahara"}, :value "ESH"}
                                                               {:label {:fi "Macao"}, :value "MAC"}
                                                               {:label {:fi "Madagaskar"}, :value "MDG"}
                                                               {:label {:fi "Makedonia"}, :value "MKD"}
                                                               {:label {:fi "Malawi"}, :value "MWI"}
                                                               {:label {:fi "Malediivit"}, :value "MDV"}
                                                               {:label {:fi "Malesia"}, :value "MYS"}
                                                               {:label {:fi "Mali"}, :value "MLI"}
                                                               {:label {:fi "Malta"}, :value "MLT"}
                                                               {:label {:fi "Mansaari"}, :value "IMN"}
                                                               {:label {:fi "Marokko"}, :value "MAR"}
                                                               {:label {:fi "Marshallinsaaret"}, :value "MHL"}
                                                               {:label {:fi "Martinique"}, :value "MTQ"}
                                                               {:label {:fi "Mauritania"}, :value "MRT"}
                                                               {:label {:fi "Mauritius"}, :value "MUS"}
                                                               {:label {:fi "Mayotte"}, :value "MYT"}
                                                               {:label {:fi "Meksiko"}, :value "MEX"}
                                                               {:label {:fi "Mikronesia"}, :value "FSM"}
                                                               {:label {:fi "Moldova"}, :value "MDA"}
                                                               {:label {:fi "Monaco"}, :value "MCO"}
                                                               {:label {:fi "Mongolia"}, :value "MNG"}
                                                               {:label {:fi "Montenegro"}, :value "MNE"}
                                                               {:label {:fi "Montserrat"}, :value "MSR"}
                                                               {:label {:fi "Mosambik"}, :value "MOZ"}
                                                               {:label {:fi "Myanmar"}, :value "MMR"}
                                                               {:label {:fi "Namibia"}, :value "NAM"}
                                                               {:label {:fi "Nauru"}, :value "NRU"}
                                                               {:label {:fi "Nepal"}, :value "NPL"}
                                                               {:label {:fi "Nicaragua"}, :value "NIC"}
                                                               {:label {:fi "Niger"}, :value "NER"}
                                                               {:label {:fi "Nigeria"}, :value "NGA"}
                                                               {:label {:fi "Niue"}, :value "NIU"}
                                                               {:label {:fi "Norfolkinsaari"}, :value "NFK"}
                                                               {:label {:fi "Norja"}, :value "NOR"}
                                                               {:label {:fi "Norsunluurannikko"}, :value "CIV"}
                                                               {:label {:fi "Oman"}, :value "OMN"}
                                                               {:label {:fi "Pakistan"}, :value "PAK"}
                                                               {:label {:fi "Palau"}, :value "PLW"}
                                                               {:label {:fi "Palestiinan valtio"}, :value "PSE"}
                                                               {:label {:fi "Panama"}, :value "PAN"}
                                                               {:label {:fi "Papua-Uusi-Guinea"}, :value "PNG"}
                                                               {:label {:fi "Paraguay"}, :value "PRY"}
                                                               {:label {:fi "Peru"}, :value "PER"}
                                                               {:label {:fi "Pitcairn"}, :value "PCN"}
                                                               {:label {:fi "Pohjois-Mariaanit"}, :value "MNP"}
                                                               {:label {:fi "Portugali"}, :value "PRT"}
                                                               {:label {:fi "Puerto Rico"}, :value "PRI"}
                                                               {:label {:fi "Puola"}, :value "POL"}
                                                               {:label {:fi "Päiväntasaajan Guinea"}, :value "GNQ"}
                                                               {:label {:fi "Qatar"}, :value "QAT"}
                                                               {:label {:fi "Ranska"}, :value "FRA"}
                                                               {:label {:fi "Ranskan Guayana"}, :value "GUF"}
                                                               {:label {:fi "Ranskan Polynesia"}, :value "PYF"}
                                                               {:label {:fi "Ranskan eteläiset alueet"}, :value "ATF"}
                                                               {:label {:fi "Romania"}, :value "ROM"}
                                                               {:label {:fi "Ruanda"}, :value "RWA"}
                                                               {:label {:fi "Ruotsi"}, :value "SWE"}
                                                               {:label {:fi "Réunion"}, :value "REU"}
                                                               {:label {:fi "Saint Barthélemy"}, :value "BLM"}
                                                               {:label {:fi "Saint Helena"}, :value "SHN"}
                                                               {:label {:fi "Saint Kitts ja Nevis"}, :value "KNA"}
                                                               {:label {:fi "Saint Lucia"}, :value "LCA"}
                                                               {:label {:fi "Saint Martin (Ranska)"}, :value "MAF"}
                                                               {:label {:fi "Saint Vincent ja Grenadiinit"}, :value "VCT"}
                                                               {:label {:fi "Saint-Pierre ja Miquelon"}, :value "SPM"}
                                                               {:label {:fi "Saksa"}, :value "DEU"}
                                                               {:label {:fi "Salomonsaaret"}, :value "SLB"}
                                                               {:label {:fi "Sambia"}, :value "ZMB"}
                                                               {:label {:fi "Samoa"}, :value "WSM"}
                                                               {:label {:fi "San Marino"}, :value "SMR"}
                                                               {:label {:fi "Saudi-Arabia"}, :value "SAU"}
                                                               {:label {:fi "Senegal"}, :value "SEN"}
                                                               {:label {:fi "Serbia"}, :value "SRB"}
                                                               {:label {:fi "Seychellit"}, :value "SYC"}
                                                               {:label {:fi "Sierra Leone"}, :value "SLE"}
                                                               {:label {:fi "Singapore"}, :value "SGP"}
                                                               {:label {:fi "Sint Maarten(Alankomaat)"}, :value "SMX"}
                                                               {:label {:fi "Slovakia"}, :value "SVK"}
                                                               {:label {:fi "Slovenia"}, :value "SVN"}
                                                               {:label {:fi "Somalia"}, :value "SOM"}
                                                               {:label {:fi "Sri Lanka"}, :value "LKA"}
                                                               {:label {:fi "Sudan"}, :value "SDN"}
                                                               {:label {:fi "Suomi"}, :value "FIN", :default-value true}
                                                               {:label {:fi "Suriname"}, :value "SUR"}
                                                               {:label {:fi "Svalbard ja Jan Mayen"}, :value "SJM"}
                                                               {:label {:fi "Sveitsi"}, :value "CHE"}
                                                               {:label {:fi "Swazimaa"}, :value "SWZ"}
                                                               {:label {:fi "Syyria"}, :value "SYR"}
                                                               {:label {:fi "São Tomé ja Príncipe"}, :value "STP"}
                                                               {:label {:fi "Tadzikistan"}, :value "TJK"}
                                                               {:label {:fi "Taiwan"}, :value "TWN"}
                                                               {:label {:fi "Tansania"}, :value "TZA"}
                                                               {:label {:fi "Tanska"}, :value "DNK"}
                                                               {:label {:fi "Thaimaa"}, :value "THA"}
                                                               {:label {:fi "Togo"}, :value "TGO"}
                                                               {:label {:fi "Tokelau"}, :value "TKL"}
                                                               {:label {:fi "Tonga"}, :value "TON"}
                                                               {:label {:fi "Trinidad ja Tobago"}, :value "TTO"}
                                                               {:label {:fi "Tunisia"}, :value "TUN"}
                                                               {:label {:fi "Tuntematon"}, :value "XXX"}
                                                               {:label {:fi "Turkki"}, :value "TUR"}
                                                               {:label {:fi "Turkmenistan"}, :value "TKM"}
                                                               {:label {:fi "Turks- ja Caicossaaret"}, :value "TCA"}
                                                               {:label {:fi "Tuvalu"}, :value "TUV"}
                                                               {:label {:fi "Tšad"}, :value "TCD"}
                                                               {:label {:fi "Tšekki"}, :value "CZE"}
                                                               {:label {:fi "Uganda"}, :value "UGA"}
                                                               {:label {:fi "Ukraina"}, :value "UKR"}
                                                               {:label {:fi "Unkari"}, :value "HUN"}
                                                               {:label {:fi "Uruguay"}, :value "URY"}
                                                               {:label {:fi "Uusi-Kaledonia"}, :value "NCL"}
                                                               {:label {:fi "Uusi-Seelanti"}, :value "NZL"}
                                                               {:label {:fi "Uzbekistan"}, :value "UZB"}
                                                               {:label {:fi "Valko-Venäjä"}, :value "BLR"}
                                                               {:label {:fi "Vanuatu"}, :value "VUT"}
                                                               {:label {:fi "Vatikaani"}, :value "VAT"}
                                                               {:label {:fi "Venezuela"}, :value "VEN"}
                                                               {:label {:fi "Venäjä"}, :value "RUS"}
                                                               {:label {:fi "Vietnam"}, :value "VNM"}
                                                               {:label {:fi "Viro"}, :value "EST"}
                                                               {:label {:fi "Wallis ja Futuna"}, :value "WLF"}
                                                               {:label {:fi "Yhdysvallat (USA)"}, :value "USA"}
                                                               {:label {:fi "Yhdysvaltain Neitsytsaaret"}, :value "VIR"}
                                                               {:label {:fi "Yhdysvaltain pienet erillissaaret"}, :value "UMI"}
                                                               {:label {:fi "Zimbabwe"}, :value "ZWE"}],
                                                  :fieldType  "dropdown",
                                                  :fieldClass "formField",
                                                  :validators ["required"]}
                                                 {:id              "a3199cdf-fba3-4be1-8ab1-760f75f16d54",
                                                  :params          {},
                                                  :children
                                                                   [{:id         "ssn",
                                                                     :label      {:fi "Henkilötunnus", :sv "Personnummer"},
                                                                     :params     {:size "S"},
                                                                     :fieldType  "textField",
                                                                     :fieldClass "formField",
                                                                     :validators ["ssn" "required"]}
                                                                    {:id         "birth-date",
                                                                     :label      {:fi "Syntymäaika", :sv "Födelsedag"},
                                                                     :params     {:size "S"},
                                                                     :fieldType  "textField",
                                                                     :fieldClass "formField",
                                                                     :validators ["past-date" "required"]}],
                                                  :fieldType       "rowcontainer",
                                                  :fieldClass      "wrapperElement",
                                                  :child-validator "one-of"}],
                                    :fieldType  "rowcontainer",
                                    :fieldClass "wrapperElement"}
                                   {:id         "gender",
                                    :label      {:fi "Sukupuoli", :sv "Kön"},
                                    :params     {},
                                    :options
                                                [{:label {:fi "", :sv ""}, :value ""}
                                                 {:label {:fi "Mies", :sv "Människa"}, :value "1"}
                                                 {:label {:fi "Nainen", :sv "Kvinna"}, :value "2"}],
                                    :fieldType  "dropdown",
                                    :fieldClass "formField",
                                    :validators ["required"]}
                                   {:id         "email",
                                    :label      {:fi "Sähköpostiosoite", :sv "E-postadress"},
                                    :params     {:size "M"},
                                    :fieldType  "textField",
                                    :fieldClass "formField",
                                    :validators ["email" "required"]}
                                   {:id         "phone",
                                    :label      {:fi "Matkapuhelin", :sv "Mobiltelefonnummer"},
                                    :params     {:size "M"},
                                    :fieldType  "textField",
                                    :fieldClass "formField",
                                    :validators ["phone" "required"]}
                                   {:id         "address",
                                    :label      {:fi "Katuosoite", :sv "Adress"},
                                    :params     {:size "L"},
                                    :fieldType  "textField",
                                    :fieldClass "formField",
                                    :validators ["required"]}
                                   {:id         "5c311f49-fd7d-47bb-a58e-414ece07e149",
                                    :params     {},
                                    :children
                                                [{:id         "postal-office",
                                                  :label      {:fi "Postitoimipaikka", :sv "Postkontor"},
                                                  :params     {:size "M"},
                                                  :fieldType  "textField",
                                                  :fieldClass "formField",
                                                  :validators ["required"]}
                                                 {:id         "postal-code",
                                                  :label      {:fi "Postinumero", :sv "Postnummer"},
                                                  :params     {:size "S"},
                                                  :fieldType  "textField",
                                                  :fieldClass "formField",
                                                  :validators ["postal-code" "required"]}],
                                    :fieldType  "rowcontainer",
                                    :fieldClass "wrapperElement"}
                                   {:id         "home-town",
                                    :label      {:fi "Kotikunta", :sv "Bostadsort"},
                                    :params     {:size "M"},
                                    :fieldType  "textField",
                                    :fieldClass "formField",
                                    :validators ["required"]}
                                   {:id         "language",
                                    :label      {:fi "Äidinkieli", :sv "Modersmål"},
                                    :params     {},
                                    :options
                                                [{:label {:fi "", :sv ""}, :value ""}
                                                 {:label {:fi "abhaasi"}, :value "ab"}
                                                 {:label {:fi "afar"}, :value "aa"}
                                                 {:label {:fi "afgaani, pasto"}, :value "ps"}
                                                 {:label {:fi "afrikaans"}, :value "af"}
                                                 {:label {:fi "aimara, aymara"}, :value "ay"}
                                                 {:label {:fi "akan"}, :value "ak"}
                                                 {:label {:fi "albania"}, :value "sq"}
                                                 {:label {:fi "ambo, ndonga"}, :value "ng"}
                                                 {:label {:fi "amhara"}, :value "am"}
                                                 {:label {:fi "arabia"}, :value "ar"}
                                                 {:label {:fi "aragonia"}, :value "an"}
                                                 {:label {:fi "armenia"}, :value "hy"}
                                                 {:label {:fi "assami"}, :value "as"}
                                                 {:label {:fi "avaari"}, :value "av"}
                                                 {:label {:fi "avesta"}, :value "ae"}
                                                 {:label {:fi "azeri, azerbaidzani"}, :value "az"}
                                                 {:label {:fi "bambara"}, :value "bm"}
                                                 {:label {:fi "baski, euskera, euskara"}, :value "eu"}
                                                 {:label {:fi "baskiiri"}, :value "ba"}
                                                 {:label {:fi "bengali"}, :value "bn"}
                                                 {:label {:fi "bhutani, dzongkha"}, :value "dz"}
                                                 {:label {:fi "bihari"}, :value "bh"}
                                                 {:label {:fi "bislama"}, :value "bi"}
                                                 {:label {:fi "bosnia"}, :value "bs"}
                                                 {:label {:fi "bretoni"}, :value "br"}
                                                 {:label {:fi "bulgaria"}, :value "bg"}
                                                 {:label {:fi "burma"}, :value "my"}
                                                 {:label {:fi "chamorro"}, :value "ch"}
                                                 {:label {:fi "cree"}, :value "cr"}
                                                 {:label {:fi "divehi, malediivi"}, :value "dv"}
                                                 {:label {:fi "ei virallinen kieli"}, :value "98"}
                                                 {:label {:fi "englanti"}, :value "en"}
                                                 {:label {:fi "eskimo"}, :value "iu"}
                                                 {:label {:fi "espanja"}, :value "es"}
                                                 {:label {:fi "esperanto"}, :value "eo"}
                                                 {:label {:fi "eteländebele"}, :value "nr"}
                                                 {:label {:fi "ewe"}, :value "ee"}
                                                 {:label {:fi "fidzi"}, :value "fj"}
                                                 {:label {:fi "friisi"}, :value "fy"}
                                                 {:label {:fi "fulani, fulfulde"}, :value "ff"}
                                                 {:label {:fi "fääri"}, :value "fo"}
                                                 {:label {:fi "galicia"}, :value "gl"}
                                                 {:label {:fi "galla, afan oromo, oromo"}, :value "om"}
                                                 {:label {:fi "ganda, luganda"}, :value "lg"}
                                                 {:label {:fi "georgia, gruusia"}, :value "ka"}
                                                 {:label {:fi "grönlanti, grönlannineskimo"}, :value "kl"}
                                                 {:label {:fi "guarani"}, :value "gn"}
                                                 {:label {:fi "gudzarati, gujarati"}, :value "gu"}
                                                 {:label {:fi "guernsey"}, :value "gg"}
                                                 {:label {:fi "haiti"}, :value "ht"}
                                                 {:label {:fi "hausa"}, :value "ha"}
                                                 {:label {:fi "heprea, ivrit"}, :value "he"}
                                                 {:label {:fi "herero"}, :value "hz"}
                                                 {:label {:fi "hindi"}, :value "hi"}
                                                 {:label {:fi "hiri-motu"}, :value "ho"}
                                                 {:label {:fi "hollanti"}, :value "nl"}
                                                 {:label {:fi "ido"}, :value "io"}
                                                 {:label {:fi "igbo"}, :value "ig"}
                                                 {:label {:fi "iiri"}, :value "ga"}
                                                 {:label {:fi "indonesia, bahasa indonesia"}, :value "id"}
                                                 {:label {:fi "interlingua"}, :value "ia"}
                                                 {:label {:fi "interlingue"}, :value "ie"}
                                                 {:label {:fi "inupiak"}, :value "ik"}
                                                 {:label {:fi "islanti"}, :value "is"}
                                                 {:label {:fi "italia"}, :value "it"}
                                                 {:label {:fi "jaava"}, :value "jv"}
                                                 {:label {:fi "japani"}, :value "ja"}
                                                 {:label {:fi "jersey"}, :value "je"}
                                                 {:label {:fi "jiddi, jiddis"}, :value "yi"}
                                                 {:label {:fi "joruba"}, :value "yo"}
                                                 {:label {:fi "kannada"}, :value "kn"}
                                                 {:label {:fi "kanuri"}, :value "kr"}
                                                 {:label {:fi "karjala"}, :value "ke"}
                                                 {:label {:fi "kasmiri"}, :value "ks"}
                                                 {:label {:fi "katalaani"}, :value "ca"}
                                                 {:label {:fi "kazakki, kasakki"}, :value "kk"}
                                                 {:label {:fi "ketsua"}, :value "qu"}
                                                 {:label {:fi "khmer, kambodza"}, :value "km"}
                                                 {:label {:fi "kiina"}, :value "zh"}
                                                 {:label {:fi "kikongo, kongo"}, :value "kg"}
                                                 {:label {:fi "kikuju"}, :value "ki"}
                                                 {:label {:fi "kirgiisi"}, :value "ky"}
                                                 {:label {:fi "kirjanorja"}, :value "nb"}
                                                 {:label {:fi "kirkkoslaavi"}, :value "cu"}
                                                 {:label {:fi "komi"}, :value "kv"}
                                                 {:label {:fi "korea"}, :value "ko"}
                                                 {:label {:fi "korni"}, :value "kw"}
                                                 {:label {:fi "korsika"}, :value "co"}
                                                 {:label {:fi "kreikka"}, :value "el"}
                                                 {:label {:fi "kroatia"}, :value "hr"}
                                                 {:label {:fi "kuanjama"}, :value "kj"}
                                                 {:label {:fi "kurdi"}, :value "ku"}
                                                 {:label {:fi "kymri, wales"}, :value "cy"}
                                                 {:label {:fi "lao"}, :value "lo"}
                                                 {:label {:fi "latina"}, :value "la"}
                                                 {:label {:fi "latvia, lätti"}, :value "lv"}
                                                 {:label {:fi "letzeburg, luxemburg"}, :value "lb"}
                                                 {:label {:fi "liettua"}, :value "lt"}
                                                 {:label {:fi "limburg"}, :value "li"}
                                                 {:label {:fi "lingala"}, :value "ln"}
                                                 {:label {:fi "luba-katanga"}, :value "lu"}
                                                 {:label {:fi "makedonia"}, :value "mk"}
                                                 {:label {:fi "malagasi, madagassi"}, :value "mg"}
                                                 {:label {:fi "malaiji"}, :value "ms"}
                                                 {:label {:fi "malajalam"}, :value "ml"}
                                                 {:label {:fi "malta"}, :value "mt"}
                                                 {:label {:fi "manx"}, :value "gv"}
                                                 {:label {:fi "maori"}, :value "mi"}
                                                 {:label {:fi "marathi"}, :value "mr"}
                                                 {:label {:fi "marshallese"}, :value "mh"}
                                                 {:label {:fi "moldavia"}, :value "mo"}
                                                 {:label {:fi "mongoli"}, :value "mn"}
                                                 {:label {:fi "muu kieli"}, :value "xx"}
                                                 {:label {:fi "nauru"}, :value "na"}
                                                 {:label {:fi "navaho"}, :value "nv"}
                                                 {:label {:fi "nepali"}, :value "ne"}
                                                 {:label {:fi "njandza, tseva"}, :value "ny"}
                                                 {:label {:fi "norja"}, :value "no"}
                                                 {:label {:fi "ojibwa"}, :value "oj"}
                                                 {:label {:fi "oksitaani, provensaali"}, :value "oc"}
                                                 {:label {:fi "orija"}, :value "or"}
                                                 {:label {:fi "osseetti"}, :value "os"}
                                                 {:label {:fi "pali"}, :value "pi"}
                                                 {:label {:fi "pandzabi"}, :value "pa"}
                                                 {:label {:fi "persia, nykypersia, farsi"}, :value "fa"}
                                                 {:label {:fi "pohjoisndebele"}, :value "nd"}
                                                 {:label {:fi "portugali"}, :value "pt"}
                                                 {:label {:fi "puola"}, :value "pl"}
                                                 {:label {:fi "ranska"}, :value "fr"}
                                                 {:label {:fi "retoromaani"}, :value "rm"}
                                                 {:label {:fi "romani"}, :value "ri"}
                                                 {:label {:fi "romania"}, :value "ro"}
                                                 {:label {:fi "ruanda, kinjaruanda, njaruanda"}, :value "rw"}
                                                 {:label {:fi "rundi, kirundi"}, :value "rn"}
                                                 {:label {:fi "ruotsi"}, :value "sv"}
                                                 {:label {:fi "saame, lappi"}, :value "se"}
                                                 {:label {:fi "saksa"}, :value "de"}
                                                 {:label {:fi "samoa"}, :value "sm"}
                                                 {:label {:fi "sango"}, :value "sg"}
                                                 {:label {:fi "sanskrit"}, :value "sa"}
                                                 {:label {:fi "sardi"}, :value "sc"}
                                                 {:label {:fi "serbia"}, :value "sr"}
                                                 {:label {:fi "serbokroatia, serbokroatiska"}, :value "sh"}
                                                 {:label {:fi "shona"}, :value "sn"}
                                                 {:label {:fi "sichuan yi"}, :value "ii"}
                                                 {:label {:fi "sindhi"}, :value "sd"}
                                                 {:label {:fi "singali"}, :value "si"}
                                                 {:label {:fi "siswati, swazi"}, :value "ss"}
                                                 {:label {:fi "skotti, gaeli"}, :value "gd"}
                                                 {:label {:fi "slovakki"}, :value "sk"}
                                                 {:label {:fi "sloveeni"}, :value "sl"}
                                                 {:label {:fi "somali"}, :value "so"}
                                                 {:label {:fi "sotho, sesotho"}, :value "st"}
                                                 {:label {:fi "suahili"}, :value "sw"}
                                                 {:label {:fi "sunda"}, :value "su"}
                                                 {:label {:fi "suomi"}, :value "fi", :default-value true}
                                                 {:label {:fi "tadzikki"}, :value "tg"}
                                                 {:label {:fi "tagalog, pilipino"}, :value "tl"}
                                                 {:label {:fi "tahiti"}, :value "ty"}
                                                 {:label {:fi "tamili"}, :value "ta"}
                                                 {:label {:fi "tanska"}, :value "da"}
                                                 {:label {:fi "tataari"}, :value "tt"}
                                                 {:label {:fi "telugu"}, :value "te"}
                                                 {:label {:fi "thai"}, :value "th"}
                                                 {:label {:fi "tigrinja"}, :value "ti"}
                                                 {:label {:fi "tiibet"}, :value "bo"}
                                                 {:label {:fi "tonga"}, :value "to"}
                                                 {:label {:fi "tsekki"}, :value "cs"}
                                                 {:label {:fi "tsetseeni"}, :value "ce"}
                                                 {:label {:fi "tsonga"}, :value "ts"}
                                                 {:label {:fi "tsuang"}, :value "za"}
                                                 {:label {:fi "tsuvassi"}, :value "cv"}
                                                 {:label {:fi "tswana, setswana"}, :value "tn"}
                                                 {:label {:fi "tuntematon"}, :value "99"}
                                                 {:label {:fi "turkki"}, :value "tr"}
                                                 {:label {:fi "turkmeeni"}, :value "tk"}
                                                 {:label {:fi "twi"}, :value "tw"}
                                                 {:label {:fi "uiguuri"}, :value "ug"}
                                                 {:label {:fi "ukraina"}, :value "uk"}
                                                 {:label {:fi "unkari"}, :value "hu"}
                                                 {:label {:fi "urdu"}, :value "ur"}
                                                 {:label {:fi "uusnorja"}, :value "nn"}
                                                 {:label {:fi "uzbekki, usbekki, ösbekki"}, :value "uz"}
                                                 {:label {:fi "valkovenäjä"}, :value "be"}
                                                 {:label {:fi "venda"}, :value "ve"}
                                                 {:label {:fi "venäjä"}, :value "ru"}
                                                 {:label {:fi "vietnam"}, :value "vi"}
                                                 {:label {:fi "viittomakieli"}, :value "vk"}
                                                 {:label {:fi "viro, eesti"}, :value "et"}
                                                 {:label {:fi "volapük"}, :value "vo"}
                                                 {:label {:fi "walloon"}, :value "wa"}
                                                 {:label {:fi "wolof"}, :value "wo"}
                                                 {:label {:fi "xhosa, kafferi, hosa"}, :value "xh"}
                                                 {:label {:fi "zulu"}, :value "zu"}],
                                    :fieldType  "dropdown",
                                    :fieldClass "formField",
                                    :validators ["required"]}


                                   {:id         "047da62c-9afe-4e28-bfe8-5b50b21b4277",
                                    :label      {:fi "Ensimmäinen kysymys, toistuvilla arvoilla", :sv ""},
                                    :params     {:repeatable true},
                                    :fieldType  "textField",
                                    :fieldClass "formField",
                                    :validators []}


                                   {:id         "c8558a1f-86e9-4d76-83eb-a0d7e1fd44b0",
                                    :label      {:fi "Viides kysymys", :sv ""},
                                    :params     {},
                                    :options
                                                [{:label {:fi "Ensimmäinen vaihtoehto", :sv ""}, :value "Ensimmäinen vaihtoehto"}
                                                 {:label {:fi "Toinen vaihtoehto", :sv ""}, :value "Toinen vaihtoehto"}
                                                 {:label {:fi "Kolmas vaihtoehto", :sv ""}, :value "Kolmas vaihtoehto"}
                                                 {:label {:fi "", :sv ""}, :value ""}],
                                    :fieldType  "multipleChoice",
                                    :fieldClass "formField"}

                                   {:id         "b05a6057-2c65-40a8-9312-c837429f44bb",
                                    :label      {:fi "Pohjakoulutus/ylin koulutus/tutkinto, josta on todistus", :sv ""},
                                    :params     {},
                                    :options
                                                [{:label {:fi "Lukio", :sv ""},
                                                  :value "Lukio",
                                                  :followups
                                                         [{:id         "5d8023b1-22c6-4388-8bd4-8e3634fc78ef",
                                                           :label      {:fi "NONIIN", :sv ""},
                                                           :params     {:info-text {:label nil}},
                                                           :fieldType  "textField",
                                                           :fieldClass "formField"}]}
                                                 {:label {:fi "Ammatillinen peruskoulu", :sv ""},
                                                  :value "Ammatillinen peruskoulu",
                                                  :followups
                                                         [{:id         "fbe3522d-6f1d-4e05-85e3-4e716146c686",
                                                           :label      {:fi "Ois yks kysymys vielä", :sv ""},
                                                           :params     {},
                                                           :fieldType  "textField",
                                                           :fieldClass "formField"}]}
                                                 {:label     {:fi "Ammattitutkinto", :sv ""},
                                                  :value     "Ammattitutkinto",
                                                  :followups [{:id "62d37b52-3237-4f7f-9e78-df373b0b5c79", :label {:fi "", :sv ""}, :params {}, :fieldType "textArea", :fieldClass "formField"}]}
                                                 {:label {:fi "Erikoisammattitutkinto", :sv ""}, :value "Erikoisammattitutkinto"}
                                                 {:label {:fi "Opisto/ammattikorkeakoulu", :sv ""}, :value "Opisto/ammattikorkeakoulu"}
                                                 {:label {:fi "Yliopisto", :sv ""}, :value "Yliopisto"}],
                                    :fieldType  "dropdown",
                                    :fieldClass "formField"}
                                   ],
                 :fieldType       "fieldset",
                 :fieldClass      "wrapperElement",
                 :label-amendment {:fi "(Osio lisätään automaattisesti lomakkeelle)", :sv "Partitionen automatiskt lägga formen"}}]})
