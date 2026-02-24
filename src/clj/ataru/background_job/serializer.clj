(ns ataru.background-job.serializer
  (:require [cognitect.transit :as transit]
            [proletarian.protocols :as p])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)
           (java.time Instant ZoneId ZonedDateTime)))

(set! *warn-on-reflection* true)

(def ^:private instant-writer
  (transit/write-handler
    (constantly "Instant")
    (fn [v] (-> ^Instant v .toEpochMilli))
    (fn [v] (str (-> ^Instant v .toEpochMilli)))))

(def ^:private instant-reader
  (transit/read-handler
    (fn [o]
      (Instant/ofEpochMilli o))))

(defn- zoned-date-time-representation
  [^ZonedDateTime v]
  {:epoch-millis (-> v .toInstant .toEpochMilli)
   :zone-id      (.getId (.getZone v))})

(defn- parse-zoned-date-time-representation
  [o]
  (cond
    (number? o)
    {:epoch-millis (long o)}

    (vector? o)
    {:epoch-millis (long (nth o 0))
     :zone-id      (nth o 1 nil)}

    (map? o)
    {:epoch-millis (or (:epoch-millis o)
                       (:epochMillis o)
                       (get o "epoch-millis")
                       (get o "epochMillis"))
     :zone-id      (or (:zone-id o)
                       (:zoneId o)
                       (get o "zone-id")
                       (get o "zoneId"))}

    :else
    (throw (ex-info "Unsupported ZonedDateTime representation" {:value o}))))

(defn- ->zoned-date-time
  [{:keys [epoch-millis zone-id] :as rep}]
  (when-not (some? epoch-millis)
    (throw (ex-info "Missing epoch-millis in ZonedDateTime representation" {:value rep})))
  (let [resolved-zone (if (some? zone-id)
                        (ZoneId/of zone-id)
                        (ZoneId/systemDefault))]
    (ZonedDateTime/ofInstant (Instant/ofEpochMilli (long epoch-millis)) resolved-zone)))

(def ^:private zoned-date-time-writer
  (transit/write-handler
    (constantly "ZonedDateTime")
    (fn [v] (zoned-date-time-representation v))
    (fn [v]
      (let [{:keys [epoch-millis zone-id]} (zoned-date-time-representation v)]
        (str epoch-millis "@" zone-id)))))

(def ^:private zoned-date-time-reader
  (transit/read-handler
    (fn [o]
      (-> o
          parse-zoned-date-time-representation
          ->zoned-date-time))))

(def ^:private default-write-handlers
  {Instant       instant-writer
   ZonedDateTime zoned-date-time-writer})

(def ^:private default-read-handlers
  {"Instant"       instant-reader
   "ZonedDateTime" zoned-date-time-reader
   "DateTime"      zoned-date-time-reader})

(defn ^:private encode
  ([data]
   (encode data {}))
  ([data write-handlers]
   (let [out (ByteArrayOutputStream. 4096)
         writer (transit/writer out :json {:handlers (merge default-write-handlers write-handlers)})]
     (transit/write writer data)
     (.toString out "UTF-8"))))

(defn ^:private decode
  ([^String s]
   (decode s {}))
  ([^String s read-handlers]
   (let [in (-> s
                (.getBytes "UTF-8")
                (ByteArrayInputStream.))
         reader (transit/reader in :json {:handlers (merge default-read-handlers read-handlers)})]
     (transit/read reader))))

(defn create-serializer
  []
  (reify p/Serializer
    (encode [_ data] (encode data))
    (decode [_ data-string] (decode data-string))))
