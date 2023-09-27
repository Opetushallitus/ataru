(ns ataru.applications.synthetic-application-util)

(defn synthetic-application->application
  [synthetic form-id]
  (let [answers [{:key "hakukohteet" :value [(:hakukohdeOid synthetic)] :fieldType "hakukohteet" :label {:fi "Hakukohteet"}}
                 {:key "address" :value (:lahiosoite synthetic) :fieldType "textField" :label {:fi "Katuosoite"}}
                 {:key "email" :value (:email synthetic) :fieldType "textField" :label {:fi "Sähköpostiosoite"}}
                 {:key "preferred-name" :value (:kutsumanimi synthetic) :fieldType "textField" :label {:fi "Kutsumanimi"}}
                 {:key "last-name" :value (:sukunimi synthetic) :fieldType "textField" :label {:fi "Sukunimi"}}
                 {:key "phone" :value (:matkapuhelin synthetic) :fieldType "textField"  :label {:fi "Matkapuhelin"}}
                 {:key "nationality" :value [(:kansalaisuus synthetic)] :fieldType "dropdown" :label {:fi "Kansalaisuus"}}
                 {:key "country-of-residence" :value (:asuinmaa synthetic) :fieldType "dropdown" :label {:fi "Asuinmaa"}}
                 {:key "ssn" :value (:hetu synthetic) :fieldType "textField" :label {:fi "Henkilötunnus" :sv "Personnummer"}}
                 {:key "first-name" :value (:etunimet synthetic) :fieldType "textField" :label {:fi "Etunimet"}}
                 {:key "postal-code" :value (:postinumero synthetic) :fieldType "textField" :label {:fi "Postinumero"}}
                 {:key "postal-office" :value (:postitoimipaikka synthetic) :fieldType "textField" :label {:fi "Postitoimipaikka"}}
                 {:key "home-town" :value (:kotikunta synthetic) :fieldType "dropdown" :label {:fi "Kotikunta"}}
                 {:key "language" :value (:aidinkieli synthetic) :fieldType "dropdown" :label {:fi "Äidinkieli"}}
                 {:key "gender" :value (:sukupuoli synthetic) :fieldType "dropdown" :label {:fi "Sukupuoli"}}
                 {:key "birth-date" :value (:syntymaaika synthetic) :fieldType "textField" :label {:fi "Syntymäaika"}}
                 {:key "asiointikieli" :value (:asiointikieli synthetic) :fieldType "dropdown" :label {:fi "Asiointikieli"}}
                 {:key "secondary-completed-base-education" :value (:toisenAsteenKoulutus synthetic) :fieldType "singleChoice" :label {:fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon?"}}
                 {:key "secondary-completed-base-education–country" :value (:toisenAsteenKoulutusMaa synthetic) :fieldType "dropdown" :label {:fi "Suoritusmaa"}}]]
        {:haku (:hakuOid synthetic)
         :hakukohde [(:hakukohdeOid synthetic)]
         :form form-id
         :lang "fi"
         :answers (remove #(nil? (:value %)) answers)}))
