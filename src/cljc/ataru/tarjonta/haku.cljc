(ns ataru.tarjonta.haku
  (:require [clojure.string :as string]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [clj-time.format :as format]
            [ataru.constants :refer [hakutapa-jatkuva-haku]]))

(defn toisen-asteen-yhteishaku?
  [haku]
  (let [kohdejoukko-uri (get haku :kohdejoukko-uri)
        kohdejoukko     (when (some? kohdejoukko-uri)
                          (-> kohdejoukko-uri
                            (string/split #"#")
                            first))
        yhteishaku?     (get haku :yhteishaku)]
    (boolean
      (and yhteishaku?
        (= kohdejoukko "haunkohdejoukko_11")))))

(defn jatkuva-haku?
  [haku]
  (some-> (:hakutapa-uri haku)
          (string/starts-with? hakutapa-jatkuva-haku)))

; In case of peruskoulun jälkeisen koulutuksen yhteishaku, it's specified
; that lähtökoulu information should be based on whatever school the student
; was in up to 1st of June of the application year. In other applications,
; we use application end date.
(defn get-lahtokoulu-cutoff-timestamp
  [hakuvuosi tarjonta-info]
  (let [haku-end (get-in tarjonta-info [:tarjonta :hakuaika :end])
        lahtokoulu-yhteishaku-cutoff-date (time/date-time hakuvuosi 5 30)]
    (if (toisen-asteen-yhteishaku? (:tarjonta tarjonta-info))
      (coerce/to-timestamp lahtokoulu-yhteishaku-cutoff-date)
      haku-end)))

; If there's a cutoff timestamp given, only consider luokka data still ongoing on that date.
(defn filter-opiskelija-by-cutoff-timestamp
  [cutoff-timestamp alkupvm-string loppupvm-string]
  (let [start-date (coerce/from-string alkupvm-string)
        end-date (coerce/from-string loppupvm-string)
        cutoff-date (coerce/from-long cutoff-timestamp)]
    (and (some? start-date)
         (some? end-date)
         (time/before? start-date cutoff-date)
         (time/after? end-date cutoff-date))))

(defn- in-datetime-period
  [period-start period-end checked-datetime]
  (and (or (time/after? checked-datetime period-start)
           (time/equal? checked-datetime period-start))
       (or (time/before? checked-datetime period-end)
           (time/equal? checked-datetime period-end))))

(defn filter-by-jatkuva-haku-hakemus-hakukausi
  [hakemus-datetime loppupvm-string]
  (let [school-end-date (coerce/from-string loppupvm-string)
        school-end-year (time/year school-end-date)
        spring-period-start (time/date-time school-end-year 1 1)
        spring-period-end (time/date-time school-end-year 7 31)
        hakemus-spring-period-end (time/date-time school-end-year 8 31)]
    (if (in-datetime-period spring-period-start spring-period-end school-end-date)
      (in-datetime-period spring-period-start hakemus-spring-period-end hakemus-datetime)
      (in-datetime-period (time/date-time school-end-year 8 1)(time/date-time (+ 1 school-end-year) 1 31)
                          hakemus-datetime))))

(defn resolve-lahtokoulu-vuodet-jatkuva-haku
  [application]
  (let [hakemus-datetime (format/parse (:date-time format/formatters) (:created-time application))
        hakemus-year (time/year hakemus-datetime)]
    (if (in-datetime-period (time/date-time hakemus-year 1 1) (time/date-time hakemus-year 1 31) hakemus-datetime)
      [hakemus-year (- hakemus-year 1)]
      [hakemus-year])))