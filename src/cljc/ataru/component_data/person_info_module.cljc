  (ns ataru.component-data.person-info-module
    (:require [ataru.virkailija.component-data.component :as component]
              [clojure.walk]))

; validators defined in ataru.hakija.application-validators

(defn ^:private text-field
  [labels & {:keys [size id validators rules params blur-rules exclude-from-answers-if-hidden] :or
                   {size "M" validators [:required] rules {} params {} blur-rules {} exclude-from-answers-if-hidden false}}]
  (-> (component/text-field)
      (assoc :id id)
      (assoc :rules rules)
      (assoc :label labels)
      (assoc :validators validators)
      (assoc-in [:params :size] size)
      (cond-> (not (empty? params)) (assoc :params params))
      (cond-> (not (empty? blur-rules)) (assoc :blur-rules blur-rules))
      (cond-> exclude-from-answers-if-hidden (assoc :exclude-from-answers-if-hidden true))))

(defn ^:private first-name-component
  []
  (text-field {:fi "Etunimet" :sv "Förnamn" :en "Forenames"} :id :first-name :blur-rules {:prefill-preferred-first-name :main-first-name}))

(defn ^:private preferred-name-component
  []
  (text-field {:fi "Kutsumanimi" :sv "Tilltalsnamn" :en "Main forename"}
              :size "S"
              :id :preferred-name
              :validators [:required :main-first-name]))

(defn ^:private first-name-section
  []
  (component/row-section [(first-name-component)
                          (preferred-name-component)]))

(defn ^:private last-name-component
  []
  (text-field {:fi "Sukunimi" :sv "Efternamn" :en "Surname"} :id :last-name))

(defn ^:private dropdown-option
  [value labels & {:keys [default-value] :or {default-value false}}]
  (-> (component/dropdown-option)
      (merge {:value value :label labels}
             (when default-value
               {:default-value default-value}))))

(defn ^:private nationality-component
  []
  (-> (component/dropdown)
      (merge {:label {:fi "Kansalaisuus" :sv "Medborgarskap" :en "Nationality"}
              :validators [:required]
              :rules {:swap-ssn-birthdate-based-on-nationality [:ssn :birth-date]}
              :id :nationality
              :koodisto-source {:uri "maatjavaltiot2" :version 1 :default-option "Suomi"}})))

(defn- country-of-residence-component
  []
  (-> (component/dropdown)
      (merge {:label {:fi "Asuinmaa" :sv "Boningsland" :en "Country of residence"}
              :validators [:required]
              :rules {:change-country-of-residence nil}
              :id :country-of-residence
              :koodisto-source {:uri "maatjavaltiot2" :version 1 :default-option "Suomi"}})))

(defn- have-finnish-ssn-component
  []
  (-> (component/dropdown)
      (merge {:label {:fi "Onko sinulla suomalainen henkilötunnus?"
                      :sv "Har du en finländsk personbeteckning?"
                      :en "Do you have a Finnish social security number?"}
              :rules {:toggle-ssn-based-fields :ssn}
              :no-blank-option true
              :exclude-from-answers true
              :id :have-finnish-ssn})
      (assoc :options [(dropdown-option "true" {:fi "Kyllä" :sv "Ja" :en "Yes"} :default-value true)
                       (dropdown-option "false" {:fi "Ei" :sv "Nej" :en "No"})])))

(defn ^:private ssn-component
  []
  (assoc (text-field {:fi "Henkilötunnus" :sv "Personbeteckning" :en "Social security number"} :size "S" :id :ssn)
         :rules {:update-gender-and-birth-date-based-on-ssn :gender}
         :validators [:ssn :required]))

(defn ^:private gender-section
  []
  (-> (component/dropdown)
      (merge {:label           {:fi "Sukupuoli" :sv "Kön" :en "Gender"}
              :validators      [:required]
              :id              :gender
              :koodisto-source {:uri "sukupuoli" :version 1}})))

(defn ^:private birthdate-and-gender-component
  []
  (component/row-section
    [(merge-with merge
                 (text-field
                   {:fi "Syntymäaika" :sv "Födelsetid" :en "Date of birth"}
                   :size "S"
                   :id :birth-date
                   :validators [:past-date :required])
                 {:params {:placeholder {:fi "pp.kk.vvvv"
                                         :sv "dd.mm.åååå"
                                         :en "dd.mm.yyyy"}}})
     (gender-section)]))

(defn- ssn-birthdate-gender-wrapper []
  (assoc
    (component/row-section
      [(ssn-component)
       (birthdate-and-gender-component)])
    :child-validator :birthdate-and-gender-component))

(defn- passport-number
  []
  (text-field {:fi "Passin numero" :sv "Passnummer" :en "Passport number"}
              :size "M" :id :passport-number  :validators []))

(defn- national-id-number
  []
  (text-field {:fi "Kansallinen ID-tunnus" :sv "Nationellt ID-signum" :en "National ID number"}
              :size "M" :id :national-id-number :validators []))

(defn- birthplace
  []
  (text-field {:fi "Syntymäpaikka ja -maa" :sv "Födelseort och -land" :en "Place and country of birth"}
              :size "M" :id :birthplace :validators [:birthplace]))

(defn ^:private email-component
  []
  (text-field {:fi "Sähköpostiosoite" :sv "E-postadress" :en "E-mail address"} :id :email :validators [:required :email]))

(defn ^:private phone-component
  []
  (text-field {:fi "Matkapuhelin" :sv "Mobiltelefonnummer" :en "Mobile phone number"} :id :phone :validators [:required :phone]))

(defn ^:private street-address-component
  []
  (text-field {:fi "Katuosoite" :sv "Näraddress" :en "Address"} :size "M" :id :address))

(defn ^:private home-town-component
  []
  (text-field {:fi "Kotikunta" :sv "Hemkommun" :en "Home town"}
              :id :home-town
              :validators [:home-town]
              :exclude-from-answers-if-hidden true))

(defn- city-component
  []
  (text-field {:fi "Kaupunki" :sv "Stad" :en "City"}
              :id :city
              :validators [:city]
              :exclude-from-answers-if-hidden true))

(defn ^:private postal-code-component
  []
  (text-field
    {:fi "Postinumero" :sv "Postnummer" :en "Postal code"}
    :size "S"
    :id :postal-code
    :rules {:select-postal-office-based-on-postal-code :postal-office}
    :validators [:postal-code]))

(defn ^:private postal-office-component
  []
  (text-field {:fi "Postitoimipaikka" :sv "Postkontor" :en "Postal office"}
              :id :postal-office
              :validators [:postal-office]
              :exclude-from-answers-if-hidden true))

(defn ^:private postal-office-section
  []
  (component/row-section [(postal-code-component)
                          (postal-office-component)]))

(defn ^:private native-language-section
  []
  (-> (component/dropdown)
      (merge {:label {:fi "Äidinkieli" :sv "Modersmål" :en "Native language"}
              :validators [:required]
              :id :language
              :koodisto-source {:uri "kieli" :version 1 :default-option "suomi"}})))

(defn person-info-module
  []
  (merge (component/form-section)
    {:label           {:fi "Henkilötiedot"
                       :sv "Personuppgifter"
                       :en "Personal information"}
     :label-amendment {:fi "(Osio lisätään automaattisesti lomakkeelle)"
                       :sv "Partitionen automatiskt lägga formen"
                       :en "The section will be automatically added to the application"}
     :children        [(first-name-section)
                       (last-name-component)
                       (nationality-component)
                       (have-finnish-ssn-component)
                       (ssn-birthdate-gender-wrapper)
                       (birthplace)
                       (passport-number)
                       (national-id-number)
                       (email-component)
                       (phone-component)
                       (country-of-residence-component)
                       (street-address-component)
                       (postal-office-section)
                       (home-town-component)
                       (city-component)
                       (native-language-section)]
     :module          :person-info}))
