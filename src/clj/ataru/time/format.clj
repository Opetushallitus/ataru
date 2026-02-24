(ns ataru.time.format
  (:import [java.time
            LocalDate
            LocalDateTime
            ZonedDateTime]
           [java.time.format DateTimeFormatter DateTimeFormatterBuilder]))

(def ^:private date-time-formatter
  (-> (DateTimeFormatterBuilder.)
      (.append DateTimeFormatter/ISO_LOCAL_DATE_TIME)
      (.optionalStart)
      (.appendOffsetId)
      (.optionalEnd)
      (.toFormatter)))

(def formatters
  {:date                     (DateTimeFormatter/ofPattern "yyyy-MM-dd")
   :date-time                date-time-formatter
   :date-time-no-ms          (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss")
   :date-hour-minute-second  (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")
   :year-month-day           (DateTimeFormatter/ofPattern "yyyy-MM-dd")
   :rfc822                   (DateTimeFormatter/ofPattern "EEE, dd MMM yyyy HH:mm:ss Z")})

(defn- resolve-formatter
  [pattern-or-key]
  (cond
    (keyword? pattern-or-key)
    (or (get formatters pattern-or-key)
        (throw (ex-info "Unknown formatter key" {:value pattern-or-key})))

    (string? pattern-or-key)
    (DateTimeFormatter/ofPattern pattern-or-key)

    (instance? DateTimeFormatter pattern-or-key)
    pattern-or-key

    :else
    (throw (ex-info "Unsupported formatter" {:value pattern-or-key}))))

(defn formatter
  ([pattern-or-key]
   (resolve-formatter pattern-or-key))
  ([pattern-or-key zone]
   (let [base (resolve-formatter pattern-or-key)]
     (if zone
       (.withZone ^DateTimeFormatter base zone)
       base))))

(defn with-zone
  [formatter-value zone]
  (.withZone ^DateTimeFormatter formatter-value zone))

(defn with-locale
  [formatter-value locale]
  (.withLocale ^DateTimeFormatter formatter-value locale))

(defn- parse-temporal
  [^DateTimeFormatter formatter-value s]
  (let [accessor (.parse formatter-value s)]
    (try
      (ZonedDateTime/from accessor)
      (catch Exception _
        (try
          (LocalDateTime/from accessor)
          (catch Exception _
            (LocalDate/from accessor)))))))

(defn parse
  [formatter-value s]
  (parse-temporal formatter-value s))

(defn parse-local-date
  [formatter-value s]
  (LocalDate/parse s formatter-value))

(defn unparse
  [formatter-value temporal]
  (.format ^DateTimeFormatter formatter-value temporal))
