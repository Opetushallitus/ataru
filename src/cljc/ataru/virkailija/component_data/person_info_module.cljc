  (ns ataru.virkailija.component-data.person-info-module
    (:require [ataru.virkailija.component-data.component :as component]
              [clojure.walk]))

(defn ^:private text-field
  [labels & {:keys [size id validators] :or {size "M" validators []}}]
  (-> (component/text-field)
      (assoc :label labels)
      (assoc :validators (conj validators "required"))
      (assoc-in [:params :size] size)
      (assoc :id id)))

(defn ^:private first-name-component
  []
  (text-field {:fi "Etunimet" :sv "Förnamn"} :id :first-name))

(defn ^:private preferred-name-component
  []
  (text-field {:fi "Kutsumanimi" :sv "Smeknamn"} :size "S" :id :preferred-name))

(defn ^:private first-name-section
  []
  (component/row-section [(first-name-component)
                          (preferred-name-component)]))

(defn ^:private last-name-component
  []
  (text-field {:fi "Sukunimi" :sv "Efternamn"} :id :last-name))

(defn ^:private dropdown-option
  [value labels]
  {:value value :label labels})

(defn ^:private nationality-component
  []
  (merge (component/dropdown) {:label {:fi "Kansalaisuus" :sv "Nationalitet"}
                               :validators ["required"]
                               :options [(dropdown-option "AFG" {:fi "Afganistan"})
                                         (dropdown-option "ALA" {:fi "Ahvenanmaa"})
                                         (dropdown-option "NLD" {:fi "Alankomaat"})
                                         (dropdown-option "ALB" {:fi "Albania"})
                                         (dropdown-option "DZA" {:fi "Algeria"})
                                         (dropdown-option "ASM" {:fi "Amerikan Samoa"})
                                         (dropdown-option "AND" {:fi "Andorra"})
                                         (dropdown-option "AGO" {:fi "Angola"})
                                         (dropdown-option "AIA" {:fi "Anguilla"})
                                         (dropdown-option "ATA" {:fi "Antarktis"})
                                         (dropdown-option "ATG" {:fi "Antigua ja Barbuda"})
                                         (dropdown-option "ARE" {:fi "Arabiemiirikunnat"})
                                         (dropdown-option "ARG" {:fi "Argentiina"})
                                         (dropdown-option "ARM" {:fi "Armenia"})
                                         (dropdown-option "ABW" {:fi "Aruba"})
                                         (dropdown-option "AUS" {:fi "Australia"})
                                         (dropdown-option "AZE" {:fi "Azerbaidzan"})
                                         (dropdown-option "BHS" {:fi "Bahama"})
                                         (dropdown-option "BHR" {:fi "Bahrain"})
                                         (dropdown-option "BGD" {:fi "Bangladesh"})
                                         (dropdown-option "BRB" {:fi "Barbados"})
                                         (dropdown-option "BEL" {:fi "Belgia"})
                                         (dropdown-option "BLZ" {:fi "Belize"})
                                         (dropdown-option "BEN" {:fi "Benin"})
                                         (dropdown-option "BMU" {:fi "Bermuda"})
                                         (dropdown-option "BTN" {:fi "Bhutan"})
                                         (dropdown-option "BOL" {:fi "Bolivia"})
                                         (dropdown-option "BES" {:fi "Bonaire,Sint Eustatius ja Saba"})
                                         (dropdown-option "BIH" {:fi "Bosnia ja Hertsegovina"})
                                         (dropdown-option "BWA" {:fi "Botswana"})
                                         (dropdown-option "BVT" {:fi "Bouvet&#039;nsaari"})
                                         (dropdown-option "BRA" {:fi "Brasilia"})
                                         (dropdown-option "GBR" {:fi "Britannia"})
                                         (dropdown-option "IOT" {:fi "Brittiläinen Intian valtameren alue"})
                                         (dropdown-option "VGB" {:fi "Brittiläiset Neitsytsaaret"})
                                         (dropdown-option "BRN" {:fi "Brunei"})
                                         (dropdown-option "BGR" {:fi "Bulgaria"})
                                         (dropdown-option "BFA" {:fi "Burkina Faso"})
                                         (dropdown-option "BDI" {:fi "Burundi"})
                                         (dropdown-option "CYM" {:fi "Caymansaaret"})
                                         (dropdown-option "CHL" {:fi "Chile"})
                                         (dropdown-option "COK" {:fi "Cookinsaaret"})
                                         (dropdown-option "CRI" {:fi "Costa Rica"})
                                         (dropdown-option "CUW" {:fi "Curacao"})
                                         (dropdown-option "DJI" {:fi "Djibouti"})
                                         (dropdown-option "DMA" {:fi "Dominica"})
                                         (dropdown-option "DOM" {:fi "Dominikaaninen tasavalta"})
                                         (dropdown-option "ECU" {:fi "Ecuador"})
                                         (dropdown-option "EGY" {:fi "Egypti"})
                                         (dropdown-option "SLV" {:fi "El Salvador"})
                                         (dropdown-option "ERI" {:fi "Eritrea"})
                                         (dropdown-option "ESP" {:fi "Espanja"})
                                         (dropdown-option "ZAF" {:fi "Etelä-Afrikka"})
                                         (dropdown-option "SGS" {:fi "Etelä-Georgia ja Eteläiset Sandwichsaaret"})
                                         (dropdown-option "SSD" {:fi "Etelä-Sudan"})
                                         (dropdown-option "ETH" {:fi "Etiopia"})
                                         (dropdown-option "FLK" {:fi "Falklandinsaaret"})
                                         (dropdown-option "FJI" {:fi "Fidzi"})
                                         (dropdown-option "PHL" {:fi "Filippiinit"})
                                         (dropdown-option "FRO" {:fi "Färsaaret"})
                                         (dropdown-option "GAB" {:fi "Gabon"})
                                         (dropdown-option "GMB" {:fi "Gambia"})
                                         (dropdown-option "GEO" {:fi "Georgia"})
                                         (dropdown-option "GHA" {:fi "Ghana"})
                                         (dropdown-option "GIB" {:fi "Gibraltar"})
                                         (dropdown-option "GRD" {:fi "Grenada"})
                                         (dropdown-option "GRL" {:fi "Grönlanti"})
                                         (dropdown-option "GLP" {:fi "Guadeloupe"})
                                         (dropdown-option "GUM" {:fi "Guam"})
                                         (dropdown-option "GTM" {:fi "Guatemala"})
                                         (dropdown-option "GGY" {:fi "Guernsey"})
                                         (dropdown-option "GIN" {:fi "Guinea"})
                                         (dropdown-option "GNB" {:fi "Guinea-Bissau"})
                                         (dropdown-option "GUY" {:fi "Guyana"})
                                         (dropdown-option "HTI" {:fi "Haiti"})
                                         (dropdown-option "HMD" {:fi "Heard ja McDonaldinsaaret"})
                                         (dropdown-option "HND" {:fi "Honduras"})
                                         (dropdown-option "HKG" {:fi "Hongkong"})
                                         (dropdown-option "YYY" {:fi "Ilman kansalaisuutta"})
                                         (dropdown-option "IDN" {:fi "Indonesia"})
                                         (dropdown-option "IND" {:fi "Intia"})
                                         (dropdown-option "IRQ" {:fi "Irak"})
                                         (dropdown-option "IRN" {:fi "Iran"})
                                         (dropdown-option "IRL" {:fi "Irlanti"})
                                         (dropdown-option "ISL" {:fi "Islanti"})
                                         (dropdown-option "ISR" {:fi "Israel"})
                                         (dropdown-option "ITA" {:fi "Italia"})
                                         (dropdown-option "TMP" {:fi "Itä-Timor"})
                                         (dropdown-option "AUT" {:fi "Itävalta"})
                                         (dropdown-option "JAM" {:fi "Jamaika"})
                                         (dropdown-option "JPN" {:fi "Japani"})
                                         (dropdown-option "YEM" {:fi "Jemen"})
                                         (dropdown-option "JEY" {:fi "Jersey"})
                                         (dropdown-option "JOR" {:fi "Jordania"})
                                         (dropdown-option "CXR" {:fi "Joulusaari"})
                                         (dropdown-option "KHM" {:fi "Kambodza"})
                                         (dropdown-option "CMR" {:fi "Kamerun"})
                                         (dropdown-option "CAN" {:fi "Kanada"})
                                         (dropdown-option "CPV" {:fi "Kap Verde"})
                                         (dropdown-option "KAZ" {:fi "Kazakstan"})
                                         (dropdown-option "KEN" {:fi "Kenia"})
                                         (dropdown-option "CAF" {:fi "Keski-Afrikkan tasavalta"})
                                         (dropdown-option "CHN" {:fi "Kiina"})
                                         (dropdown-option "KGZ" {:fi "Kirgisia"})
                                         (dropdown-option "KIR" {:fi "Kiribati"})
                                         (dropdown-option "COL" {:fi "Kolumbia"})
                                         (dropdown-option "COM" {:fi "Komorit"})
                                         (dropdown-option "COG" {:fi "Kongo (Kongo-Brazzaville)"})
                                         (dropdown-option "ZAR" {:fi "Kongo (Kongo-Kinshasa)"})
                                         (dropdown-option "CCK" {:fi "Kookossaaret"})
                                         (dropdown-option "KOR" {:fi "Korean Tasavalta (Etelä-Korea)"})
                                         (dropdown-option "PRK" {:fi "Korean demokraattinen kansantasavalta (Pohjois-Korea)"})
                                         (dropdown-option "XKK" {:fi "Kosovo"})
                                         (dropdown-option "GRC" {:fi "Kreikka"})
                                         (dropdown-option "HRV" {:fi "Kroatia"})
                                         (dropdown-option "CUB" {:fi "Kuuba"})
                                         (dropdown-option "KWT" {:fi "Kuwait"})
                                         (dropdown-option "CYP" {:fi "Kypros"})
                                         (dropdown-option "LAO" {:fi "Laos"})
                                         (dropdown-option "LVA" {:fi "Latvia"})
                                         (dropdown-option "LSO" {:fi "Lesotho"})
                                         (dropdown-option "LBN" {:fi "Libanon"})
                                         (dropdown-option "LBR" {:fi "Liberia"})
                                         (dropdown-option "LBY" {:fi "Libya"})
                                         (dropdown-option "LIE" {:fi "Liechtenstein"})
                                         (dropdown-option "LTU" {:fi "Liettua"})
                                         (dropdown-option "LUX" {:fi "Luxemburg"})
                                         (dropdown-option "ESH" {:fi "Länsi-Sahara"})
                                         (dropdown-option "MAC" {:fi "Macao"})
                                         (dropdown-option "MDG" {:fi "Madagaskar"})
                                         (dropdown-option "MKD" {:fi "Makedonia"})
                                         (dropdown-option "MWI" {:fi "Malawi"})
                                         (dropdown-option "MDV" {:fi "Malediivit"})
                                         (dropdown-option "MYS" {:fi "Malesia"})
                                         (dropdown-option "MLI" {:fi "Mali"})
                                         (dropdown-option "MLT" {:fi "Malta"})
                                         (dropdown-option "IMN" {:fi "Mansaari"})
                                         (dropdown-option "MAR" {:fi "Marokko"})
                                         (dropdown-option "MHL" {:fi "Marshallinsaaret"})
                                         (dropdown-option "MTQ" {:fi "Martinique"})
                                         (dropdown-option "MRT" {:fi "Mauritania"})
                                         (dropdown-option "MUS" {:fi "Mauritius"})
                                         (dropdown-option "MYT" {:fi "Mayotte"})
                                         (dropdown-option "MEX" {:fi "Meksiko"})
                                         (dropdown-option "FSM" {:fi "Mikronesia"})
                                         (dropdown-option "MDA" {:fi "Moldova"})
                                         (dropdown-option "MCO" {:fi "Monaco"})
                                         (dropdown-option "MNG" {:fi "Mongolia"})
                                         (dropdown-option "MNE" {:fi "Montenegro"})
                                         (dropdown-option "MSR" {:fi "Montserrat"})
                                         (dropdown-option "MOZ" {:fi "Mosambik"})
                                         (dropdown-option "MMR" {:fi "Myanmar"})
                                         (dropdown-option "NAM" {:fi "Namibia"})
                                         (dropdown-option "NRU" {:fi "Nauru"})
                                         (dropdown-option "NPL" {:fi "Nepal"})
                                         (dropdown-option "NIC" {:fi "Nicaragua"})
                                         (dropdown-option "NER" {:fi "Niger"})
                                         (dropdown-option "NGA" {:fi "Nigeria"})
                                         (dropdown-option "NIU" {:fi "Niue"})
                                         (dropdown-option "NFK" {:fi "Norfolkinsaari"})
                                         (dropdown-option "NOR" {:fi "Norja"})
                                         (dropdown-option "CIV" {:fi "Norsunluurannikko"})
                                         (dropdown-option "OMN" {:fi "Oman"})
                                         (dropdown-option "PAK" {:fi "Pakistan"})
                                         (dropdown-option "PLW" {:fi "Palau"})
                                         (dropdown-option "PSE" {:fi "Palestiinan valtio"})
                                         (dropdown-option "PAN" {:fi "Panama"})
                                         (dropdown-option "PNG" {:fi "Papua-Uusi-Guinea"})
                                         (dropdown-option "PRY" {:fi "Paraguay"})
                                         (dropdown-option "PER" {:fi "Peru"})
                                         (dropdown-option "PCN" {:fi "Pitcairn"})
                                         (dropdown-option "MNP" {:fi "Pohjois-Mariaanit"})
                                         (dropdown-option "PRT" {:fi "Portugali"})
                                         (dropdown-option "PRI" {:fi "Puerto Rico"})
                                         (dropdown-option "POL" {:fi "Puola"})
                                         (dropdown-option "GNQ" {:fi "Päiväntasaajan Guinea"})
                                         (dropdown-option "QAT" {:fi "Qatar"})
                                         (dropdown-option "FRA" {:fi "Ranska"})
                                         (dropdown-option "GUF" {:fi "Ranskan Guayana"})
                                         (dropdown-option "PYF" {:fi "Ranskan Polynesia"})
                                         (dropdown-option "ATF" {:fi "Ranskan eteläiset alueet"})
                                         (dropdown-option "ROM" {:fi "Romania"})
                                         (dropdown-option "RWA" {:fi "Ruanda"})
                                         (dropdown-option "SWE" {:fi "Ruotsi"})
                                         (dropdown-option "REU" {:fi "Réunion"})
                                         (dropdown-option "BLM" {:fi "Saint Barthélemy"})
                                         (dropdown-option "SHN" {:fi "Saint Helena"})
                                         (dropdown-option "KNA" {:fi "Saint Kitts ja Nevis"})
                                         (dropdown-option "LCA" {:fi "Saint Lucia"})
                                         (dropdown-option "MAF" {:fi "Saint Martin (Ranska)"})
                                         (dropdown-option "VCT" {:fi "Saint Vincent ja Grenadiinit"})
                                         (dropdown-option "SPM" {:fi "Saint-Pierre ja Miquelon"})
                                         (dropdown-option "DEU" {:fi "Saksa"})
                                         (dropdown-option "SLB" {:fi "Salomonsaaret"})
                                         (dropdown-option "ZMB" {:fi "Sambia"})
                                         (dropdown-option "WSM" {:fi "Samoa"})
                                         (dropdown-option "SMR" {:fi "San Marino"})
                                         (dropdown-option "SAU" {:fi "Saudi-Arabia"})
                                         (dropdown-option "SEN" {:fi "Senegal"})
                                         (dropdown-option "SRB" {:fi "Serbia"})
                                         (dropdown-option "SYC" {:fi "Seychellit"})
                                         (dropdown-option "SLE" {:fi "Sierra Leone"})
                                         (dropdown-option "SGP" {:fi "Singapore"})
                                         (dropdown-option "SMX" {:fi "Sint Maarten(Alankomaat)"})
                                         (dropdown-option "SVK" {:fi "Slovakia"})
                                         (dropdown-option "SVN" {:fi "Slovenia"})
                                         (dropdown-option "SOM" {:fi "Somalia"})
                                         (dropdown-option "LKA" {:fi "Sri Lanka"})
                                         (dropdown-option "SDN" {:fi "Sudan"})
                                         (dropdown-option "FIN" {:fi "Suomi"})
                                         (dropdown-option "SUR" {:fi "Suriname"})
                                         (dropdown-option "SJM" {:fi "Svalbard ja Jan Mayen"})
                                         (dropdown-option "CHE" {:fi "Sveitsi"})
                                         (dropdown-option "SWZ" {:fi "Swazimaa"})
                                         (dropdown-option "SYR" {:fi "Syyria"})
                                         (dropdown-option "STP" {:fi "São Tomé ja Príncipe"})
                                         (dropdown-option "TJK" {:fi "Tadzikistan"})
                                         (dropdown-option "TWN" {:fi "Taiwan"})
                                         (dropdown-option "TZA" {:fi "Tansania"})
                                         (dropdown-option "DNK" {:fi "Tanska"})
                                         (dropdown-option "THA" {:fi "Thaimaa"})
                                         (dropdown-option "TGO" {:fi "Togo"})
                                         (dropdown-option "TKL" {:fi "Tokelau"})
                                         (dropdown-option "TON" {:fi "Tonga"})
                                         (dropdown-option "TTO" {:fi "Trinidad ja Tobago"})
                                         (dropdown-option "TUN" {:fi "Tunisia"})
                                         (dropdown-option "XXX" {:fi "Tuntematon"})
                                         (dropdown-option "TUR" {:fi "Turkki"})
                                         (dropdown-option "TKM" {:fi "Turkmenistan"})
                                         (dropdown-option "TCA" {:fi "Turks- ja Caicossaaret"})
                                         (dropdown-option "TUV" {:fi "Tuvalu"})
                                         (dropdown-option "TCD" {:fi "Tšad"})
                                         (dropdown-option "CZE" {:fi "Tšekki"})
                                         (dropdown-option "UGA" {:fi "Uganda"})
                                         (dropdown-option "UKR" {:fi "Ukraina"})
                                         (dropdown-option "HUN" {:fi "Unkari"})
                                         (dropdown-option "URY" {:fi "Uruguay"})
                                         (dropdown-option "NCL" {:fi "Uusi-Kaledonia"})
                                         (dropdown-option "NZL" {:fi "Uusi-Seelanti"})
                                         (dropdown-option "UZB" {:fi "Uzbekistan"})
                                         (dropdown-option "BLR" {:fi "Valko-Venäjä"})
                                         (dropdown-option "VUT" {:fi "Vanuatu"})
                                         (dropdown-option "VAT" {:fi "Vatikaani"})
                                         (dropdown-option "VEN" {:fi "Venezuela"})
                                         (dropdown-option "RUS" {:fi "Venäjä"})
                                         (dropdown-option "VNM" {:fi "Vietnam"})
                                         (dropdown-option "EST" {:fi "Viro"})
                                         (dropdown-option "WLF" {:fi "Wallis ja Futuna"})
                                         (dropdown-option "USA" {:fi "Yhdysvallat (USA)"})
                                         (dropdown-option "VIR" {:fi "Yhdysvaltain Neitsytsaaret"})
                                         (dropdown-option "UMI" {:fi "Yhdysvaltain pienet erillissaaret"})
                                         (dropdown-option "ZWE" {:fi "Zimbabwe"})]
                               :id :nationality}))

(defn ^:private ssn-component
  []
  (text-field {:fi "Henkilötunnus" :sv "Personnummer"} :size "S" :id :ssn :validators ["ssn"]))

(defn ^:private identification-section
  []
  (component/row-section [(nationality-component)
                          (ssn-component)]))

(defn ^:private gender-section
  []
  (merge (component/dropdown) {:label {:fi "Sukupuoli" :sv "Kön"}
                               :validators ["required"]
                               :options [(dropdown-option "male" {:fi "Mies" :sv "Människa"})
                                         (dropdown-option "female" {:fi "Nainen" :sv "Kvinna"})]
                               :id :gender}))

(defn ^:private email-component
  []
  (text-field {:fi "Sähköpostiosoite" :sv "E-postadress"} :id :email :validators ["email"]))

(defn ^:private phone-component
  []
  (text-field {:fi "Matkapuhelin" :sv "Mobiltelefonnummer"} :id :phone :validators ["phone"]))

(defn ^:private street-address-component
  []
  (text-field {:fi "Katuosoite" :sv "Adress"} :size "L" :id :address))

(defn ^:private home-town-component
  []
  (text-field {:fi "Kotikunta" :sv "Bostadsort"} :id :home-town))

(defn ^:private postal-code-component
  []
  (text-field {:fi "Postinumero" :sv "Postnummer"} :size "S" :id :postal-code :validators ["postal-code"]))

(defn ^:private postal-office-component
  []
  (text-field {:fi "Postitoimipaikka" :sv "Postkontor"} :id :postal-office))

(defn ^:private postal-office-section
  []
  (component/row-section [(postal-office-component)
                          (postal-code-component)]))

(defn ^:private native-language-section
  []
  (merge (component/dropdown) {:label {:fi "Äidinkieli" :sv "Modersmål"}
                               :validators ["required"]
                               :options [(dropdown-option "fi" {:fi "suomi" :sv "finska"})
                                         (dropdown-option "sv" {:fi "ruotsi" :sv "svenska"})]
                               :id :language}))

(defn person-info-module
  []
  (clojure.walk/prewalk
    (fn [x]
      (if (map? x)
        (dissoc x :focus?)
        x))
    (merge (component/form-section) {:label {:fi "Henkilötiedot"
                                             :sv "Personlig information"}
                                     :children [(first-name-section)
                                                (last-name-component)
                                                (identification-section)
                                                (gender-section)
                                                (email-component)
                                                (phone-component)
                                                (street-address-component)
                                                (postal-office-section)
                                                (home-town-component)
                                                (native-language-section)]
                                     :module :person-info})))
