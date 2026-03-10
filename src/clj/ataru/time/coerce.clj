(ns ataru.time.coerce
  (:require [ataru.time :as time])
  (:import [java.sql Date Timestamp]
           [java.time Instant LocalDate LocalDateTime OffsetDateTime ZonedDateTime]))

(defn from-long
  [value]
  (cond
    (nil? value) nil
    (number? value) (ZonedDateTime/ofInstant (Instant/ofEpochMilli (long value)) (time/default-zone))
    (instance? ZonedDateTime value) value
    (instance? Instant value) (ZonedDateTime/ofInstant value (time/default-zone))
    (instance? java.time.LocalDateTime value) (.atZone ^java.time.LocalDateTime value (time/default-zone))
    (instance? java.time.LocalDate value) (.atStartOfDay ^java.time.LocalDate value (time/default-zone))
    :else (ZonedDateTime/ofInstant (time/to-instant value) (time/default-zone))))

(defn to-long
  [t]
  (cond
    (nil? t) nil
    (number? t) (long t)
    :else (-> (time/to-instant t)
              (.toEpochMilli))))

(defn from-string
  [value]
  (when (some? value)
    (or (try (ZonedDateTime/parse value) (catch Exception _ nil))
        (try (-> value OffsetDateTime/parse .toZonedDateTime) (catch Exception _ nil))
        (try (-> value LocalDateTime/parse (.atZone (time/default-zone))) (catch Exception _ nil))
        (try (-> value LocalDate/parse (.atStartOfDay (time/default-zone))) (catch Exception _ nil)))))

(defn to-timestamp
  [t]
  (to-long t))

(defn from-sql-date
  [^Date date]
  (when date
    (-> (.toLocalDate date)
        (.atStartOfDay (time/default-zone)))))

(defn from-sql-time
  [^Timestamp ts]
  (when ts
    (ZonedDateTime/ofInstant (.toInstant ts) (time/default-zone))))

(defn to-sql-time
  [t]
  (when t
    (Timestamp/from (time/to-instant t))))
