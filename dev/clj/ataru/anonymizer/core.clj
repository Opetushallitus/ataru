(ns ataru.anonymizer.core
  (:require [ataru.anonymizer.anonymizer-application-store :as application-store]
            [ataru.anonymizer.data :as data]
            [taoensso.timbre :as log]
            [ataru.anonymizer.ssn-generator :as ssn-gen]))

(defn- date-to-iso8601
  [date]
  (let [[_ d m y] (re-matches #"(\d{2}).(\d{2}).(\d{4})" date)]
    (str y "-" m "-" d)))

(defn- anonymize [fake-person application]
  (let [anonymize-answer (fn [{:keys [key value] :as answer}]
                           (let [value (case key
                                         "gender"         (:gender fake-person)
                                         "first-name"     (:first-name fake-person)
                                         "preferred-name" (:preferred-name fake-person)
                                         "last-name"      (:last-name fake-person)
                                         "address"        (:address fake-person)
                                         "ssn"            (:fake-ssn fake-person)
                                         "phone"          (:phone fake-person)
                                         "email"          (:email fake-person)
                                         "postal-code"    (:postal-code fake-person)
                                         "birth-date"     (:birth-date fake-person)
                                         "postal-office"  (:postal-office fake-person)
                                         "home-town"      (:home-town fake-person)
                                         value)]
                             (assoc answer :value value)))]
    (merge application {:preferred_name (:preferred-name fake-person)
                        :last_name      (:last-name fake-person)
                        :ssn            (:fake-ssn fake-person)
                        :email          (:email fake-person)
                        :dob            (date-to-iso8601 (:birth-date fake-person))
                        :content        (update (:content application) :answers #(map anonymize-answer %))})))

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
  {:person-oid     personOid
   :fake-ssn       hetu
   :address        lahiosoite
   :email          sahkopostiosoite
   :last-name      sukunimi
   :phone          puhelinnumero
   :first-name     (str etunimi " " toinennimi)
   :preferred-name etunimi
   :postal-code    "00100"
   :postal-office  "Helsinki"
   :home-town      "091"
   :gender         sukupuoli
   :birth-date     syntymaaika})

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
    (doseq [application (application-store/get-all-applications)]
      (if-let [fake-person (first (get fake-persons (:person_oid application)))]
        (do (application-store/update-application (anonymize fake-person application))
            (log/info "Anonymized application" (:id application)))
        (log/info "Did not anonymize application" (:id application))))))
