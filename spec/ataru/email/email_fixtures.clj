(ns ataru.email.email-fixtures)

(def application
  {:haku       "1.2.246.562.29.00000000000000010643",
   :key        "1.2.246.562.11.00000000000000891149",
   :secret     "rvbYXUzNTrf2wOsnjWJicn2rALVbGucLrR_87hOlA-zzZg",
   :lang       "fi",
   :id         1448760,
   :hakukohde
               ["1.2.246.562.20.00000000000000010644"
                "1.2.246.562.20.00000000000000010645"
                "1.2.246.562.20.00000000000000010646"],
   :answers
               [{:key       "address",
                 :value     "Mannerheimintie 15",
                 :fieldType "textField"}
                {:key                               "birth-date",
                 :value                             "29.09.1969",
                 :fieldType                         "textField",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "country-of-residence",
                 :value                             "246",
                 :fieldType                         "dropdown",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "email",
                 :value                             "tiina@testaaja.fi",
                 :fieldType                         "textField",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "first-name",
                 :value                             "Tiina",
                 :fieldType                         "textField",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "gender",
                 :value                             "1",
                 :fieldType                         "dropdown",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "harkinnanvaraisuus",
                 :value                             nil,
                 :fieldType                         "singleChoice",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "harkinnanvaraisuus_1.2.246.562.20.00000000000000010644",
                 :value                             "1",
                 :fieldType                         "singleChoice",
                 :original-followup                 nil,
                 :original-question                 "harkinnanvaraisuus",
                 :duplikoitu-kysymys-hakukohde-oid
                                                    "1.2.246.562.20.00000000000000010644",
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "harkinnanvaraisuus_1.2.246.562.20.00000000000000010645",
                 :value                             "1",
                 :fieldType                         "singleChoice",
                 :original-followup                 nil,
                 :original-question                 "harkinnanvaraisuus",
                 :duplikoitu-kysymys-hakukohde-oid
                                                    "1.2.246.562.20.00000000000000010645",
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "harkinnanvaraisuus_1.2.246.562.20.00000000000000010646",
                 :value                             "0",
                 :fieldType                         "singleChoice",
                 :original-followup                 nil,
                 :original-question                 "harkinnanvaraisuus",
                 :duplikoitu-kysymys-hakukohde-oid
                                                    "1.2.246.562.20.00000000000000010646",
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key
                                                   "harkinnanvaraisuus-reason_1.2.246.562.20.00000000000000010644",
                 :value                            "0",
                 :fieldType                        "singleChoice",
                 :original-followup                "harkinnanvaraisuus-reason",
                 :original-question                nil,
                 :duplikoitu-kysymys-hakukohde-oid nil,
                 :duplikoitu-followup-hakukohde-oid
                                                   "1.2.246.562.20.00000000000000010644"}
                {:key
                                                   "harkinnanvaraisuus-reason_1.2.246.562.20.00000000000000010645",
                 :value                            "3",
                 :fieldType                        "singleChoice",
                 :original-followup                "harkinnanvaraisuus-reason",
                 :original-question                nil,
                 :duplikoitu-kysymys-hakukohde-oid nil,
                 :duplikoitu-followup-hakukohde-oid
                                                   "1.2.246.562.20.00000000000000010645"}
                {:key                               "home-town",
                 :value                             "018",
                 :fieldType                         "dropdown",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "language",
                 :value                             "FI",
                 :fieldType                         "dropdown",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "last-name",
                 :value                             "Testaaja",
                 :fieldType                         "textField",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "phone",
                 :value                             "03213213123",
                 :fieldType                         "textField",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "postal-code",
                 :value                             "00100",
                 :fieldType                         "textField",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "postal-office",
                 :value                             "HELSINKI",
                 :fieldType                         "textField",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "preferred-name",
                 :value                             "Tiina",
                 :fieldType                         "textField",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "ssn",
                 :value                             "290969-527S",
                 :fieldType                         "textField",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "21f2bf53-0b8e-47c6-8716-fe04884c24f8",
                 :value                             [],
                 :fieldType                         "attachment",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key
                                                    "21f2bf53-0b8e-47c6-8716-fe04884c24f8_1.2.246.562.20.00000000000000010644",
                 :value                             [],
                 :fieldType                         "attachment",
                 :original-followup                 nil,
                 :original-question                 "21f2bf53-0b8e-47c6-8716-fe04884c24f8",
                 :duplikoitu-kysymys-hakukohde-oid
                                                    "1.2.246.562.20.00000000000000010644",
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key
                                                    "21f2bf53-0b8e-47c6-8716-fe04884c24f8_1.2.246.562.20.00000000000000010645",
                 :value                             [],
                 :fieldType                         "attachment",
                 :original-followup                 nil,
                 :original-question                 "21f2bf53-0b8e-47c6-8716-fe04884c24f8",
                 :duplikoitu-kysymys-hakukohde-oid
                                                    "1.2.246.562.20.00000000000000010645",
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key
                                                    "21f2bf53-0b8e-47c6-8716-fe04884c24f8_1.2.246.562.20.00000000000000010646",
                 :value                             [],
                 :fieldType                         "attachment",
                 :original-followup                 nil,
                 :original-question                 "21f2bf53-0b8e-47c6-8716-fe04884c24f8",
                 :duplikoitu-kysymys-hakukohde-oid
                                                    "1.2.246.562.20.00000000000000010646",
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "8cd33167-a5df-4ab3-ac57-0790deb5a898",
                 :value                             [],
                 :fieldType                         "attachment",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key
                                                    "8cd33167-a5df-4ab3-ac57-0790deb5a898_1.2.246.562.20.00000000000000010644",
                 :value                             [],
                 :fieldType                         "attachment",
                 :original-followup                 nil,
                 :original-question                 "8cd33167-a5df-4ab3-ac57-0790deb5a898",
                 :duplikoitu-kysymys-hakukohde-oid
                                                    "1.2.246.562.20.00000000000000010644",
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key
                                                    "8cd33167-a5df-4ab3-ac57-0790deb5a898_1.2.246.562.20.00000000000000010645",
                 :value                             [],
                 :fieldType                         "attachment",
                 :original-followup                 nil,
                 :original-question                 "8cd33167-a5df-4ab3-ac57-0790deb5a898",
                 :duplikoitu-kysymys-hakukohde-oid
                                                    "1.2.246.562.20.00000000000000010645",
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key
                                                    "8cd33167-a5df-4ab3-ac57-0790deb5a898_1.2.246.562.20.00000000000000010646",
                 :value                             [],
                 :fieldType                         "attachment",
                 :original-followup                 nil,
                 :original-question                 "8cd33167-a5df-4ab3-ac57-0790deb5a898",
                 :duplikoitu-kysymys-hakukohde-oid
                                                    "1.2.246.562.20.00000000000000010646",
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key
                                                   "8fc03b55-5c87-4f98-8107-97cca8380335_1.2.246.562.20.00000000000000010644",
                 :value                            [],
                 :fieldType                        "attachment",
                 :original-followup                "8fc03b55-5c87-4f98-8107-97cca8380335",
                 :original-question                nil,
                 :duplikoitu-kysymys-hakukohde-oid nil,
                 :duplikoitu-followup-hakukohde-oid
                                                   "1.2.246.562.20.00000000000000010644"}
                {:key                              "8fc03b55-5c87-4f98-8107-97cca8380335_1.2.246.562.20.00000000000000010645",
                 :value                            [],
                 :fieldType                        "attachment",
                 :original-followup                "8fc03b55-5c87-4f98-8107-97cca8380335",
                 :original-question                nil,
                 :duplikoitu-kysymys-hakukohde-oid nil,
                 :duplikoitu-followup-hakukohde-oid "1.2.246.562.20.00000000000000010645"}
                {:key                               "98655824-bb9d-4f4a-a1e5-4e39bd0f61f0",
                 :value                             [],
                 :fieldType                         "attachment",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "9b00783c-5f4e-4ef9-bca4-c2e57b443d3c",
                 :value                             [],
                 :fieldType                         "attachment",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "hakukohteet",
                 :value
                                                    ["1.2.246.562.20.00000000000000010644"
                                                     "1.2.246.562.20.00000000000000010645"
                                                     "1.2.246.562.20.00000000000000010646"],
                 :fieldType                         "hakukohteet",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}
                {:key                               "nationality",
                 :value                             [["246"]],
                 :fieldType                         "dropdown",
                 :original-followup                 nil,
                 :original-question                 nil,
                 :duplikoitu-kysymys-hakukohde-oid  nil,
                 :duplikoitu-followup-hakukohde-oid nil}],
   :person-oid nil,
   :form       787276}
  )

(def tarjonta-info
  {:haku-oid                         "1.2.246.562.29.00000000000000010643",
   :prioritize-hakukohteet           false,
   :yhteishaku                       true,
   :hakuaika
                                     {:start                               1638309600000,
                                      :end                                 1645999200000,
                                      :on                                  true,
                                      :attachment-modify-grace-period-days nil,
                                      :jatkuva-haku?                       false,
                                      :joustava-haku?                      false,
                                      :jatkuva-or-joustava-haku?           false,
                                      :hakukierros-end                     1640987999000,
                                      :label
                                                                           {:start
                                                                            {:fi "1.12.2021 klo 00:00",
                                                                             :sv "1.12.2021 kl. 00:00 EET",
                                                                             :en "Dec. 1, 2021 at 12:00 AM EET"},
                                                                            :end
                                                                            {:fi "28.2.2022 klo 00:00",
                                                                             :sv "28.2.2022 kl. 00:00 EET",
                                                                             :en "Feb. 28, 2022 at 12:00 AM EET"},
                                                                            :end-time
                                                                            {:fi "klo 00:00", :sv "kl. 00:00 EET", :en "at 12:00 AM EET"}}},
   :haku-name
                                     {:en "Riston liitetestihaku In English",
                                      :fi "Riston liitetestihaku",
                                      :sv "Riston liitetestihaku På Svenska"},
   :hakukohteet
                                     [{:can-be-applied-to?                                          true,
                                       :hakukohderyhmat                                             ["1.2.246.562.28.53448852193"],
                                       :liitteet-onko-sama-toimitusosoite?                          true,
                                       :koulutustyyppikoodi                                         "koulutustyyppi_26",
                                       :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja? false,
                                       :liitteet-onko-sama-toimitusaika?                            true,
                                       :koulutukset
                                                                                                    [{:oid                  "1.2.246.562.17.00000000000000004281",
                                                                                                      :koulutuskoodi-name   {},
                                                                                                      :koulutusohjelma-name {},
                                                                                                      :tutkintonimike-names []}],
                                       :form-key                                                    "ecc2a9c6-8051-41bf-a06b-c13f3cad8c18",
                                       :name
                                                                                                    {:fi "Elintarvikealan perustutkinto",
                                                                                                     :sv "Grundexamen inom livsmedelsbranschen"},
                                       :kohdejoukko-korkeakoulu?                                    false,
                                       :hakuaika
                                                                                                    {:start                               1638309600000,
                                                                                                     :end                                 1645999200000,
                                                                                                     :on                                  true,
                                                                                                     :attachment-modify-grace-period-days nil,
                                                                                                     :jatkuva-haku?                       false,
                                                                                                     :joustava-haku?                      false,
                                                                                                     :jatkuva-or-joustava-haku?           false,
                                                                                                     :hakukierros-end                     1640987999000,
                                                                                                     :label
                                                                                                                                          {:start
                                                                                                                                           {:fi "1.12.2021 klo 00:00",
                                                                                                                                            :sv "1.12.2021 kl. 00:00 EET",
                                                                                                                                            :en "Dec. 1, 2021 at 12:00 AM EET"},
                                                                                                                                           :end
                                                                                                                                           {:fi "28.2.2022 klo 00:00",
                                                                                                                                            :sv "28.2.2022 kl. 00:00 EET",
                                                                                                                                            :en "Feb. 28, 2022 at 12:00 AM EET"},
                                                                                                                                           :end-time
                                                                                                                                           {:fi "klo 00:00", :sv "kl. 00:00 EET", :en "at 12:00 AM EET"}}},
                                       :applicable-base-educations                                  [],
                                       :oid                                                         "1.2.246.562.20.00000000000000010644",
                                       :liitteet
                                                                                                    [{:tyyppi               "liitetyypitamm_6#1",
                                                                                                      :toimitusaika         nil,
                                                                                                      :toimitetaan-erikseen false,
                                                                                                      :toimitusosoite       {:osoite nil, :postinumero nil, :verkkosivu nil}}
                                                                                                     {:tyyppi               "liitetyypitamm_7#1",
                                                                                                      :toimitusaika         nil,
                                                                                                      :toimitetaan-erikseen false,
                                                                                                      :toimitusosoite
                                                                                                                            {:osoite nil, :postinumero nil, :verkkosivu nil}}],
                                       :liitteiden-toimitusosoite
                                                                                                    {:osoite     {:fi "Toimisto\nElintie 5"},
                                                                                                     :postinumero
                                                                                                                 {:fi {:koodiUri "posti_00100#2", :nimi "HELSINKI"},
                                                                                                                  :sv {:koodiUri "posti_00100#2", :nimi "HELSINGFORS"}},
                                                                                                     :verkkosivu "https://elintie-liite.fi"},
                                       :liitteiden-toimitusaika
                                                                                                    {:fi "31.1.2022 klo 12:00",
                                                                                                     :sv "31.1.2022 kl. 12:00 EET",
                                                                                                     :en "Jan. 31, 2022 at 12:00 PM EET"},
                                       :tarjoaja-name
                                                                                                    {:fi "Stadin ammatti- ja aikuisopisto, Hattulantien toimipaikka"}}
                                      {:can-be-applied-to?                                          true,
                                       :hakukohderyhmat                                             ["1.2.246.562.28.53448852193"],
                                       :liitteet-onko-sama-toimitusosoite?                          false,
                                       :koulutustyyppikoodi                                         "koulutustyyppi_26",
                                       :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja? false,
                                       :liitteet-onko-sama-toimitusaika?                            false,
                                       :koulutukset
                                                                                                    [{:oid                  "1.2.246.562.17.00000000000000004326",
                                                                                                      :koulutuskoodi-name   {},
                                                                                                      :koulutusohjelma-name {},
                                                                                                      :tutkintonimike-names []}],
                                       :form-key                                                    "ecc2a9c6-8051-41bf-a06b-c13f3cad8c18",
                                       :name
                                                                                                    {:sv "Grundexamen i tandteknik",
                                                                                                     :fi "Hammastekniikan perustutkinto"},
                                       :kohdejoukko-korkeakoulu?                                    false,
                                       :hakuaika
                                                                                                    {:start                               1638309600000,
                                                                                                     :end                                 1645999200000,
                                                                                                     :on                                  true,
                                                                                                     :attachment-modify-grace-period-days nil,
                                                                                                     :jatkuva-haku?                       false,
                                                                                                     :joustava-haku?                      false,
                                                                                                     :jatkuva-or-joustava-haku?           false,
                                                                                                     :hakukierros-end                     1640987999000,
                                                                                                     :label
                                                                                                                                          {:start
                                                                                                                                           {:fi "1.12.2021 klo 00:00",
                                                                                                                                            :sv "1.12.2021 kl. 00:00 EET",
                                                                                                                                            :en "Dec. 1, 2021 at 12:00 AM EET"},
                                                                                                                                           :end
                                                                                                                                           {:fi "28.2.2022 klo 00:00",
                                                                                                                                            :sv "28.2.2022 kl. 00:00 EET",
                                                                                                                                            :en "Feb. 28, 2022 at 12:00 AM EET"},
                                                                                                                                           :end-time
                                                                                                                                           {:fi "klo 00:00", :sv "kl. 00:00 EET", :en "at 12:00 AM EET"}}},
                                       :applicable-base-educations                                  [],
                                       :oid                                                         "1.2.246.562.20.00000000000000010645",
                                       :liitteet
                                                                                                    [{:tyyppi               "liitetyypitamm_6#1",
                                                                                                      :toimitusaika
                                                                                                                            {:fi "31.1.2022 klo 00:00",
                                                                                                                             :sv "31.1.2022 kl. 00:00 EET",
                                                                                                                             :en "Jan. 31, 2022 at 12:00 AM EET"},
                                                                                                      :toimitetaan-erikseen true,
                                                                                                      :toimitusosoite
                                                                                                                            {:osoite     {:fi "Hammasportti 500", :sv "Hammasportti 500 (SV)"},
                                                                                                                             :postinumero {:fi {:koodiUri "posti_01600#2", :nimi "VANTAA"},
                                                                                                                                           :sv {:koodiUri "posti_01600#2", :nimi "VANDA"}},
                                                                                                                             :verkkosivu "https://kauniit_puhtaat_hampaat-liitteena.fi"}}],
                                       :liitteiden-toimitusosoite                                   nil,
                                       :liitteiden-toimitusaika                                     nil,
                                       :tarjoaja-name
                                                                                                    {:fi "Stadin ammatti- ja aikuisopisto, Vilppulantien toimipaikka"}}
                                      {:can-be-applied-to?                                          true,
                                       :hakukohderyhmat                                             ["1.2.246.562.28.53448852193"],
                                       :liitteet-onko-sama-toimitusosoite?                          false,
                                       :koulutustyyppikoodi                                         "koulutustyyppi_26",
                                       :jos-ylioppilastutkinto-ei-muita-pohjakoulutusliitepyyntoja? false,
                                       :liitteet-onko-sama-toimitusaika?                            false,
                                       :koulutukset
                                                                                                    [{:oid                  "1.2.246.562.17.00000000000000005557",
                                                                                                      :koulutuskoodi-name   {},
                                                                                                      :koulutusohjelma-name {},
                                                                                                      :tutkintonimike-names []}],
                                       :form-key                                                    "ecc2a9c6-8051-41bf-a06b-c13f3cad8c18",
                                       :name
                                                                                                    {:sv "Grundexamen inom hår- och skönhetsbranschen",
                                                                                                     :fi "Hius- ja kauneudenhoitoalan perustutkinto"},
                                       :kohdejoukko-korkeakoulu?                                    false,
                                       :hakuaika
                                                                                                    {:start                               1638309600000,
                                                                                                     :end                                 1645999200000,
                                                                                                     :on                                  true,
                                                                                                     :attachment-modify-grace-period-days nil,
                                                                                                     :jatkuva-haku?                       false,
                                                                                                     :joustava-haku?                      false,
                                                                                                     :jatkuva-or-joustava-haku?           false,
                                                                                                     :hakukierros-end                     1640987999000,
                                                                                                     :label
                                                                                                                                          {:start
                                                                                                                                           {:fi "1.12.2021 klo 00:00",
                                                                                                                                            :sv "1.12.2021 kl. 00:00 EET",
                                                                                                                                            :en "Dec. 1, 2021 at 12:00 AM EET"},
                                                                                                                                           :end
                                                                                                                                           {:fi "28.2.2022 klo 00:00",
                                                                                                                                            :sv "28.2.2022 kl. 00:00 EET",
                                                                                                                                            :en "Feb. 28, 2022 at 12:00 AM EET"},
                                                                                                                                           :end-time
                                                                                                                                           {:fi "klo 00:00", :sv "kl. 00:00 EET", :en "at 12:00 AM EET"}}},
                                       :applicable-base-educations                                  [],
                                       :oid                                                         "1.2.246.562.20.00000000000000010646",
                                       :liitteet
                                                                                                    [{:tyyppi               "liitetyypitamm_6#1",
                                                                                                      :toimitusaika
                                                                                                                            {:fi "31.3.2022 klo 00:00",
                                                                                                                             :sv "31.3.2022 kl. 00:00 EEST",
                                                                                                                             :en "Mar. 31, 2022 at 12:00 AM EEST"},
                                                                                                      :toimitetaan-erikseen true,
                                                                                                      :toimitusosoite
                                                                                                                            {:osoite     {:fi "Hiushalkojantie 4"},
                                                                                                                             :postinumero
                                                                                                                                         {:fi {:koodiUri "posti_00100#2", :nimi "HELSINKI"},
                                                                                                                                          :sv {:koodiUri "posti_00100#2", :nimi "HELSINGFORS"}},
                                                                                                                             :verkkosivu "https://liite-hius.fi"}}
                                                                                                     {:tyyppi               "liitetyypitamm_7#1",
                                                                                                      :toimitusaika
                                                                                                                            {:fi "28.2.2022 klo 00:00",
                                                                                                                             :sv "28.2.2022 kl. 00:00 EET",
                                                                                                                             :en "Feb. 28, 2022 at 12:00 AM EET"},
                                                                                                      :toimitetaan-erikseen true,
                                                                                                      :toimitusosoite
                                                                                                                            {:osoite     {:fi "Hiuskatu 2"},
                                                                                                                             :postinumero
                                                                                                                                         {:fi {:koodiUri "posti_00500#2", :nimi "HELSINKI"},
                                                                                                                                          :sv {:koodiUri "posti_00500#2", :nimi "HELSINGFORS"}},
                                                                                                                             :verkkosivu "https://tupee-liitteena.fi"}}],
                                       :liitteiden-toimitusosoite                                   nil,
                                       :liitteiden-toimitusaika                                     nil,
                                       :tarjoaja-name
                                                                                                    {:fi "Stadin ammatti- ja aikuisopisto, Sturenkadun toimipaikka"}}],
   :kohdejoukko-uri                  "haunkohdejoukko_11#1",
   :max-hakukohteet                  nil,
   :can-submit-multiple-applications false})

(def form
  {:properties       {},
   :deleted          nil,
   :key              "ecc2a9c6-8051-41bf-a06b-c13f3cad8c18",
   :content
                     [{:params                         {},
                       :validators                     ["hakukohteet"],
                       :fieldClass                     "formField",
                       :label
                                                       {:en "Application options", :fi "Hakukohteet", :sv "Ansökningsmål"},
                       :id                             "hakukohteet",
                       :options                        [],
                       :exclude-from-answers-if-hidden true,
                       :metadata
                                                       {:created-by
                                                        {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                        :modified-by
                                                        {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                       :fieldType                      "hakukohteet"}
                      {:children
                                   [{:id         "4d7d930d-563b-4f2c-95e8-314b15824b43",
                                     :params     {},
                                     :children
                                                 [{:params     {:size "M"},
                                                   :rules      {},
                                                   :blur-rules {:prefill-preferred-first-name "main-first-name"},
                                                   :validators ["required"],
                                                   :fieldClass "formField",
                                                   :label      {:en "First/given names", :fi "Etunimet", :sv "Förnamn"},
                                                   :id         "first-name",
                                                   :metadata
                                                               {:created-by
                                                                {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                                :modified-by
                                                                {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                                   :fieldType  "textField"}
                                                  {:id         "preferred-name",
                                                   :label
                                                               {:en "Preferred first/given name",
                                                                :fi "Kutsumanimi",
                                                                :sv "Tilltalsnamn"},
                                                   :rules      {},
                                                   :params     {:size "S"},
                                                   :metadata
                                                               {:created-by
                                                                {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                                :modified-by
                                                                {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                                   :fieldType  "textField",
                                                   :fieldClass "formField",
                                                   :validators ["required" "main-first-name"]}],
                                     :metadata
                                                 {:created-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                  :modified-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType  "rowcontainer",
                                     :fieldClass "wrapperElement"}
                                    {:id         "last-name",
                                     :label      {:en "Last name", :fi "Sukunimi", :sv "Efternamn"},
                                     :rules      {},
                                     :params     {:size "M"},
                                     :metadata
                                                 {:created-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                  :modified-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType  "textField",
                                     :fieldClass "formField",
                                     :validators ["required"]}
                                    {:id         "6ef318e4-8b20-45e1-8434-be80924f3a41",
                                     :label      {:fi "", :sv ""},
                                     :params     {},
                                     :children
                                                 [{:params     {},
                                                   :koodisto-source
                                                               {:uri "maatjavaltiot2", :version 2, :default-option "Suomi"},
                                                   :rules      {:toggle-ssn-based-fields nil},
                                                   :validators ["required"],
                                                   :fieldClass "formField",
                                                   :label
                                                               {:en "Nationality", :fi "Kansalaisuus", :sv "Medborgarskap"},
                                                   :id         "nationality",
                                                   :options    [],
                                                   :metadata
                                                               {:created-by
                                                                {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                                :modified-by
                                                                {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                                   :fieldType  "dropdown"}],
                                     :metadata
                                                 {:created-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                  :modified-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType  "fieldset",
                                     :fieldClass "questionGroup"}
                                    {:params               {},
                                     :rules                {:toggle-ssn-based-fields nil},
                                     :fieldClass           "formField",
                                     :label
                                                           {:en "Do you have a Finnish personal identity code?",
                                                            :fi "Onko sinulla suomalainen henkilötunnus?",
                                                            :sv "Har du en finländsk personbeteckning?"},
                                     :id                   "have-finnish-ssn",
                                     :exclude-from-answers true,
                                     :no-blank-option      true,
                                     :options
                                                           [{:label         {:en "Yes", :fi "Kyllä", :sv "Ja"},
                                                             :value         "true",
                                                             :default-value true}
                                                            {:label {:en "No", :fi "Ei", :sv "Nej"}, :value "false"}],
                                     :metadata
                                                           {:created-by
                                                            {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                            :modified-by
                                                            {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType            "dropdown"}
                                    {:id              "f145636f-d9f3-44db-af2f-7db62a9af5ca",
                                     :params          {},
                                     :children
                                                      [{:id         "ssn",
                                                        :label
                                                                    {:en "Finnish personal identity code",
                                                                     :fi "Henkilötunnus",
                                                                     :sv "Personbeteckning"},
                                                        :rules      {:toggle-ssn-based-fields nil},
                                                        :params     {:size "S"},
                                                        :metadata
                                                                    {:created-by
                                                                     {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                                     :modified-by
                                                                     {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                                        :fieldType  "textField",
                                                        :fieldClass "formField",
                                                        :validators ["ssn" "required"]}
                                                       {:id         "d23e3a0e-f6a6-4d4d-87e8-5e1c283413bb",
                                                        :params     {},
                                                        :children
                                                                    [{:id         "birth-date",
                                                                      :label
                                                                                  {:en "Date of birth", :fi "Syntymäaika", :sv "Födelsetid"},
                                                                      :rules      {:toggle-birthdate-based-fields nil},
                                                                      :params
                                                                                  {:size "S",
                                                                                   :placeholder
                                                                                         {:en "dd.mm.yyyy", :fi "pp.kk.vvvv", :sv "dd.mm.åååå"}},
                                                                      :metadata
                                                                                  {:created-by
                                                                                   {:oid  "system",
                                                                                    :date "1970-01-01T00:00:00Z",
                                                                                    :name "system"},
                                                                                   :modified-by
                                                                                   {:oid  "system",
                                                                                    :date "1970-01-01T00:00:00Z",
                                                                                    :name "system"}},
                                                                      :fieldType  "textField",
                                                                      :fieldClass "formField",
                                                                      :validators ["past-date" "required"]}
                                                                     {:id              "gender",
                                                                      :label           {:en "Gender", :fi "Sukupuoli", :sv "Kön"},
                                                                      :params          {},
                                                                      :metadata
                                                                                       {:created-by
                                                                                        {:oid  "system",
                                                                                         :date "1970-01-01T00:00:00Z",
                                                                                         :name "system"},
                                                                                        :modified-by
                                                                                        {:oid  "system",
                                                                                         :date "1970-01-01T00:00:00Z",
                                                                                         :name "system"}},
                                                                      :fieldType       "dropdown",
                                                                      :fieldClass      "formField",
                                                                      :validators      ["required"],
                                                                      :koodisto-source {:uri "sukupuoli", :version 1}}],
                                                        :metadata
                                                                    {:created-by
                                                                     {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                                     :modified-by
                                                                     {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                                        :fieldType  "rowcontainer",
                                                        :fieldClass "wrapperElement"}],
                                     :metadata
                                                      {:created-by
                                                       {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                       :modified-by
                                                       {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType       "rowcontainer",
                                     :fieldClass      "wrapperElement",
                                     :child-validator "birthdate-and-gender-component"}
                                    {:id         "birthplace",
                                     :label
                                                 {:en "Place and country of birth",
                                                  :fi "Syntymäpaikka ja -maa",
                                                  :sv "Födelseort och -land"},
                                     :rules      {},
                                     :params     {:size "M"},
                                     :metadata
                                                 {:created-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                  :modified-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType  "textField",
                                     :fieldClass "formField",
                                     :validators ["birthplace"]}
                                    {:id         "passport-number",
                                     :label
                                                 {:en "Passport number", :fi "Passin numero", :sv "Passnummer"},
                                     :rules      {},
                                     :params     {:size "M"},
                                     :metadata
                                                 {:created-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                  :modified-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType  "textField",
                                     :fieldClass "formField",
                                     :validators []}
                                    {:id         "national-id-number",
                                     :label
                                                 {:en "National ID number",
                                                  :fi "Kansallinen ID-tunnus",
                                                  :sv "Nationellt ID-signum"},
                                     :rules      {},
                                     :params     {:size "M"},
                                     :metadata
                                                 {:created-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                  :modified-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType  "textField",
                                     :fieldClass "formField",
                                     :validators []}
                                    {:id     "email",
                                     :label
                                     {:en "E-mail address",
                                      :fi "Sähköpostiosoite",
                                      :sv "E-postadress"},
                                     :rules  {},
                                     :params {:size "M"},
                                     :metadata
                                     {:created-by
                                      {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                      :modified-by
                                      {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType "textField",
                                     :fieldClass "formField",
                                     :validators ["required" "email"]}
                                    {:id         "phone",
                                     :label
                                                 {:en "Mobile phone number",
                                                  :fi "Matkapuhelin",
                                                  :sv "Mobiltelefonnummer"},
                                     :rules      {},
                                     :params     {:size "M"},
                                     :metadata
                                                 {:created-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                  :modified-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType  "textField",
                                     :fieldClass "formField",
                                     :validators ["required" "phone"]}
                                    {:params     {},
                                     :koodisto-source
                                                 {:uri "maatjavaltiot2", :version 2, :default-option "Suomi"},
                                     :rules      {:change-country-of-residence nil},
                                     :validators ["required"],
                                     :fieldClass "formField",
                                     :label
                                                 {:en "Country of residence",
                                                  :fi "Asuinmaa",
                                                  :sv "Bosättningsland"},
                                     :id         "country-of-residence",
                                     :metadata
                                                 {:created-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                  :modified-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType  "dropdown"}
                                    {:id         "address",
                                     :label      {:en "Address", :fi "Katuosoite", :sv "Näradress"},
                                     :rules      {},
                                     :params     {:size "M"},
                                     :metadata
                                                 {:created-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                  :modified-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType  "textField",
                                     :fieldClass "formField",
                                     :validators ["required"]}
                                    {:id         "d2711779-010a-413d-99ba-81fc7e1889f7",
                                     :params     {},
                                     :children
                                                 [{:id         "postal-code",
                                                   :label      {:en "Postal code", :fi "Postinumero", :sv "Postnummer"},
                                                   :rules
                                                               {:select-postal-office-based-on-postal-code "postal-office"},
                                                   :params     {:size "S"},
                                                   :metadata
                                                               {:created-by
                                                                {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                                :modified-by
                                                                {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                                   :fieldType  "textField",
                                                   :fieldClass "formField",
                                                   :validators ["postal-code"]}
                                                  {:params                         {:size "M"},
                                                   :rules                          {},
                                                   :validators                     ["postal-office"],
                                                   :fieldClass                     "formField",
                                                   :label
                                                                                   {:en "Town/city", :fi "Postitoimipaikka", :sv "Postkontor"},
                                                   :id                             "postal-office",
                                                   :exclude-from-answers-if-hidden true,
                                                   :metadata
                                                                                   {:created-by
                                                                                    {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                                                    :modified-by
                                                                                    {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                                   :fieldType                      "textField"}],
                                     :metadata
                                                 {:created-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                  :modified-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType  "rowcontainer",
                                     :fieldClass "wrapperElement"}
                                    {:params                         {},
                                     :koodisto-source                {:uri "kunta", :version 1},
                                     :validators                     ["home-town"],
                                     :fieldClass                     "formField",
                                     :label
                                                                     {:en "Municipality of residence",
                                                                      :fi "Kotikunta",
                                                                      :sv "Hemkommun"},
                                     :id                             "home-town",
                                     :exclude-from-answers-if-hidden true,
                                     :metadata
                                                                     {:created-by
                                                                      {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                                      :modified-by
                                                                      {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType                      "dropdown"}
                                    {:params                         {:size "M"},
                                     :rules                          {},
                                     :validators                     ["city"],
                                     :fieldClass                     "formField",
                                     :label
                                                                     {:en "City and country",
                                                                      :fi "Kaupunki ja maa",
                                                                      :sv "Stad och land"},
                                     :id                             "city",
                                     :exclude-from-answers-if-hidden true,
                                     :metadata
                                                                     {:created-by
                                                                      {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                                      :modified-by
                                                                      {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType                      "textField"}
                                    {:params     {},
                                     :koodisto-source
                                                 {:uri "kieli", :version 1, :default-option "suomi"},
                                     :rules
                                                 {:toggle-arvosanat-module-aidinkieli-ja-kirjallisuus-oppiaineet
                                                  nil},
                                     :validators ["required"],
                                     :fieldClass "formField",
                                     :label      {:en "Native language", :fi "Äidinkieli", :sv "Modersmål"},
                                     :id         "language",
                                     :metadata
                                                 {:created-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                                  :modified-by
                                                  {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                                     :fieldType  "dropdown"}],
                       :params     {},
                       :module     "person-info",
                       :fieldClass "wrapperElement",
                       :label
                                   {:en "Personal information",
                                    :fi "Henkilötiedot",
                                    :sv "Personuppgifter"},
                       :id         "onr",
                       :label-amendment
                                   {:en "The section will be automatically added to the application",
                                    :fi "(Osio lisätään automaattisesti lomakkeelle)",
                                    :sv "Denna del införs automatiskt i blanketten"},
                       :metadata
                                   {:created-by
                                    {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"},
                                    :modified-by
                                    {:oid "system", :date "1970-01-01T00:00:00Z", :name "system"}},
                       :fieldType  "fieldset"}
                      {:params
                                                  {:deadline               "30.5.2022 13:05",
                                                   :info-text
                                                                           {:value    {:en "Hint", :fi "Ohjeteksti", :sv "Information"},
                                                                            :enabled? true},
                                                   :attachment-type        "liitetyypitamm_7",
                                                   :mail-attachment?       true,
                                                   :fetch-info-from-kouta? true},
                       :belongs-to-hakukohderyhma ["1.2.246.562.28.53448852193"],
                       :validators                [],
                       :fieldClass                "formField",
                       :per-hakukohde             true,
                       :label                     {:en "Test", :fi "Testi", :sv "Test"},
                       :id                        "21f2bf53-0b8e-47c6-8716-fe04884c24f8",
                       :options                   [],
                       :metadata
                                                  {:created-by
                                                   {:oid  "1.2.246.562.24.48978573294",
                                                    :date "2021-11-17T11:54:32Z",
                                                    :name "Risto Salama"},
                                                   :modified-by
                                                   {:oid  "1.2.246.562.24.48978573294",
                                                    :date "2021-12-14T15:02:06Z",
                                                    :name "Risto Salama"}},
                       :fieldType                 "attachment"}
                      {:id         "e655e2ba-b9be-48cd-bf08-a19bff666bc9",
                       :label      {:en "Test3", :fi "Testi3", :sv "Test3"},
                       :params     {},
                       :children
                                   [{:params
                                                                {:info-text              {:enabled? true},
                                                                 :attachment-type        "liitetyypitamm_3",
                                                                 :mail-attachment?       true,
                                                                 :fetch-info-from-kouta? true},
                                     :belongs-to-hakukohderyhma ["1.2.246.562.28.53448852193"],
                                     :validators                [],
                                     :fieldClass                "formField",
                                     :per-hakukohde             true,
                                     :label                     {:en "Test3", :fi "Testi3", :sv "Test3"},
                                     :id                        "8cd33167-a5df-4ab3-ac57-0790deb5a898",
                                     :options                   [],
                                     :metadata
                                                                {:created-by
                                                                 {:oid  "1.2.246.562.24.48978573294",
                                                                  :date "2021-11-22T07:55:42Z",
                                                                  :name "Risto Salama"},
                                                                 :modified-by
                                                                 {:oid  "1.2.246.562.24.48978573294",
                                                                  :date "2021-12-14T15:02:28Z",
                                                                  :name "Risto Salama"}},
                                     :fieldType                 "attachment"}],
                       :metadata
                                   {:created-by
                                    {:oid  "1.2.246.562.24.48978573294",
                                     :date "2021-11-22T07:55:12Z",
                                     :name "Risto Salama"},
                                    :modified-by
                                    {:oid  "1.2.246.562.24.48978573294",
                                     :date "2021-12-14T15:02:18Z",
                                     :name "Risto Salama"}},
                       :fieldType  "fieldset",
                       :fieldClass "wrapperElement"}
                      {:id         "harkinnanvaraisuus-wrapper",
                       :label      {:fi "Harkinnanvaraisuus"},
                       :params     {},
                       :children
                                   [{:params                    {},
                                     :belongs-to-hakukohderyhma ["1.2.246.562.28.53448852193"],
                                     :validators                ["required"],
                                     :fieldClass                "formField",
                                     :per-hakukohde             true,
                                     :label                     {:fi "Haetko harkinnanvaraisesti"},
                                     :id                        "harkinnanvaraisuus",
                                     :options
                                                                [{:label {:fi "Kyllä"},
                                                                  :value "1",
                                                                  :followups
                                                                         [{:id         "harkinnanvaraisuus-reason",
                                                                           :label      {:fi "Peruste harkinnanvaraisuudelle"},
                                                                           :params     {},
                                                                           :options
                                                                                       [{:label {:fi "Oppimisvaikeudet"}, :value "0"}
                                                                                        {:label {:fi "Sosiaaliset syyt"}, :value "1"}
                                                                                        {:label {:fi "Koulutodistusten vertailuvaikeudet"},
                                                                                         :value "2"}
                                                                                        {:label {:fi "Riittämätön tutkintokielen taito"},
                                                                                         :value "3"}],
                                                                           :metadata
                                                                                       {:created-by
                                                                                        {:oid  "1.2.246.562.24.88548681369",
                                                                                         :date "2021-12-02T07:38:21Z",
                                                                                         :name "Jaakko Ketola"},
                                                                                        :modified-by
                                                                                        {:oid  "1.2.246.562.24.88548681369",
                                                                                         :date "2021-12-02T07:38:21Z",
                                                                                         :name "Jaakko Ketola"}},
                                                                           :fieldType  "singleChoice",
                                                                           :fieldClass "formField",

                                                                           :validators ["required"]}
                                                                          {:params
                                                                                          {:info-text              {:enabled? true},
                                                                                           :attachment-type        "liitetyypitamm_6",
                                                                                           :mail-attachment?       true,
                                                                                           :fetch-info-from-kouta? true},
                                                                           :validators    [],
                                                                           :fieldClass    "formField",
                                                                           :per-hakukohde false,
                                                                           :label         {:fi "", :sv ""},
                                                                           :id            "8fc03b55-5c87-4f98-8107-97cca8380335",
                                                                           :options       [],
                                                                           :metadata
                                                                                          {:created-by
                                                                                           {:oid  "1.2.246.562.24.88548681369",
                                                                                            :date "2021-12-02T07:38:34Z",
                                                                                            :name "Jaakko Ketola"},
                                                                                           :modified-by
                                                                                           {:oid  "1.2.246.562.24.88548681369",
                                                                                            :date "2021-12-02T09:23:18Z",
                                                                                            :name "Jaakko Ketola"}},
                                                                           :fieldType     "attachment"}]}
                                                                 {:label {:fi "Ei"}, :value "0", :followups []}],
                                     :metadata
                                                                {:created-by
                                                                 {:oid  "1.2.246.562.24.88548681369",
                                                                  :date "2021-12-02T07:38:21Z",
                                                                  :name "Jaakko Ketola"},
                                                                 :modified-by
                                                                 {:oid "1.2.246.562.24.88548681369",
                                                                  :date "2021-12-02T09:23:14Z",
                                                                  :name "Jaakko Ketola"}},
                                     :fieldType                 "singleChoice"}],
                       :metadata
                                   {:created-by
                                    {:oid  "1.2.246.562.24.88548681369",
                                     :date "2021-12-02T07:38:21Z",
                                     :name "Jaakko Ketola"},
                                    :modified-by
                                    {:oid  "1.2.246.562.24.88548681369",
                                     :date "2021-12-02T07:38:21Z",
                                     :name "Jaakko Ketola"}},
                       :fieldType  "fieldset",
                       :fieldClass "wrapperElement"}
                      {:params
                                      {:hidden           false,
                                       :deadline         "31.12.2022 12:00",
                                       :info-text        {:value {:fi "Perinteiset ohjeet"}, :enabled? true},
                                       :mail-attachment? true},
                       :validators    [],
                       :fieldClass    "formField",
                       :per-hakukohde false,
                       :label         {:fi "Perinteinen liitepyyntö", :sv ""},
                       :id            "98655824-bb9d-4f4a-a1e5-4e39bd0f61f0",
                       :options       [],
                       :metadata
                                      {:created-by
                                       {:oid  "1.2.246.562.24.88548681369",
                                        :date "2021-12-14T08:33:05Z",
                                        :name "Jaakko Ketola"},
                                       :modified-by
                                       {:oid  "1.2.246.562.24.88548681369",
                                        :date "2021-12-14T08:39:18Z",
                                        :name "Jaakko Ketola"}},
                       :fieldType     "attachment"}
                      {:id         "9b00783c-5f4e-4ef9-bca4-c2e57b443d3c",
                       :label      {:fi "Upload liite", :sv ""},
                       :params     {:hidden false, :deadline "1.12.2022 12:00"},
                       :options    [],
                       :metadata
                                   {:created-by
                                    {:oid  "1.2.246.562.24.88548681369",
                                     :date "2021-12-14T08:43:43Z",
                                     :name "Jaakko Ketola"},
                                    :modified-by
                                    {:oid  "1.2.246.562.24.88548681369",
                                     :date "2021-12-14T08:44:10Z",
                                     :name "Jaakko Ketola"}},
                       :fieldType  "attachment",
                       :fieldClass "formField"}],
   :name
                     {:en "HLE-1452 liitepyyntö In English",
                      :fi "HLE-1452 liitepyyntö",
                      :sv "HLE-1452 liitepyyntö På Svenska"},
   :organization-oid "1.2.246.562.10.00000000001",
   :created-by       "SalamaGofore",
   :id               787276,
   :created-time     "2021-12-14T15:02:36.523Z",
   :languages        ["fi" "sv" "en"]}
  )

(def email-template
  {:from    "no-reply@opintopolku.fi",
   :subject "Opintopolku: hakemuksesi on vastaanotettu",
   :content "",
   :content-ending
            "Voit katsella ja muokata hakemustasi yllä olevan linkin kautta. Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta.\n\nJos sinulla on verkkopankkitunnukset, mobiilivarmenne tai sähköinen henkilökortti, voit vaihtoehtoisesti kirjautua sisään [Opintopolku.fi](https://www.opintopolku.fi):ssä, ja tehdä muutoksia hakemukseesi Oma Opintopolku -palvelussa hakuaikana. Oma Opintopolku -palvelussa voit lisäksi nähdä valintojen tulokset ja ottaa opiskelupaikan vastaan.\n",
   :lang    "fi",
   :signature
            "Älä vastaa tähän viestiin - viesti on lähetetty automaattisesti.\n\nYstävällisin terveisin <br/>\nOpintopolku\n",
   :body
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n<html>\n  <head>\n    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n    <title></title>\n  </head>\n  <body style=\"margin: 0; font-family: 'Open Sans', Arial, sans-serif;\">\n    <table style=\"width: 600px;\">\n      <tr>\n        <td style=\"padding-left: 20px; padding-top: 20px;\">\n          <span style=\"font-size: 24px;\">Hakemuksesi on vastaanotettu</span>\n        </td>\n      </tr>\n      \n      <tr>\n        <td style=\"padding-left: 20px; padding-top: 20px;\">\n          Hakemusnumero: 1.2.246.562.11.00000000000000000000\n        </td>\n      </tr>\n      \n      <tr>\n        <td style=\"padding-left: 20px; padding-top: 20px;\">\n          Hakutoiveesi ovat:\n        </td>\n      </tr>\n      \n      <tr>\n        <td style=\"padding-left: 20px; padding-top: 10px;\">\n          Hakukohde 1\n        </td>\n      </tr>\n      \n      <tr>\n        <td style=\"padding-left: 20px; padding-top: 10px;\">\n          Hakukohde 2\n        </td>\n      </tr>\n      \n      <tr>\n        <td style=\"padding-left: 20px; padding-top: 10px;\">\n          Hakukohde 3\n        </td>\n      </tr>\n      \n      \n      \n        <tr>\n          <td style=\"padding-left: 20px; padding-top: 20px;\">\n            Muistathan palauttaa vielä liitteet:\n          </td>\n        </tr>\n      \n      \n        <tr>\n          <td>\n            <ul>\n              \n                <li>\n                  Liite 1\n                  \n                  <br> Palautettava viimeistään 31.12.2000 klo 02:00\n                  \n                </li>\n              \n                <li>\n                  Liite 2\n                  \n                </li>\n              \n                <li>\n                  Liite 3\n                  \n                </li>\n              \n            </ul>\n          </td>\n        </tr>\n      \n      \n      \n      <tr>\n        <td style=\"padding-left: 20px; padding-top: 20px;\">\n          <a style=\"color: #0093C4;\" href=\"https://opintopolku.fi/hakemus/01234567890abcdefghijklmn\" target=\"_blank\">https://opintopolku.fi/hakemus/01234567890abcdefghijklmn</a>\n        </td>\n      </tr>\n      \n      \n      <tr>\n        <td style=\"padding-left: 20px; padding-top: 20px;\">\n          <p>Voit katsella ja muokata hakemustasi yllä olevan linkin kautta. Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta.</p><p>Jos sinulla on verkkopankkitunnukset, mobiilivarmenne tai sähköinen henkilökortti, voit vaihtoehtoisesti kirjautua sisään <a href=\"https://www.opintopolku.fi\" target=\"_blank\" style=\"color: #0093C4;\" rel=\"noopener noreferrer\">Opintopolku.fi</a>:ssä, ja tehdä muutoksia hakemukseesi Oma Opintopolku -palvelussa hakuaikana. Oma Opintopolku -palvelussa voit lisäksi nähdä valintojen tulokset ja ottaa opiskelupaikan vastaan.</p>\n        </td>\n      </tr>\n      \n      \n      <tr>\n        <td style=\"padding-left: 20px; padding-top: 20px;\">\n          <p>Älä vastaa tähän viestiin - viesti on lähetetty automaattisesti.</p><p>Ystävällisin terveisin <br /> Opintopolku</p>\n        </td>\n      </tr>\n      \n      <tr>\n        <td style=\"padding-top: 50px;\"></td>\n      </tr>\n    </table>\n  </body>\n</html>\n"}
  )