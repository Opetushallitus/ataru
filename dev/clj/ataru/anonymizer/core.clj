(ns ataru.anonymizer.core
  (:require [ataru.anonymizer.anonymizer-application-store :as application-store]
            [ataru.anonymizer.data :as data]
            [taoensso.timbre :as log]
            [ataru.anonymizer.ssn-generator :as ssn-gen]))

(defn- anonymize [fake-persons applications {:keys [key preferred_name last_name content person_oid] :as application}]
  (if-let [fake-person (get fake-persons person_oid)]
    (let [{:keys [gender first-name last-name address fake-ssn phone email postal-code]} (first fake-person)
          anonymize-answer (fn [{:keys [key value] :as answer}]
                               (let [value (case key
                                             "gender"         gender
                                             "first-name"     first-name
                                             "preferred-name" first-name
                                             "last-name"      last-name
                                             "address"        address
                                             "ssn"            fake-ssn
                                             "phone"          phone
                                             "email"          email
                                             "postal-code"    postal-code
                                             "postal-office"  "Helsinki"
                                             "home-town"      "Äkäslompolo"
                                             value)]
                                 (assoc answer :value value)))
          content          (clojure.walk/prewalk (fn [x]
                                                     (cond-> x
                                                       (map? x)
                                                       (anonymize-answer)))
                             content)
          application      (merge application {:preferred_name first-name
                                               :last_name      last-name
                                               :ssn            fake-ssn
                                               :content        content})]
      (cond-> (update applications :applications conj application)
        (not (contains? applications key))
        (update key merge {:first-name  first-name
                           :last-name   last-name
                           :address     address
                           :phone       phone
                           :email       email
                           :postal-code postal-code})))
    applications))

(defn fake-person->ataru-person [{:keys [sukupuoli
                                         toinennimi
                                         hetu_aes
                                         syntymaaika
                                         sahkopostiosoite
                                         sukunimi
                                         hetu_sha
                                         hetu
                                         etunimi
                                         puhelinnumero
                                         personOid
                                         lahiosoite]}]
  {:person-oid  personOid
   :fake-ssn    hetu
   :address     lahiosoite
   :email       sahkopostiosoite
   :last-name   sukunimi
   :phone       puhelinnumero
   :first-name  (str etunimi " " toinennimi)
   :postal-code "00100"
   :gender      sukupuoli})

(defn file->fake-persons [file]
  (->> file
       (slurp)
       (clojure.string/split-lines)
       (map (comp fake-person->ataru-person
                  clojure.walk/keywordize-keys
                  cheshire.core/parse-string))
       (group-by :person-oid)))

(defn anonymize-data [& args]
  (let [fake-persons (file->fake-persons (first args))]
    (doseq [application (->> (application-store/get-all-applications)
                             (reduce (partial anonymize fake-persons) {})
                             :applications)]
      (application-store/update-application application)
      (log/info (str "Anonymized application " (:id application) " with key " (:key application))))))
