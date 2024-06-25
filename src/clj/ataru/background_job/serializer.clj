(ns ataru.background-job.serializer
  (:require [cognitect.transit :as transit]
            [proletarian.protocols :as p])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)
           (java.time Instant)
           (org.joda.time DateTime)))

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

(def ^:private jodatime-writer
  (transit/write-handler
    (constantly "DateTime")
    (fn [v] (-> ^DateTime v .getMillis))
    (fn [v] (str (-> ^DateTime v .getMillis)))))

(def ^:private jodatime-reader
  (transit/read-handler
    (fn [o]
      (DateTime. o))))

(def ^:private default-write-handlers
  {Instant   instant-writer
   DateTime  jodatime-writer})

(def ^:private default-read-handlers
  {"Instant"  instant-reader
   "DateTime" jodatime-reader})

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