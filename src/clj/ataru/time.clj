(ns ataru.time
  (:import [java.time
            Duration
            Instant
            LocalDate
            LocalDateTime
            LocalTime
            Period
            ZoneId
            ZonedDateTime]
           [java.time.temporal TemporalAmount]))

(def ^:private default-zone-id (ZoneId/systemDefault))


(defn default-zone
  []
  default-zone-id)

(defn time-zone-for-id
  [zone-id]
  (ZoneId/of zone-id))

(defn now
  ([] (ZonedDateTime/now default-zone-id))
  ([zone] (ZonedDateTime/now zone)))

(defn today
  ([] (LocalDate/now default-zone-id))
  ([zone] (LocalDate/now zone)))

(defn today-at-midnight
  ([] (.atStartOfDay (today) default-zone-id))
  ([zone] (.atStartOfDay (today zone) zone)))

(defn local-date
  [year month day]
  (LocalDate/of year month day))

(defn local-time
  [hour minute]
  (LocalTime/of hour minute))

(defn date-time
  ([year month day]
   (ZonedDateTime/of year month day 0 0 0 0 default-zone-id))
  ([year month day hour minute]
   (ZonedDateTime/of year month day hour minute 0 0 default-zone-id))
  ([year month day hour minute second]
   (ZonedDateTime/of year month day hour minute second 0 default-zone-id))
  ([year month day hour minute second millis]
   (ZonedDateTime/of year month day hour minute second (* millis 1000000) default-zone-id)))

(defn- millis-from
  [t]
  (try
    (.getMillis t)
    (catch Exception _
      nil)))

(defn- ->zoned-date-time
  [t]
  (cond
    (instance? ZonedDateTime t) t
    (instance? LocalDateTime t) (.atZone ^LocalDateTime t default-zone-id)
    (instance? LocalDate t) (.atStartOfDay ^LocalDate t default-zone-id)
    (instance? Instant t) (ZonedDateTime/ofInstant ^Instant t default-zone-id)
    (instance? java.time.OffsetDateTime t) (.toZonedDateTime ^java.time.OffsetDateTime t)
    (instance? java.sql.Timestamp t) (ZonedDateTime/ofInstant (.toInstant ^java.sql.Timestamp t) default-zone-id)
    (instance? java.util.Date t) (ZonedDateTime/ofInstant (.toInstant ^java.util.Date t) default-zone-id)
    (some? (millis-from t)) (ZonedDateTime/ofInstant (Instant/ofEpochMilli (long (millis-from t))) default-zone-id)
    :else (throw (ex-info "Unsupported temporal value" {:value t}))))

(defn- ->instant
  [t]
  (cond
    (instance? Instant t) t
    :else (.toInstant ^ZonedDateTime (->zoned-date-time t))))

(defn year
  [t]
  (.getYear ^ZonedDateTime (->zoned-date-time t)))

(defn month
  [t]
  (.getMonthValue ^ZonedDateTime (->zoned-date-time t)))

(defn day
  [t]
  (.getDayOfMonth ^ZonedDateTime (->zoned-date-time t)))

(defn hour
  [t]
  (.getHour ^ZonedDateTime (->zoned-date-time t)))

(defn minute
  [t]
  (.getMinute ^ZonedDateTime (->zoned-date-time t)))

(defn plus
  [t & amounts]
  (reduce (fn [acc amount]
            (.plus acc ^TemporalAmount amount))
          t
          amounts))

(defn minus
  [t & amounts]
  (reduce (fn [acc amount]
            (.minus acc ^TemporalAmount amount))
          t
          amounts))

(defn days
  [n]
  (Period/ofDays (int n)))

(defn weeks
  [n]
  (Period/ofWeeks (int n)))

(defn months
  [n]
  (Period/ofMonths (int n)))

(defn hours
  [n]
  (Duration/ofHours (long n)))

(defn minutes
  [n]
  (Duration/ofMinutes (long n)))

(defn seconds
  [n]
  (Duration/ofSeconds (long n)))

(defn before?
  [t1 t2]
  (.isBefore (->instant t1) (->instant t2)))

(defn after?
  [t1 t2]
  (.isAfter (->instant t1) (->instant t2)))

(defn equal?
  [t1 t2]
  (.equals (->instant t1) (->instant t2)))

(defn interval
  [start end]
  {:start start
   :end   end})

(defn within?
  [interval-value t]
  (let [start-inst (->instant (:start interval-value))
        end-inst   (->instant (:end interval-value))
        inst       (->instant t)]
    (and (not (.isBefore inst start-inst))
         (.isBefore inst end-inst))))

(defn to-time-zone
  [t zone]
  (cond
    (instance? ZonedDateTime t) (.withZoneSameInstant ^ZonedDateTime t zone)
    (instance? Instant t) (ZonedDateTime/ofInstant ^Instant t zone)
    (instance? LocalDateTime t) (.atZone ^LocalDateTime t zone)
    (instance? LocalDate t) (.atStartOfDay ^LocalDate t zone)
    :else (.withZoneSameInstant ^ZonedDateTime (->zoned-date-time t) zone)))

(defn from-time-zone
  [t zone]
  (cond
    (instance? ZonedDateTime t) (.withZoneSameLocal ^ZonedDateTime t zone)
    (instance? LocalDateTime t) (.atZone ^LocalDateTime t zone)
    (instance? LocalDate t) (.atStartOfDay ^LocalDate t zone)
    (instance? Instant t) (ZonedDateTime/ofInstant ^Instant t zone)
    :else (.withZoneSameLocal ^ZonedDateTime (->zoned-date-time t) zone)))

(defn with-time-at-start-of-day
  [t]
  (cond
    (instance? LocalDate t) (.atStartOfDay ^LocalDate t default-zone-id)
    :else (let [zdt  (->zoned-date-time t)
                zone (.getZone ^ZonedDateTime zdt)]
            (.atStartOfDay (.toLocalDate ^ZonedDateTime zdt) zone))))

(defn with-time
  [t local-time]
  (let [zdt  (->zoned-date-time t)
        zone (.getZone ^ZonedDateTime zdt)]
    (ZonedDateTime/of (.toLocalDate ^ZonedDateTime zdt) local-time zone)))

(defn to-instant
  [t]
  (->instant t))
