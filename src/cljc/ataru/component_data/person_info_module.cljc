  (ns ataru.component-data.person-info-module
    (:require [ataru.component-data.component :as component]
              [ataru.translations.texts :refer [person-info-module-texts general-texts]]
              [clojure.walk]))

; validators defined in ataru.hakija.application-validators

(defn ^:private text-field
  [labels & {:keys [size id validators rules params blur-rules exclude-from-answers-if-hidden metadata] :or
                   {size "M" validators [:required] rules {} params {} blur-rules {} exclude-from-answers-if-hidden false}}]
  (-> (component/text-field metadata)
      (assoc :id id)
      (assoc :rules rules)
      (assoc :label labels)
      (assoc :validators validators)
      (assoc-in [:params :size] size)
      (cond-> (not (empty? params)) (assoc :params params))
      (cond-> (not (empty? blur-rules)) (assoc :blur-rules blur-rules))
      (cond-> exclude-from-answers-if-hidden (assoc :exclude-from-answers-if-hidden true))))

(defn ^:private first-name-component
  [metadata]
  (text-field (:forenames person-info-module-texts)
              :id :first-name
              :metadata metadata
              :blur-rules {:prefill-preferred-first-name :main-first-name}))

(defn ^:private preferred-name-component
  [metadata]
  (text-field (:main-forename person-info-module-texts)
              :size "S"
              :id :preferred-name
              :metadata metadata
              :validators [:required :main-first-name]))

(defn ^:private first-name-section
  [metadata]
  (component/row-section [(first-name-component metadata)
                          (preferred-name-component metadata)]
    metadata))

(defn ^:private last-name-component
  [metadata]
  (text-field (:surname person-info-module-texts) :id :last-name :metadata metadata))

(defn ^:private dropdown-option
  [value labels & {:keys [default-value] :or {default-value false}}]
  (-> (component/dropdown-option)
      (merge {:value value :label labels}
             (when default-value
               {:default-value default-value}))))

(defn ^:private nationality-component
  [metadata]
  (-> (component/question-group metadata)
      (merge {:label    (:nationality person-info-module-texts)
              :children [(merge (dissoc (component/dropdown metadata) :validators)
                                {:label           (:nationality person-info-module-texts)
                                 :options         []
                                 :id              :nationality
                                 :validators      [:required]
                                 :rules           {:swap-ssn-birthdate-based-on-nationality [:ssn :birth-date]}
                                 :koodisto-source {:uri "maatjavaltiot2" :version 1 :default-option "Suomi"}})]})))

(defn- country-of-residence-component
  [metadata]
  (-> (component/dropdown metadata)
      (merge {:label (:country-of-residence person-info-module-texts)
              :validators [:required]
              :rules {:change-country-of-residence nil}
              :id :country-of-residence
              :koodisto-source {:uri "maatjavaltiot2" :version 1 :default-option "Suomi"}})))

(defn- have-finnish-ssn-component
  [metadata]
  (-> (component/dropdown metadata)
      (merge {:label (:have-finnish-ssn person-info-module-texts)
              :rules {:toggle-ssn-based-fields :ssn}
              :no-blank-option true
              :exclude-from-answers true
              :id :have-finnish-ssn})
      (assoc :options [(dropdown-option "true" (:yes general-texts) :default-value true)
                       (dropdown-option "false" (:no general-texts))])))

(defn ^:private ssn-component
  [metadata]
  (assoc (text-field (:ssn person-info-module-texts) :size "S" :id :ssn :metadata metadata)
         :rules {:update-gender-and-birth-date-based-on-ssn :gender}
         :validators [:ssn :required]))

(defn ^:private gender-section
  [metadata]
  (-> (component/dropdown metadata)
      (merge {:label           (:gender person-info-module-texts)
              :validators      [:required]
              :id              :gender
              :koodisto-source {:uri "sukupuoli" :version 1}})))

(defn ^:private birthdate-and-gender-component
  [metadata]
  (component/row-section
    [(merge-with merge
                 (text-field
                   (:birth-date person-info-module-texts)
                   :size "S"
                   :id :birth-date
                   :metadata metadata
                   :validators [:past-date :required])
                 {:params {:placeholder (:date-formats person-info-module-texts)}})
     (gender-section metadata)]
    metadata))

(defn- ssn-birthdate-gender-wrapper
  [metadata]
  (assoc
    (component/row-section
      [(ssn-component metadata)
       (birthdate-and-gender-component metadata)]
      metadata)
    :child-validator :birthdate-and-gender-component))

(defn- passport-number
  [metadata]
  (text-field (:passport-number person-info-module-texts)
              :size "M" :id :passport-number  :validators [] :metadata metadata))

(defn- national-id-number
  [metadata]
  (text-field (:national-id-number person-info-module-texts)
              :size "M" :id :national-id-number :validators [] :metadata metadata))

(defn- birthplace
  [metadata]
  (text-field (:birthplace person-info-module-texts)
              :size "M" :id :birthplace :validators [:birthplace] :metadata metadata))

(defn ^:private email-component
  [metadata]
  (text-field (:email person-info-module-texts) :id :email :validators [:required :email] :metadata metadata))

(defn ^:private phone-component
  [metadata]
  (text-field (:phone person-info-module-texts) :id :phone :validators [:required :phone] :metadata metadata))

(defn ^:private street-address-component
  [metadata]
  (text-field (:address person-info-module-texts) :size "M" :id :address :metadata metadata))

(defn ^:private home-town-component
  [metadata]
  (merge (component/dropdown metadata)
         {:label (:home-town person-info-module-texts)
          :id :home-town
          :validators [:home-town]
          :koodisto-source {:uri "kunta" :version 1}
          :exclude-from-answers-if-hidden true}))

(defn- city-component
  [metadata]
  (text-field (:city person-info-module-texts)
              :id :city
              :metadata metadata
              :validators [:city]
              :exclude-from-answers-if-hidden true))

(defn ^:private postal-code-component
  [metadata]
  (text-field
    (:postal-code person-info-module-texts)
    :size "S"
    :id :postal-code
    :metadata metadata
    :rules {:select-postal-office-based-on-postal-code :postal-office}
    :validators [:postal-code]))

(defn ^:private postal-office-component
  [metadata]
  (text-field (:postal-office person-info-module-texts)
              :id :postal-office
              :metadata metadata
              :validators [:postal-office]
              :exclude-from-answers-if-hidden true))

(defn ^:private postal-office-section
  [metadata]
  (component/row-section [(postal-code-component metadata)
                          (postal-office-component metadata)]
    metadata))

(defn ^:private native-language-section
  [metadata]
  (-> (component/dropdown metadata)
      (merge {:label (:language person-info-module-texts)
              :validators [:required]
              :id :language
              :koodisto-source {:uri "kieli" :version 1 :default-option "suomi"}})))

(defn person-info-module
  []
  (let [metadata {:created-by  {:name "system"
                                :oid  "system"
                                :date "1970-01-01T00:00:00Z"}
                  :modified-by {:name "system"
                                :oid  "system"
                                :date "1970-01-01T00:00:00Z"}}]
    (merge (component/form-section metadata)
           {:label           (:label person-info-module-texts)
            :label-amendment (:label-amendment person-info-module-texts)
            :children        [(first-name-section metadata)
                              (last-name-component metadata)
                              (nationality-component metadata)
                              (have-finnish-ssn-component metadata)
                              (ssn-birthdate-gender-wrapper metadata)
                              (birthplace metadata)
                              (passport-number metadata)
                              (national-id-number metadata)
                              (email-component metadata)
                              (phone-component metadata)
                              (country-of-residence-component metadata)
                              (street-address-component metadata)
                              (postal-office-section metadata)
                              (home-town-component metadata)
                              (city-component metadata)
                              (native-language-section metadata)]
            :module          :person-info})))
