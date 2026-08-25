(ns ataru.koodisto.koodisto-codes)

(def finland-country-code "246")    ; as defined in maatjavaltiot2
(def aland-country-code "248")      ; Åland Islands (Ahvenanmaa) — Finnish autonomous region, same exemption as Finland

(def finland-equivalent-country-codes
  "Country codes exempt from KK application fee on the same grounds as Finnish citizenship."
  #{finland-country-code aland-country-code})

(def institution-type-codes ["21"   ; AMMATILLINEN_OPPILAITOS
                             "22"   ; AMMATILLINEN_ERITYISOPPILAITOS
                             "23"   ; AMMATILLINEN_ERIKOISOPPILAITOS
                             "24"   ; AMMATILLINEN_AIKUISKOULUTUSKESKUS
                             "28"   ; PALO_POLIISI_VARTIOINTI_OPPILAITOS
                             "29"   ; SOTILASALAN_OPPILAITOS
                             "61"   ; LIIKUNNAN_KOULUTUSKEKUS
                             "62"   ; MUSIIKKIOPPILAITOS
                             "63"   ; KANSANOPISTO
                             "xx"]) ; EI_TIEDOSSA
