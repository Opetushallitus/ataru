(ns ataru.applications.synthetic-application-util
  (:require [ataru.ssn :as ssn]))

(defn synthetic-application->application
  [synthetic form-id]
  (let [ssn (:henkilotunnus synthetic)
        birth-date (or (:syntymaAika synthetic) (ssn/ssn->birth-date ssn))
        gender (or (:sukupuoli synthetic) (ssn/ssn->gender ssn))
        birthplace (when (empty? ssn) (:syntymapaikka synthetic)) ; Doesn't pass validation together with Finnish SSN, so silently remove.
        answers [{:key "hakukohteet" :value [(:hakukohdeOid synthetic)] :fieldType "hakukohteet" :label {:fi "Hakukohteet"}}
                 {:key "first-name" :value (:etunimi synthetic) :fieldType "textField" :label {:fi "Etunimet"}}
                 {:key "preferred-name" :value (:kutsumanimi synthetic) :fieldType "textField" :label {:fi "Kutsumanimi"}}
                 {:key "last-name" :value (:sukunimi synthetic) :fieldType "textField" :label {:fi "Sukunimi"}}
                 {:key "phone" :value (:puhelinnumero synthetic) :fieldType "textField"  :label {:fi "Matkapuhelin"}}
                 {:key "email" :value (:sahkoposti synthetic) :fieldType "textField" :label {:fi "Sähköpostiosoite"}}
                 {:key "ssn" :value (:henkilotunnus synthetic) :fieldType "textField" :label {:fi "Henkilötunnus"}}
                 {:key "nationality" :value [[(:kansalaisuus synthetic)]] :fieldType "dropdown" :label {:fi "Kansalaisuus"}}
                 {:key "gender" :value gender :fieldType "dropdown" :label {:fi "Sukupuoli"}}
                 {:key "birth-date" :value birth-date :fieldType "textField" :label {:fi "Syntymäaika"}}
                 {:key "birthplace" :value birthplace :fieldType "textField" :label {:fi "Syntymäpaikka ja -maa"}}
                 {:key "passport-number" :value (:passinNumero synthetic) :fieldType "textField" :label {:fi "Passin numero"}}
                 {:key "national-id-number" :value (:idTunnus synthetic) :fieldType "textField" :label {:fi "Kansallinen ID-tunnus"}}
                 {:key "country-of-residence" :value (:asuinmaa synthetic) :fieldType "dropdown" :label {:fi "Asuinmaa"}}
                 {:key "address" :value (:osoite synthetic) :fieldType "textField" :label {:fi "Katuosoite"}}
                 {:key "postal-code" :value (:postinumero synthetic) :fieldType "textField" :label {:fi "Postinumero"}}
                 {:key "postal-office" :value (:postitoimipaikka synthetic) :fieldType "textField" :label {:fi "Postitoimipaikka"}}
                 {:key "home-town" :value (:kotikunta synthetic) :fieldType "dropdown" :label {:fi "Kotikunta"}}
                 {:key "city" :value (:kaupunkiJaMaa synthetic) :fieldType "textField" :label {:fi "Kaupunki ja maa"}}
                 {:key "language" :value (:aidinkieli synthetic) :fieldType "dropdown" :label {:fi "Äidinkieli"}}
                 {:key "asiointikieli" :value (:asiointikieli synthetic) :fieldType "dropdown" :label {:fi "Asiointikieli"}}
                 {:key "secondary-completed-base-education" :value (:toisenAsteenSuoritus synthetic) :fieldType "singleChoice" :label {:fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon?"}}
                 {:key "secondary-completed-base-education–country" :value (:toisenAsteenSuoritusmaa synthetic) :fieldType "dropdown" :label {:fi "Suoritusmaa"}}]]
        {:haku (:hakuOid synthetic)
         :hakukohde [(:hakukohdeOid synthetic)]
         :form form-id
         :lang "fi"
         :answers (remove #(empty? (:value %)) answers)}))
