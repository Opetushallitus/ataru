  (ns ataru.virkailija.component-data.person-info-module
  (:require [ataru.virkailija.component-data.component :as component]))

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
                               :options [(dropdown-option "fi" {:fi "Suomi" :sv "Finland"})
                                         (dropdown-option "sv" {:fi "Ruotsi" :sv "Sverige"})]
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
  (text-field {:fi "Matkapuhelin" :sv "Mobiltelefonnummer"} :id :phone))

(defn ^:private street-address-component
  []
  (text-field {:fi "Katuosoite" :sv "Adress"} :size "L" :id :address))

(defn ^:private municipality-component
  []
  (text-field {:fi "Kotikunta" :sv "Bostadsort"} :id :municipality))

(defn ^:private postal-code-component
  []
  (text-field {:fi "Postinumero" :sv "Postnummer"} :size "S" :id :postal-code :validators ["postal-code"]))

(defn ^:private municipality-section
  []
  (component/row-section [(municipality-component)
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
                                                (municipality-section)
                                                (native-language-section)]
                                     :module :person-info})))
