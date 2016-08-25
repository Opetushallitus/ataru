(ns ataru.fixtures.application
  (:require [clj-time.core :as c]))

(def form {
 :id 703,
 :name "Test fixture what is this",
 :created-by "DEVELOPER",
 :modified-time (c/date-time 2016 6 14 12 34 56)
 :content
 [{:id "G__31",
   :label {:fi "Osion nimi joo on", :sv "Avsnitt namn"},
   :children
   [{:id "G__19",
     :label {:fi "Eka kysymys", :sv ""},
     :fieldType "textField",
     :fieldClass "formField"}
    {:id "G__17",
     :label {:fi "Toka kysymys", :sv ""},
     :params {},
     :fieldType "textField",
     :fieldClass "formField"}
    {:id "G__24",
     :label {:fi "Kolmas kysymys", :sv ""},
     :params {},
     :fieldType "textField",
     :fieldClass "formField"}
    {:id "G__36",
     :label {:fi "Neljas kysymys", :sv ""},
     :params {},
     :fieldType "textField",
     :fieldClass "formField"}],
   :fieldType "fieldset",
   :fieldClass "wrapperElement"}
  {:id "G__14",
   :label {:fi "Viides kysymys", :sv ""},
   :params {},
   :fieldType "textField",
   :fieldClass "formField"}
  {:id "G__47",
   :label {:fi "Kuudes kysymys", :sv ""},
   :params {},
   :fieldType "textField",
   :fieldClass "formField"}]})

;; NOTE: Unlike above, these are in database format, lowercase keys. This is converted in application-store to
;; the format used in REST callls
(def applications
  [{:key "c58df586-fdb9-4ee1-b4c4-030d4cfe9f81",
  :lang "fi",
  :modified-time (c/date-time 2016 6 15 12 30 55)
  :form_id 703
  :id 1
  :content
    {:answers
     [{:key "G__19", :label {:fi "Eka kysymys"}, :value "1", :fieldType "textField"}
      {:key "G__17", :label {:fi "Toka kysymys"}, :value "2", :fieldType "textField"}
      {:key "G__24", :label {:fi "Kolmas kysymys"}, :value "3", :fieldType "textField"}
      {:key "G__36", :label {:fi "Neljas kysymys"}, :value "4", :fieldType "textField"}
      {:key "G__14", :label {:fi "Viides kysymys"}, :value "5", :fieldType "textField"}
      {:key "G__47", :label {:fi "Kuudes kysymys"}, :value "6", :fieldType "textField"}]}}
 {:key "956ae57b-8bd2-42c5-90ac-82bd0a4fd31f",
  :lang "fi",
  :modified-time (c/date-time 2016 6 15 14 30 55)
  :form_id 703
  :id 2
  :content
    {:answers
     [{:key "G__19", :label {:fi "Eka kysymys"}, :value "Vastaus", :fieldType "textField"}
      {:key "G__17", :label {:fi "Toka kysymys"}, :value "lomakkeeseen", :fieldType "textField"}
      {:key "G__24", :label {:fi "Kolmas kysymys"}, :value "asiallinen", :fieldType "textField"}
      {:key "G__36", :label {:fi "Neljas kysymys"}, :value "vastaus", :fieldType "textField"}
      {:key "G__47", :label {:fi "Kuudes kysymys"}, :value "jee", :fieldType "textField"}]}}
 {:key "9d24af7d-f672-4c0e-870f-3c6999f105e0",
  :lang "fi",
  :modified-time (c/date-time 2016 6 16 6 0 0)
  :form_id 703
  :id 3
  :content
    {:answers
     [{:key "G__19", :label {:fi "Eka kysymys"}, :value "a", :fieldType "textField"}
      {:key "G__17", :label {:fi "Toka kysymys"}, :value "b", :fieldType "textField"}
      {:key "G__24", :label {:fi "Kolmas kysymys"}, :value "d", :fieldType "textField"}
      {:key "G__36", :label {:fi "Neljas kysymys"}, :value "e", :fieldType "textField"}
      {:key "G__14", :label {:fi "Seitsemas kysymys"}, :value "f", :fieldType "textField"}
      {:key "G__47", :label {:fi "Kuudes kysymys"}, :value "g", :fieldType "textField"}]}}])

(def application-review
  {:id 1,
   :state "received",
   :notes "Some notes about the applicant"})

(def person-info-form-application {:form 15,
                                   :lang "fi",
                                   :id 1
                                   :answers [{:key "b0839467-a6e8-4294-b5cc-830756bbda8a",
                                              :value "Vastaus tekstikysymykseen",
                                              :fieldType "textField",
                                              :label {:fi "Tekstikysymys", :sv ""}}
                                             {:key "address", :value "Paratiisitie 13", :fieldType "textField", :label {:fi "Katuosoite", :sv "Adress"}}
                                             {:key "email",
                                              :value "aku@ankkalinna.com",
                                              :fieldType "textField",
                                              :label {:fi "Sähköpostiosoite", :sv "E-postadress"}}
                                             {:key "preferred-name", :value "Aku", :fieldType "textField", :label {:fi "Kutsumanimi", :sv "Smeknamn"}}
                                             {:key "last-name", :value "Ankka", :fieldType "textField", :label {:fi "Sukunimi", :sv "Efternamn"}}
                                             {:key "phone",
                                              :value "050123",
                                              :fieldType "textField",
                                              :label {:fi "Matkapuhelin", :sv "Mobiltelefonnummer"}}
                                             {:key "nationality", :value "Suomi", :fieldType "dropdown", :label {:fi "Kansalaisuus", :sv "Nationalitet"}}
                                             {:key "ssn", :value "010101A123N", :fieldType "textField", :label {:fi "Henkilötunnus", :sv "Personnummer"}}
                                             {:key "home-town",
                                              :value "Ankkalinna",
                                              :fieldType "textField",
                                              :label {:fi "Kotikunta", :sv "Bostadsort"}}
                                             {:key "first-name", :value "Aku Petteri", :fieldType "textField", :label {:fi "Etunimet", :sv "Förnamn"}}
                                             {:key "postal-code", :value "00013", :fieldType "textField", :label {:fi "Postinumero", :sv "Postnummer"}}
                                             {:key "language", :value "suomi", :fieldType "dropdown", :label {:fi "Äidinkieli", :sv "Modersmål"}}
                                             {:key "gender", :value "Mies", :fieldType "dropdown", :label {:fi "Sukupuoli", :sv "Kön"}}]})
