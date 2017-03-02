(ns ataru.person-service.authentication-service-person-extract
  (:require [ataru.person-service.birth-date-converter :refer [convert-birth-date]]))

(defn- extract-field [{:keys [answers]} field]
  (some (fn [{:keys [key value]}]
          (when (= key field)
            value))
        answers))

(defn- extract-birth-date [application]
  (let [finnish-format-date (extract-field application "birth-date")]
    (if-not finnish-format-date (throw (Exception. "Expected a birth-date in application")))
    (convert-birth-date finnish-format-date)))

(defn extract-person-from-application [application]
  (let [email        (extract-field application "email")
        basic-fields {:email          email
                      :firstName      (extract-field application "first-name")
                      :lastName       (extract-field application "last-name")
                      :gender         (extract-field application "gender")
                      :nativeLanguage (extract-field application "language")
                      :nationality    (extract-field application "nationality")
                      :idpEntitys     [{:idpEntityId "oppijaToken" :identifier email}]}
        person-id    (extract-field application "ssn")]
    (if person-id
      (assoc basic-fields :personId (clojure.string/upper-case person-id))
      (assoc basic-fields :birthDate (extract-birth-date application)))))
