(ns ataru.applications.lahtokoulu-util
  (:require [ataru.tarjonta.haku :refer [toisen-asteen-yhteishaku?]]
            [ataru.time :as t]
            [ataru.time.coerce :as c]))

; In case of peruskoulun jälkeisen koulutuksen yhteishaku, it's specified
; that lähtökoulu information should be based on whatever school the student
; was in up to 1st of June of the application year. In other applications,
; we use application end date.
(defn get-lahtokoulu-cutoff-timestamp
  [hakuvuosi tarjonta-info]
  (let [haku-end (get-in tarjonta-info [:tarjonta :hakuaika :end])
        lahtokoulu-yhteishaku-cutoff-date (.atStartOfDay (t/local-date hakuvuosi 5 30)
                                                         (t/time-zone-for-id "UTC"))]
    (if (toisen-asteen-yhteishaku? (:tarjonta tarjonta-info))
      (c/to-timestamp lahtokoulu-yhteishaku-cutoff-date)
      haku-end)))

; If there's a cutoff timestamp given, only consider luokka data still ongoing on that date.
(defn filter-opiskelija-by-cutoff-timestamp
  [cutoff-timestamp alkupvm-string loppupvm-string]
  (let [start-date (c/from-string alkupvm-string)
        end-date (c/from-string loppupvm-string)
        cutoff-date (c/from-long cutoff-timestamp)]
    (and (some? start-date)
         (some? end-date)
         (t/before? start-date cutoff-date)
         (t/after? end-date cutoff-date))))

(defn- in-datetime-period
  [period-start period-end checked-datetime]
  (and (or (t/after? checked-datetime period-start)
           (t/equal? checked-datetime period-start))
       (or (t/before? checked-datetime period-end)
           (t/equal? checked-datetime period-end))))

(defn filter-by-jatkuva-haku-hakemus-hakukausi
  [hakemus-datetime loppupvm-string]
  (let [school-end-date (c/from-string loppupvm-string)
        school-end-year (t/year school-end-date)
        spring-period-start (t/date-time school-end-year 1 1)
        spring-period-end (t/date-time school-end-year 7 31)
        hakemus-spring-period-end (t/date-time school-end-year 8 31)]
    (if (in-datetime-period spring-period-start spring-period-end school-end-date)
      (in-datetime-period spring-period-start hakemus-spring-period-end hakemus-datetime)
      (in-datetime-period (t/date-time school-end-year 8 1) (t/date-time (+ 1 school-end-year) 1 31)
                          hakemus-datetime))))

(defn resolve-lahtokoulu-vuodet-jatkuva-haku
  [hakemus-datetime]
  (let [hakemus-year (t/year hakemus-datetime)]
    (if (in-datetime-period (t/date-time hakemus-year 1 1) (t/date-time hakemus-year 1 31) hakemus-datetime)
      [hakemus-year (- hakemus-year 1)]
      [hakemus-year])))
