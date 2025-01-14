(ns ataru.anonymizer.core
  (:require [ataru.anonymizer.anonymizer-application-store :as application-store]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            clojure.string
            clojure.walk
            [taoensso.timbre :as log])
  (:import (java.time LocalDate)
           (java.time.format DateTimeFormatter)))

(defn- anonymize-attachment
  [answer attachment-key]
  (update answer :value (partial map (fn [v] (if (string? v)
                                               attachment-key
                                               (map (constantly attachment-key) v))))))

(defn- anonymize [fake-person attachment-key application]
  (letfn [(anonymize-answer [{:keys [key value] :as answer}]
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
                          "birth-date"     (when (:birth-date fake-person)
                                             (.format (LocalDate/parse
                                                        (:birth-date fake-person)
                                                        (DateTimeFormatter/ofPattern "yyyy-MM-dd"))
                                                      (DateTimeFormatter/ofPattern "dd.MM.yyyy")))
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
                        :dob              (:birth-date fake-person)
                        :tunnistautuminen (or (some->> (-> application
                                                           :tunnistautuminen
                                                           :session
                                                           :data
                                                           :auth-type)
                                                       (update-in {} [:session :data] assoc :auth-type))
                                              {})
                        :content          (update (:content application) :answers #(map anonymize-answer %))})))

(defn get-contact-value
  [contacts yhteystietotyyppi]
  (->> contacts
       (filter #(= (:yhteystieto_arvo_tyyppi %) yhteystietotyyppi))
       first
       :yhteystieto_arvo))

(defn fake-person->ataru-person
  [contacts]
  (fn [{:keys [henkilo_oid
               hetu
               sukupuoli
               syntymaaika
               sukunimi
               etunimet
               aidinkieli
               kotikunta
               kansalaisuus
               ; nämäkin ONR-datasta löytyy; pidetään tallessa jos päätetään myöhemmin käyttää anonymisoinnissa
               ;turvakielto
               ;yksiloityvtj
               ;master_oid
               ;linkitetyt_oidit
               ]}]
    (let [person-contacts (get contacts henkilo_oid)]
      {:person-oid           henkilo_oid
       :first-name           etunimet
       :preferred-name       (first (clojure.string/split etunimet #" "))
       :last-name            sukunimi
       :nationality          kansalaisuus
       :fake-ssn             hetu
       :birth-date           (when (re-find #"^\d{4}-\d{2}-\d{2}" syntymaaika)
                               (subs syntymaaika 0 10))
       :gender               sukupuoli
       :email                (get-contact-value person-contacts "YHTEYSTIETO_SAHKOPOSTI")
       :phone                (get-contact-value person-contacts "YHTEYSTIETO_PUHELINNUMERO")
       ; maata ei voida käyttää sellaisenaan, YHTEYSTIETO_MAA on tekstimuodossa mitä sattuu,
       ; ja :country-of-residence odottaa numeerista maakoodia
       ;:country-of-residence (get-contact-value person-contacts "YHTEYSTIETO_MAA")
       :address              (get-contact-value person-contacts "YHTEYSTIETO_KATUOSOITE")
       :postal-code          (get-contact-value person-contacts "YHTEYSTIETO_POSTINUMERO")
       :postal-office        (or (get-contact-value person-contacts "YHTEYSTIETO_KAUPUNKI")
                                 (get-contact-value person-contacts "YHTEYSTIETO_KUNTA"))
       :home-town            kotikunta
       :language             aidinkieli})))

(defn file->fake-persons [person-file contact-file]
  (log/info "Indexing persons")
  (time
    (let [contacts (with-open [reader (io/reader contact-file)]
                     (group-by :henkilo_oid
                               (->> (let [data (csv/read-csv reader)]
                                      (map zipmap
                                           (->> (first data)
                                                (map keyword)
                                                repeat)
                                           (rest data))))))]
      (with-open [reader (io/reader person-file)]
        (group-by :person-oid
                  (->> (let [data (csv/read-csv reader)]
                         (map zipmap
                              (->> (first data)
                                   (map keyword)
                                   repeat)
                              (rest data)))
                       (map (fake-person->ataru-person contacts))))))))

(defn anonymize-data [& args]
  (let [[person-csv contact-csv attachment-key skip-shutdown] args]
    (assert (not (clojure.string/blank? person-csv)))
    (assert (not (clojure.string/blank? contact-csv)))
    (assert (not (clojure.string/blank? attachment-key)))
    (let [fake-persons     (file->fake-persons person-csv contact-csv)
          _                (log/info "Found" (count (keys fake-persons)) "persons")
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
    (when-not skip-shutdown
      (log/info "Shutting down")
      (shutdown-agents))))
