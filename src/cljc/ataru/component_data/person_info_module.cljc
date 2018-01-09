  (ns ataru.component-data.person-info-module
    (:require [ataru.component-data.component :as component]
              [ataru.translations.texts :refer [person-info-module-texts general-texts]]
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
  (text-field (:forenames person-info-module-texts)
              :id :first-name
              :blur-rules {:prefill-preferred-first-name :main-first-name}))

(defn ^:private preferred-name-component
  []
  (text-field (:main-forename person-info-module-texts)
              :size "S"
              :id :preferred-name
              :validators [:required :main-first-name]))

(defn ^:private first-name-section
  []
  (component/row-section [(first-name-component)
                          (preferred-name-component)]))

(defn ^:private last-name-component
  []
  (text-field (:surname person-info-module-texts) :id :last-name))

(defn ^:private dropdown-option
  [value labels & {:keys [default-value] :or {default-value false}}]
  (-> (component/dropdown-option)
      (merge {:value value :label labels}
             (when default-value
               {:default-value default-value}))))

(defn ^:private nationality-component
  []
  (-> (component/dropdown)
      (merge {:label (:nationality person-info-module-texts)
              :validators [:required]
              :rules {:swap-ssn-birthdate-based-on-nationality [:ssn :birth-date]}
              :id :nationality
              :koodisto-source {:uri "maatjavaltiot2" :version 1 :default-option "Suomi"}})))

(defn- country-of-residence-component
  []
  (-> (component/dropdown)
      (merge {:label (:country-of-residence person-info-module-texts)
              :validators [:required]
              :rules {:change-country-of-residence nil}
              :id :country-of-residence
              :koodisto-source {:uri "maatjavaltiot2" :version 1 :default-option "Suomi"}})))

(defn- have-finnish-ssn-component
  []
  (-> (component/dropdown)
      (merge {:label (:have-finnish-ssn person-info-module-texts)
              :rules {:toggle-ssn-based-fields :ssn}
              :no-blank-option true
              :exclude-from-answers true
              :id :have-finnish-ssn})
      (assoc :options [(dropdown-option "true" (:yes general-texts) :default-value true)
                       (dropdown-option "false" (:no general-texts))])))

(defn ^:private ssn-component
  []
  (assoc (text-field (:ssn person-info-module-texts) :size "S" :id :ssn)
         :rules {:update-gender-and-birth-date-based-on-ssn :gender}
         :validators [:ssn :required]))

(defn ^:private gender-section
  []
  (-> (component/dropdown)
      (merge {:label           (:gender person-info-module-texts)
              :validators      [:required]
              :id              :gender
              :koodisto-source {:uri "sukupuoli" :version 1}})))

(defn ^:private birthdate-and-gender-component
  []
  (component/row-section
    [(merge-with merge
                 (text-field
                   (:birth-date person-info-module-texts)
                   :size "S"
                   :id :birth-date
                   :validators [:past-date :required])
                 {:params {:placeholder (:date-formats person-info-module-texts)}})
     (gender-section)]))

(defn- ssn-birthdate-gender-wrapper []
  (assoc
    (component/row-section
      [(ssn-component)
       (birthdate-and-gender-component)])
    :child-validator :birthdate-and-gender-component))

(defn- passport-number
  []
  (text-field (:passport-number person-info-module-texts)
              :size "M" :id :passport-number  :validators []))

(defn- national-id-number
  []
  (text-field (:national-id-number person-info-module-texts)
              :size "M" :id :national-id-number :validators []))

(defn- birthplace
  []
  (text-field (:birthplace person-info-module-texts)
              :size "M" :id :birthplace :validators [:birthplace]))

(defn ^:private email-component
  []
  (text-field (:email person-info-module-texts) :id :email :validators [:required :email]))

(defn ^:private phone-component
  []
  (text-field (:phone person-info-module-texts) :id :phone :validators [:required :phone]))

(defn ^:private street-address-component
  []
  (text-field (:address person-info-module-texts) :size "M" :id :address))

(defn ^:private home-town-component
  []
  (text-field (:home-town person-info-module-texts)
              :id :home-town
              :validators [:home-town]
              :exclude-from-answers-if-hidden true))

(defn- city-component
  []
  (text-field (:city person-info-module-texts)
              :id :city
              :validators [:city]
              :exclude-from-answers-if-hidden true))

(defn ^:private postal-code-component
  []
  (text-field
    (:postal-code person-info-module-texts)
    :size "S"
    :id :postal-code
    :rules {:select-postal-office-based-on-postal-code :postal-office}
    :validators [:postal-code]))

(defn ^:private postal-office-component
  []
  (text-field (:postal-office person-info-module-texts)
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
      (merge {:label (:language person-info-module-texts)
              :validators [:required]
              :id :language
              :koodisto-source {:uri "kieli" :version 1 :default-option "suomi"}})))

(defn person-info-module
  []
  (merge (component/form-section)
    {:label           (:label person-info-module-texts)
     :label-amendment (:label-amendment person-info-module-texts)
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
