(ns ataru.fixtures.application
  (:require [clj-time.core :as c]
            [ataru.application.review-states :as review-states]))

(def metadata {:created-by  {:oid  "1.2.246.562.24.1000000"
                             :date "2018-03-21T15:45:29.23+02:00"
                             :name "Teppo Testinen"}
               :modified-by {:oid  "1.2.246.562.24.1000000"
                             :date "2018-03-22T07:55:08Z"
                             :name "Teppo Testinen"}})

(def application-with-koodisto-form
  {:form       981230123,
   :lang       "fi"
   :id         1
   :person-oid "1.2.3.4.5.6"
   :answers    [{:key "kysymys_1" :value "1" :fieldType "dropdown"}]})

(def form {:id           703,
           :name         {:fi "Test fixture what is this"}
           :key          "abcdefghjkl"
           :created-by   "1.2.246.562.11.11111111111"
           :created-time (c/date-time 2016 6 14 12 34 56)
           :content
                         [{:id         "G__31"
                           :label      {:fi "Osion nimi joo on" :sv "Avsnitt namn"}
                           :children
                                       [{:id         "G__19"
                                         :label      {:fi "Eka kysymys" :sv ""}
                                         :fieldType  "textField"
                                         :fieldClass "formField"
                                         :metadata   metadata}
                                        {:id         "G__17"
                                         :label      {:fi "Toka kysymys" :sv ""}
                                         :params     {}
                                         :fieldType  "textField"
                                         :fieldClass "formField"
                                         :metadata   metadata}
                                        {:id         "G__24"
                                         :label      {:fi "Kolmas kysymys" :sv ""}
                                         :params     {}
                                         :fieldType  "textField"
                                         :fieldClass "formField"
                                         :metadata   metadata}
                                        {:id         "G__36"
                                         :label      {:fi "Neljas kysymys" :sv ""}
                                         :params     {}
                                         :fieldType  "textField"
                                         :fieldClass "formField"
                                         :metadata   metadata}],
                           :fieldType  "fieldset"
                           :fieldClass "wrapperElement"
                           :metadata   metadata}
                          {:id         "G__14"
                           :label      {:fi "Viides kysymys" :sv ""}
                           :params     {}
                           :fieldType  "textField"
                           :fieldClass "formField"
                           :metadata   metadata}
                          {:id         "G__47"
                           :label      {:fi "Kuudes kysymys" :sv ""}
                           :params     {}
                           :fieldType  "textField"
                           :fieldClass "formField"
                           :metadata   metadata}]})

;; NOTE: Unlike above, these are in database format, lowercase keys. This is converted in application-store to
;; the format used in REST callls
(def applications
  [{:key          "c58df586-fdb9-4ee1-b4c4-030d4cfe9f81"
    :lang         "fi"
    :created-time (c/date-time 2016 6 15 12 30 55)
    :state        "unprocessed"
    :form_id      703
    :id           1
    :content      {:answers
                   [{:key "G__19" :label {:fi "Eka kysymys"} :value "1" :fieldType "textField"}
                    {:key "G__17" :label {:fi "Toka kysymys"} :value "2" :fieldType "textField"}
                    {:key "G__24" :label {:fi "Kolmas kysymys"} :value "3" :fieldType "textField"}
                    {:key "G__36" :label {:fi "Neljas kysymys"} :value "4" :fieldType "textField"}
                    {:key "G__14" :label {:fi "Viides kysymys"} :value "5" :fieldType "textField"}
                    {:key "G__47" :label {:fi "Kuudes kysymys"} :value "6" :fieldType "textField"}]}}
   {:key          "956ae57b-8bd2-42c5-90ac-82bd0a4fd31f"
    :lang         "fi"
    :created-time (c/date-time 2016 6 15 14 30 55)
    :state        "unprocessed"
    :form_id      703
    :id           2
    :content      {:answers
                   [{:key "G__19" :label {:fi "Eka kysymys"} :value "Vastaus" :fieldType "textField"}
                    {:key "G__17" :label {:fi "Toka kysymys"} :value "lomakkeeseen" :fieldType "textField"}
                    {:key "G__24" :label {:fi "Kolmas kysymys"} :value "asiallinen" :fieldType "textField"}
                    {:key "G__36" :label {:fi "Neljas kysymys"} :value "vastaus" :fieldType "textField"}
                    {:key "G__47" :label {:fi "Kuudes kysymys"} :value "jee" :fieldType "textField"}]}}
   {:key          "9d24af7d-f672-4c0e-870f-3c6999f105e0"
    :lang         "fi"
    :created-time (c/date-time 2016 6 16 6 0 0)
    :state        "unprocessed"
    :form_id      703
    :id           3
    :content      {:answers
                   [{:key "G__19" :label {:fi "Eka kysymys"} :value "a" :fieldType "textField"}
                    {:key "G__17" :label {:fi "Toka kysymys"} :value "b" :fieldType "textField"}
                    {:key "G__24" :label {:fi "Kolmas kysymys"} :value "d" :fieldType "textField"}
                    {:key "G__36" :label {:fi "Neljas kysymys"} :value "e" :fieldType "textField"}
                    {:key "G__14" :label {:fi "Seitsemas kysymys"} :value "f" :fieldType "textField"}
                    {:key "G__47" :label {:fi "Kuudes kysymys"} :value "g" :fieldType "textField"}]}}
   {:key          "9d24af7d-f672-4c0e-870f-3c6999f105e1"
    :lang         "fi"
    :created-time (c/date-time 2016 6 16 6 10 0)
    :state        "unprocessed"
    :form_id      703
    :id           4
    :hakukohde    ["1.2.246.562.29.11111111110"]
    :content      {:answers
                   [{:key "G__19" :label {:fi "Eka kysymys"} :value "1" :fieldType "textField"}
                    {:key "G__17" :label {:fi "Toka kysymys"} :value "2" :fieldType "textField"}
                    {:key "G__24" :label {:fi "Kolmas kysymys"} :value "3" :fieldType "textField"}
                    {:key "G__36" :label {:fi "Neljas kysymys"} :value "4" :fieldType "textField"}
                    {:key "G__14" :label {:fi "Seitsemas kysymys"} :value "5" :fieldType "textField"}
                    {:key "G__47" :label {:fi "Kuudes kysymys"} :value "6" :fieldType "textField"}]}}
   {:key          "9d24af7d-f672-4c0e-870f-3c6999f105e2"
    :lang         "fi"
    :created-time (c/date-time 2016 6 16 6 15 0)
    :state        "unprocessed"
    :form_id      703
    :id           5
    :hakukohde    ["1.2.246.562.29.11111111110"]
    :content      {:answers
                   [{:key "G__19" :label {:fi "Eka kysymys"} :value "q" :fieldType "textField"}
                    {:key "G__17" :label {:fi "Toka kysymys"} :value "w" :fieldType "textField"}
                    {:key "G__24" :label {:fi "Kolmas kysymys"} :value "e" :fieldType "textField"}
                    {:key "G__36" :label {:fi "Neljas kysymys"} :value "r" :fieldType "textField"}
                    {:key "G__14" :label {:fi "Seitsemas kysymys"} :value "t" :fieldType "textField"}
                    {:key "G__47" :label {:fi "Kuudes kysymys"} :value "y" :fieldType "textField"}]}}
   {:key          "9d24af7d-f672-4c0e-870f-3c6999f105e3"
    :lang         "fi"
    :created-time (c/date-time 2016 6 16 7 15 0)
    :state        "unprocessed"
    :form_id      703
    :id           6
    :hakukohde    ["1.2.246.562.29.11111111119"]
    :content      {:answers
                   [{:key "G__19" :label {:fi "Eka kysymys"} :value "z" :fieldType "textField"}
                    {:key "G__17" :label {:fi "Toka kysymys"} :value "x" :fieldType "textField"}
                    {:key "G__24" :label {:fi "Kolmas kysymys"} :value "c" :fieldType "textField"}
                    {:key "G__36" :label {:fi "Neljas kysymys"} :value "v" :fieldType "textField"}
                    {:key "G__14" :label {:fi "Seitsemas kysymys"} :value "b" :fieldType "textField"}
                    {:key "G__47" :label {:fi "Kuudes kysymys"} :value "n" :fieldType "textField"}]}}
   {:key          "9d24af7d-f672-4c0e-870f-aaaa"
    :lang         "fi"
    :created-time (c/date-time 2016 6 16 7 15 0)
    :state        "unprocessed"
    :form_id      9999
    :id           999
    :hakukohde    ["1.2.246.562.29.123454321" "1.2.246.562.29.123454322"]
    :content      {:answers
                   [{:key       "G__119"
                     :label     {:fi "Eka kysymys"}
                     :value     "z"
                     :fieldType "textField"}
                    {:key       "G__117"
                     :label     {:fi "Toistuva kysymys"}
                     :value     ["x" "y" "z"]
                     :fieldType "textField"}
                    {:key       "G__224"
                     :label     {:fi "Toistuva kysymys ryhmässä"}
                     :value     [["x" "y" "z"]
                                 ["a" "b" "c"]]
                     :fieldType "textField"}]}}
   {:key          "9d24af7d-f672-4c0e-870f-aaaa"            ;; Modified answers!
    :lang         "fi"
    :created-time (c/date-time 2016 6 16 7 15 0)
    :state        "unprocessed"
    :form_id      9999
    :id           999
    :hakukohde    ["1.2.246.562.29.123454321" "1.2.246.562.29.123454322" "1.2.246.562.29.123454323"]
    :content      {:answers
                   [{:key       "G__119"
                     :label     {:fi "Eka kysymys"}
                     :value     ""
                     :fieldType "textField"}
                    {:key       "G__117"
                     :label     {:fi "Toistuva kysymys"}
                     :value     ["x" "y" "a"]
                     :fieldType "textField"}
                    {:key       "G__224"
                     :label     {:fi "Toistuva kysymys ryhmässä"}
                     :value     [["x" "y" "1"]
                                 ["a" "b" "asdfa"]]
                     :fieldType "textField"}]}}
   {:key          "attachments"
    :lang         "fi"
    :last-name    "Liittäjä"
    :created-time (c/date-time 2016 6 16 7 15 0)
    :submitted    (c/date-time 2016 6 16 7 15 0)
    :state        "unprocessed"
    :form_id      110001
    :id           110101
    :hakukohde    ["1.2.246.562.29.123454321" "1.2.246.562.29.123454322" "1.2.246.562.29.123454323"]
    :content      {:answers
                   [{:key       "att__1"
                     :label     {:fi "Liite"}
                     :value     ["liite-id"]
                     :fieldType "attachment"}]}}
   {:key          "c58df586-fdb9-4ee1-b4c4-030d4cfe9f81_1"
    :lang         "fi"
    :created-time (c/date-time 2023 6 2 10 30 55)
    :state        "unprocessed"
    :form_id      703
    :id           1
    :hakukohde    ["1.2.246.562.29.123454321"]
    :application_key "c58df586-fdb9-4ee1-b4c4-030d4cfe9f81_1"
    :content      {:answers
                   [{:key "Q__1" :label {:fi "Eka kysymys"} :value "1" :fieldType "textField"}
                    {:key "A__1" :value "attachment1" :fieldType "attachment"}
                    {:key "A__2" :value "attachment2" :fieldType "attachment"}]}}])

(def application-review
  {:id              1,
   :application_key "c58df586-fdb9-4ee1-b4c4-030d4cfe9f81"
   :state           "unprocessed"
   :notes           "Some notes about the applicant"})

(def person-info-form-application {:form       2147483647,
                                   :lang       "fi"
                                   :id         1
                                   :person-oid "1.2.3.4.5.6"
                                   :answers    [{:key       "b0839467-a6e8-4294-b5cc-830756bbda8a"
                                                 :value     "Vastaus tekstikysymykseen"
                                                 :fieldType "textField"
                                                 :label     {:fi "Tekstikysymys" :sv ""}}
                                                {:key "address" :value "Paratiisitie 13" :fieldType "textField" :label {:fi "Katuosoite" :sv "Adress"}}
                                                {:key       "email"
                                                 :value     "aku@ankkalinna.com"
                                                 :fieldType "textField"
                                                 :label     {:fi "Sähköpostiosoite" :sv "E-postadress"}}
                                                {:key "preferred-name" :value "Aku" :fieldType "textField" :label {:fi "Kutsumanimi" :sv "Smeknamn"}}
                                                {:key "last-name" :value "Ankka" :fieldType "textField" :label {:fi "Sukunimi" :sv "Efternamn"}}
                                                {:key       "phone"
                                                 :value     "050123"
                                                 :fieldType "textField"
                                                 :label     {:fi "Matkapuhelin" :sv "Mobiltelefonnummer"}}
                                                {:key "nationality" :value [["246"]] :fieldType "dropdown" :label {:fi "Kansalaisuus" :sv "Nationalitet"}}
                                                {:key "country-of-residence" :value "246" :fieldType "dropdown" :label {:fi "Asuinmaa" :sv "Boningsland"}}
                                                {:key "ssn" :value "010101A123N" :fieldType "textField" :label {:fi "Henkilötunnus" :sv "Personnummer"}}
                                                {:key "first-name" :value "Aku Petteri" :fieldType "textField" :label {:fi "Etunimet" :sv "Förnamn"}}
                                                {:key "postal-code" :value "00013" :fieldType "textField" :label {:fi "Postinumero" :sv "Postnummer"}}
                                                {:key "postal-office" :value "Paikka" :fieldType "textField" :label {:fi "Postitoimipaikka"}}
                                                {:key "home-town" :value "273" :fieldType "dropdown" :label {:fi "Kotikunta"}}
                                                {:key "language" :value "FI" :fieldType "dropdown" :label {:fi "Äidinkieli" :sv "Modersmål"}}
                                                {:key "gender" :value "1" :fieldType "dropdown" :label {:fi "Sukupuoli" :sv "Kön"}}
                                                {:key "birth-date" :value "1.1.2001" :fieldType "textField" :label {:fi "Syntymäaika"}}]})

(def form-with-followup-inside-a-question-group-application {:form       2147483646,
                                                             :lang       "fi"
                                                             :id         1
                                                             :person-oid "1.2.3.4.5.6"
                                                             :answers    [{:key "address"
                                                                           :value "Paratiisitie 13"
                                                                           :fieldType "textField"
                                                                           :label {:fi "Katuosoite" :sv "Adress"}}
                                                                          {:key       "email"
                                                                           :value     "aku@ankkalinna.com"
                                                                           :fieldType "textField"
                                                                           :label     {:fi "Sähköpostiosoite" :sv "E-postadress"}}
                                                                          {:key "preferred-name"
                                                                           :value "Aku"
                                                                           :fieldType "textField"
                                                                           :label {:fi "Kutsumanimi" :sv "Smeknamn"}}
                                                                          {:key "last-name"
                                                                           :value "Ankka"
                                                                           :fieldType "textField"
                                                                           :label {:fi "Sukunimi" :sv "Efternamn"}}
                                                                          {:key       "phone"
                                                                           :value     "050123"
                                                                           :fieldType "textField"
                                                                           :label     {:fi "Matkapuhelin" :sv "Mobiltelefonnummer"}}
                                                                          {:key "nationality"
                                                                           :value [["246"]]
                                                                           :fieldType "dropdown"
                                                                           :label {:fi "Kansalaisuus" :sv "Nationalitet"}}
                                                                          {:key "country-of-residence"
                                                                           :value "246"
                                                                           :fieldType "dropdown"
                                                                           :label {:fi "Asuinmaa" :sv "Boningsland"}}
                                                                          {:key "ssn"
                                                                           :value "010101A123N"
                                                                           :fieldType "textField"
                                                                           :label {:fi "Henkilötunnus" :sv "Personnummer"}}
                                                                          {:key "first-name"
                                                                           :value "Aku Petteri"
                                                                           :fieldType "textField"
                                                                           :label {:fi "Etunimet" :sv "Förnamn"}}
                                                                          {:key "postal-code"
                                                                           :value "00013"
                                                                           :fieldType "textField"
                                                                           :label {:fi "Postinumero" :sv "Postnummer"}}
                                                                          {:key "postal-office"
                                                                           :value "Paikka"
                                                                           :fieldType "textField"
                                                                           :label {:fi "Postitoimipaikka"}}
                                                                          {:key "home-town"
                                                                           :value "273"
                                                                           :fieldType "dropdown"
                                                                           :label {:fi "Kotikunta"}}
                                                                          {:key "language"
                                                                           :value "FI"
                                                                           :fieldType "dropdown"
                                                                           :label {:fi "Äidinkieli" :sv "Modersmål"}}
                                                                          {:key "gender"
                                                                           :value "1"
                                                                           :fieldType "dropdown"
                                                                           :label {:fi "Sukupuoli" :sv "Kön"}}
                                                                          {:key "birth-date"
                                                                           :value "1.1.2001"
                                                                           :fieldType "textField"
                                                                           :label {:fi "Syntymäaika"}}]})

(def person-info-form-application-with-extra-answer
  (update person-info-form-application
          :answers
          conj
          {:key       "extra-answer-key"
           :value     "Extra stuff!"
           :fieldType "textField"
           :label     {:fi "exxxtra" :sv ""}}))

(def person-info-form-application-with-more-answers
  (-> person-info-form-application
      (merge {:id 555})
      (update
       :answers
       (comp vec concat)
       [{:key       "adjacent-answer-1"
         :value     "Vierekkäinen vastaus 1"
         :fieldType "textField"
         :label     {:fi "Vierekkäinen Kenttä1" :sv ""}}
        {:key       "repeatable-required"
         :value     ["Toistuva pakollinen 1" "Toistuva pakollinen 2" "Toistuva pakollinen 3"]
         :fieldType "textField"
         :label     {:fi "Toistuva pakollinen" :sv ""}}
        {:key       "more-answers-dropdown-id"
         :value     ""
         :fieldType "dropdown"}
        {:key       "more-questions-attachment-id"
         :value     ["attachment-id"]
         :fieldType "attachment"
         :label     {:fi "Eka liite" :sv ""}}])))

(def person-info-form-application-with-modified-answers
  (-> person-info-form-application-with-more-answers
      (update-in [:answers 17 :value] conj "Toistuva pakollinen 4")
      (assoc-in [:answers 18 :value] "toka vaihtoehto")
      (assoc-in [:answers 19 :value] ["modified-attachment-id"])
      (update :answers (comp vec concat) [{:key       "adjacent-answer-2"
                                           :value     "Vierekkäinen vastaus 2"
                                           :fieldType "textField"}])))

(def person-info-form-application-with-empty-answers
  (-> person-info-form-application-with-more-answers
      (assoc :haku      "1.2.246.562.29.65950024186"
             :hakukohde ["1.2.246.562.20.49028196523" "1.2.246.562.20.49028196524"])
      (update :answers (comp vec concat)
              [{:key       "hakukohteet"
                :value     ["1.2.246.562.20.49028196523" "1.2.246.562.20.49028196524"]
                :fieldType "hakukohteet"
                :label     {:fi "Hakukohteet" :sv "Ansökningsmål" :en "Application options"}}])))

(def dropdown-followups
  [{:key       "dropdown-followup-1"
    :value     ["followup-attachment"]
    :fieldType "attachment"
    :label     {:fi "Dropdown liite" :sv ""}}
   {:key       "dropdown-followup-2"
    :value     "toka"
    :fieldType "singleChoice"
    :label     {:fi "Dropdown painikkeet required" :sv ""}}])

(def person-info-form-application-with-more-modified-answers
  (-> person-info-form-application-with-modified-answers
      (assoc-in [:answers 18 :value] "eka vaihtoehto")
      (update :answers (comp vec concat) dropdown-followups)))

(def person-info-form-application-for-hakukohde
  {:form      2147483647
   :lang      "fi"
   :id        2
   :haku      "1.2.246.562.29.65950024186"
   :hakukohde ["1.2.246.562.20.49028196523" "1.2.246.562.20.49028196524"]
   :answers   [{:key       "b0839467-a6e8-4294-b5cc-830756bbda8a"
                :value     "Vastaus tekstikysymykseen"
                :fieldType "textField"
                :label     {:fi "Tekstikysymys" :sv ""}}
               {:key "address" :value "Paratiisitie 13" :fieldType "textField" :label {:fi "Katuosoite" :sv "Adress"}}
               {:key       "email"
                :value     "aku@ankkalinna.com"
                :fieldType "textField"
                :label     {:fi "Sähköpostiosoite" :sv "E-postadress"}}
               {:key "preferred-name" :value "Aku" :fieldType "textField" :label {:fi "Kutsumanimi" :sv "Smeknamn"}}
               {:key "last-name" :value "Ankka" :fieldType "textField" :label {:fi "Sukunimi" :sv "Efternamn"}}
               {:key       "phone"
                :value     "050123"
                :fieldType "textField"
                :label     {:fi "Matkapuhelin" :sv "Mobiltelefonnummer"}}
               {:key "nationality" :value [["246"]] :fieldType "dropdown" :label {:fi "Kansalaisuus" :sv "Nationalitet"}}
               {:key "country-of-residence" :value "246" :fieldType "dropdown" :label {:fi "Asuinmaa" :sv "Boningsland"}}
               {:key "ssn" :value "010101A123N" :fieldType "textField" :label {:fi "Henkilötunnus" :sv "Personnummer"}}
               {:key "first-name" :value "Aku Petteri" :fieldType "textField" :label {:fi "Etunimet" :sv "Förnamn"}}
               {:key "postal-code" :value "00013" :fieldType "textField" :label {:fi "Postinumero" :sv "Postnummer"}}
               {:key "postal-office" :value "Paikka" :fieldType "textField" :label {:fi "Postitoimipaikka"}}
               {:key "home-town" :value "273" :fieldType "dropdown" :label {:fi "Kotikunta"}}
               {:key "language" :value "FI" :fieldType "dropdown" :label {:fi "Äidinkieli" :sv "Modersmål"}}
               {:key "gender" :value "1" :fieldType "dropdown" :label {:fi "Sukupuoli" :sv "Kön"}}
               {:key "birth-date" :value "1.1.2001" :fieldType "textField" :label {:fi "Syntymäaika"}}
               {:key "164954b5-7b23-4774-bd44-dee14071316b" :value ["57af9386-d80c-4321-ab4a-d53619c14a74"] :fieldType "attachment" :label {:fi "Eka liite" :sv ""}}
               {:key       "hakukohteet"
                :value     ["1.2.246.562.20.49028196523" "1.2.246.562.20.49028196524"]
                :fieldType "hakukohteet"
                :label     {:fi "Hakukohteet" :sv "Ansökningsmål" :en "Application options"}}
               {:key       "87834771-34da-40a4-a9f6-sensitive"
                :value     "Salainen vastaus"
                :fieldType "textArea"
                :label     {:fi "Salainen kysymys" :sv ""}}]})

(def application-with-person-info-module {:key     "9d24af7d-f672-4c0e-870f-3c6999f105e0"
                                          :lang    "fi"
                                          :form_id 703,
                                          :id      3,
                                          :answers [{:key       "address"
                                                     :label     {:en "Address" :fi "Katuosoite" :sv "Näraddress"}
                                                     :value     "Paratiisitie 13"
                                                     :fieldType "textField"}
                                                    {:key       "email"
                                                     :label     {:en "E-mail address" :fi "Sähköpostiosoite" :sv "E-postadress"}
                                                     :value     "aku@ankkalinna.com"
                                                     :fieldType "textField"}
                                                    {:key       "preferred-name"
                                                     :label     {:en "Middle name" :fi "Kutsumanimi" :sv "Tilltalsnamn"}
                                                     :value     "Aku"
                                                     :fieldType "textField"}
                                                    {:key       "last-name"
                                                     :label     {:en "Surname" :fi "Sukunimi" :sv "Efternamn"}
                                                     :value     "Ankka"
                                                     :fieldType "textField"}
                                                    {:key       "phone"
                                                     :label     {:en "Mobile phone number" :fi "Matkapuhelin" :sv "Mobiltelefonnummer"}
                                                     :value     "050123"
                                                     :fieldType "textField"}
                                                    {:key       "nationality"
                                                     :label     {:en "Nationality" :fi "Kansalaisuus" :sv "Medborgarskap"}
                                                     :value     [["246"]]
                                                     :fieldType "dropdown"}
                                                    {:key       "2b859fe1-8661-404c-8b19-bfb27604575c"
                                                     :label     {:en "Question" :fi "Pudotusvalikon kysymys" :sv ""}
                                                     :value     "Answer"
                                                     :fieldType "dropdown"}
                                                    {:key       "ssn"
                                                     :label     {:en "Social security number" :fi "Henkilötunnus" :sv "Personbeteckning"}
                                                     :value     "120496-924J"
                                                     :fieldType "textField"}
                                                    {:key       "first-name"
                                                     :label     {:en "First name" :fi "Etunimet" :sv "Förnamn"}
                                                     :value     "Aku"
                                                     :fieldType "textField"}
                                                    {:key       "birth-date"
                                                     :label     {:en "Date of birth" :fi "Syntymäaika" :sv "Födelsetid"}
                                                     :value     "29.10.1984"
                                                     :fieldType "textField"}
                                                    {:key       "postal-code"
                                                     :label     {:en "Postal code" :fi "Postinumero" :sv "Postnummer"}
                                                     :value     "00013"
                                                     :fieldType "textField"}
                                                    {:key       "language"
                                                     :label     {:en "Native language" :fi "Äidinkieli" :sv "Modersmål"}
                                                     :value     "FI"
                                                     :fieldType "dropdown"}
                                                    {:key "gender" :label {:en "Gender" :fi "Sukupuoli" :sv "Kön"} :value "2" :fieldType "dropdown"}
                                                    {:key       "postal-office"
                                                     :label     {:en "Postal office" :fi "Postitoimipaikka" :sv "Postkontor"}
                                                     :value     "POHJOLA"
                                                     :fieldType "textField"}
                                                    {:key       "home-town"
                                                     :label     {:en "Home town" :fi "Kotikunta" :sv "Hemkommun"}
                                                     :value     "273"
                                                     :fieldType "textField"}]})

(def bug2139-application
  (-> person-info-form-application
      (merge {:form      5
              :id        2
              :haku      "1.2.246.562.29.93102260101"
              :hakukohde ["1.2.246.562.20.49028196524"]
              :base-education ["pohjakoulutus_kk"]})))

(def applications-list-query
  {:sort
                             {:order-by "applicant-name",
                              :order    "asc"},
   :attachment-review-states {},
   :states-and-filters
                             {:attachment-states-to-include
                              ["not-checked"
                               "checked"
                               "incomplete-attachment"
                               "attachment-missing"
                               "overdue"
                               "no-requirements"],
                              :processing-states-to-include
                              ["information-request"],
                              :filters
                              {:language-requirement
                                                              {:unreviewed true, :fulfilled true, :unfulfilled true},
                               :degree-requirement
                                                              {:unreviewed true, :fulfilled true, :unfulfilled true},
                               :eligibility-set-automatically {:yes true, :no true},
                               :only-identified               {:identified true, :unidentified true},
                               :only-ssn                      {:with-ssn true, :without-ssn true},
                               :active-status                 {:active true, :passive true},
                               :eligibility-state
                                                              {:unreviewed             true,
                                                               :eligible               true,
                                                               :uneligible             true,
                                                               :conditionally-eligible true},
                               :base-education
                                                              {:pohjakoulutus_kk_ulk                     true,
                                                               :pohjakoulutus_lk                         true,
                                                               :pohjakoulutus_kk                         true,
                                                               :pohjakoulutus_amp                        true,
                                                               :pohjakoulutus_amt                        true,
                                                               :pohjakoulutus_amv                        true,
                                                               :pohjakoulutus_ulk                        true,
                                                               :pohjakoulutus_muu                        true,
                                                               :pohjakoulutus_avoin                      true,
                                                               :pohjakoulutus_yo_ammatillinen            true,
                                                               :pohjakoulutus_am                         true,
                                                               :pohjakoulutus_yo_ulkomainen              true,
                                                               :pohjakoulutus_yo                         true,
                                                               :pohjakoulutus_yo_kansainvalinen_suomessa true},
                               :payment-obligation
                                                              {:unreviewed true, :obligated true, :not-obligated true}}},
   :haku-oid                 "1.2.246.562.29.93102260101"})

(def applications-list-query-matching-everything
  (-> applications-list-query
      (assoc-in [:states-and-filters :processing-states-to-include]
                (mapv first review-states/application-hakukohde-processing-states))))

(def application-review-notes-without-hakukohde
  {:application-keys ["c58df586-fdb9-4ee1-b4c4-030d4cfe9f81"]
   :notes           "Some notes about the applicant"})

(def application-review-notes-with-hakukohde
  {:application-keys ["c58df586-fdb9-4ee1-b4c4-030d4cfe9f81"]
   :hakukohde       "1.2.246.562.29.93102260101"
   :notes           "Some notes about the applicant"})

(def invalid-application-review-notes
  {:application-keys ["c58df586-fdb9-4ee1-b4c4-030d4cfe9f81"]
   :notes           123})

(def application-review-notes-with-invalid-state
  {:application-keys ["c58df586-fdb9-4ee1-b4c4-030d4cfe9f81"]
   :notes           "Some notes about the applicant"
   :state-name      "foobar"})

(def application-review-notes-with-valid-state
  {:application-keys ["c58df586-fdb9-4ee1-b4c4-030d4cfe9f81"]
   :notes           "Some notes about the applicant"
   :state-name      "processing-state"})

(def application-with-hakemusmaksu-exemption
  (-> person-info-form-application
      (merge {:form       909909,
              :lang       "fi"
              :haku       "payment-info-test-kk-haku"
              :hakukohde  ["payment-info-test-kk-hakukohde"]
              :id         543210
              :person-oid "1.2.3.4.5.303"})
      (update :answers
              (comp vec concat)
              [{:key "vapautus_hakemusmaksusta" :value "0" :fieldType "dropdown"}])))

(def application-without-hakemusmaksu-exemption
  (-> person-info-form-application
      (merge {:form       909909,
              :lang       "fi"
              :haku       "payment-info-test-kk-haku"
              :hakukohde  ["payment-info-test-kk-hakukohde"]
              :id         543211
              :person-oid "1.2.3.4.5.303"})
      (update
        :answers
        (comp vec concat)
        [{:key "vapautus_hakemusmaksusta" :value "12345" :fieldType "dropdown"}])))
