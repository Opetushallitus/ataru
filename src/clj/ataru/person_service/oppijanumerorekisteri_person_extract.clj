(ns ataru.person-service.oppijanumerorekisteri-person-extract
  (:require [ataru.person-service.birth-date-converter :refer [convert-birth-date]]
            [clojure.string]))

(defn- extract-field [{:keys [answers]} field]
  (some (fn [{:keys [key value]}]
          (when (= key field)
            value))
        answers))

(defn- extract-birth-date [application]
  (let [finnish-format-date (extract-field application "birth-date")]
    (when-not finnish-format-date
      (throw (Exception. "Expected a birth-date in application")))
    (convert-birth-date finnish-format-date)))

(defn- extract-nationalities
  [nationalities]
  (mapv (fn [[nat-code & _]] {:kansalaisuusKoodi nat-code}) nationalities))

(defn extract-person-from-application [application]
  (let [email        (extract-field application "email")
        eidas-id     (:eidas-id application)
        basic-fields {:yhteystieto    [{:yhteystietoTyyppi "YHTEYSTIETO_SAHKOPOSTI"
                                        :yhteystietoArvo   email}]
                      :etunimet       (extract-field application "first-name")
                      :kutsumanimi    (extract-field application "preferred-name")
                      :sukunimi       (extract-field application "last-name")
                      :sukupuoli      (extract-field application "gender")
                      :aidinkieli     {:kieliKoodi (clojure.string/lower-case (extract-field application "language"))}
                      :asiointiKieli  {:kieliKoodi (:lang application)}
                      :kansalaisuus   (extract-nationalities (extract-field application "nationality"))
                      :henkiloTyyppi  "OPPIJA"}
        basic-fields (if (some? eidas-id)
                         (assoc basic-fields :eidas eidas-id)
                         basic-fields)
        person-id    (extract-field application "ssn")]
    (if person-id
      (assoc
       basic-fields
       :hetu (clojure.string/upper-case person-id)
       :eiSuomalaistaHetua false)
      (assoc
       basic-fields
       :syntymaaika (extract-birth-date application)
       :identifications [{:idpEntityId "oppijaToken" :identifier email}]
       :eiSuomalaistaHetua true))))
