(ns ataru.anonymizer.core
  (:require [ataru.anonymizer.anonymizer-application-store :as application-store]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            clojure.string
            clojure.walk
            [taoensso.timbre :as log]))

(defn- date-to-iso8601
  [date]
  (let [[_ d m y] (re-matches #"(\d{2}).(\d{2}).(\d{4})" date)]
    (str y "-" m "-" d)))

(defn- anonymize-attachment
  [answer attachment-key]
  (update answer :value (partial map (fn [v] (if (string? v)
                                               attachment-key
                                               (map (constantly attachment-key) v))))))

(defn- anonymize [fake-person attachment-key application]
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
                             (cond-> (assoc answer :value value)
                                     (= "attachment" (:fieldType answer))
                                     (anonymize-attachment attachment-key))))]
    (merge application {:preferred_name   (:preferred-name fake-person)
                        :last_name        (:last-name fake-person)
                        :ssn              (:fake-ssn fake-person)
                        :email            (:email fake-person)
                        :dob              (date-to-iso8601 (:birth-date fake-person))
                        :tunnistautuminen (or (some->> (-> application
                                                           :tunnistautuminen
                                                           :session
                                                           :data
                                                           :auth-type)
                                                       (update-in {} [:session :data] assoc :auth-type))
                                              {})
                        :content          (update (:content application) :answers #(map anonymize-answer %))})))

(defn fake-person->ataru-person [{:keys [sukupuoli
                                         toinennimi
                                         syntymaaika
                                         sahkopostiosoite
                                         sukunimi
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
   :postal-office  "HELSINKI"
   :home-town      "091"
   :gender         sukupuoli
   :birth-date     syntymaaika})

(defn file->fake-persons [file]
  (log/info "Indexing persons")
  (time
    (with-open [reader (io/reader file)]
      (group-by :person-oid
        (->> (let [data (csv/read-csv reader)]
               (map zipmap
                    (->> (first data)
                         (map keyword)
                         repeat)
                    (rest data)))
             (map fake-person->ataru-person))))))

(defn anonymize-data [& args]
  (assert (not (clojure.string/blank? (second args))))
  (let [fake-persons     (file->fake-persons (first args))
        _                (log/info "Found" (count (keys fake-persons)) "persons")
        attachment-key   (second args)
        application-ids  (application-store/get-all-application-ids)
        last-id          (last application-ids)]
    (log/info "Anonymise" (count application-ids) "application ids")
    (time
      (dorun
        (pmap (fn [id]
                (let [application (application-store/get-application id)]
                  (if-let [fake-person (first (get fake-persons (:person_oid application)))]
                    (do (application-store/update-application (anonymize fake-person attachment-key application))
                      (when (or (= last-id id)
                                (= 0 (mod id 1000)))
                        (log/info "Anonymized application id" (:id application))))
                    (log/info "Did not anonymize application" (:id application)))))
              application-ids)))
    (log/info "Anonymize guardians")
    (time (application-store/anonymize-guardian!))
    (log/info "Anonymize long textareas")
    (time
      (dorun (pcalls application-store/anonymize-long-textareas-group!
                     application-store/anonymize-long-textareas-multi!
                     application-store/anonymize-long-textareas!)))
    (log/info "Regenerate application secrets")
    (time (application-store/regenerate-application-secrets!)))
  (when-not (first (nnext args))
    (log/info "Shutting down")
    (shutdown-agents)))
