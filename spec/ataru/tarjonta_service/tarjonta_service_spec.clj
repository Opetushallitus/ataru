(ns ataru.tarjonta-service.tarjonta-service-spec
  (:require [ataru.cache.cache-service :as cache]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.tarjonta-service.tarjonta-service :as service]
            [speclj.core :refer :all])
  (:import [java.time ZonedDateTime]
           [org.joda.time DateTime DateTimeZone]))

(defrecord StaticCache [values]
  cache/Cache
  (get-from [_ key]
    (get values key))
  (get-many-from [_ keys]
    (select-keys values keys))
  (remove-from [_ _])
  (clear-all [_]))

(defn- epoch-millis
  [^ZonedDateTime time]
  (.toEpochMilli (.toInstant time)))

(describe "tarjonta service hakuajat"
  (tags :unit :tarjonta)

  (it "coerces legacy Joda haku hakuajat from cache to ZonedDateTime"
    (let [start   (DateTime. 2026 5 26 8 0 0 (DateTimeZone/forID "Europe/Helsinki"))
          end     (DateTime. 2026 5 27 15 0 0 (DateTimeZone/forID "Europe/Helsinki"))
          service (service/map->CachedTarjontaService
                    {:haku-cache (->StaticCache {"haku-oid" {:oid "haku-oid"
                                                              :hakuajat [{:hakuaika-id "1"
                                                                          :start start
                                                                          :end end}]}})})
          haku    (tarjonta/get-haku service "haku-oid")
          aika    (first (:hakuajat haku))]
      (should (instance? ZonedDateTime (:start aika)))
      (should (instance? ZonedDateTime (:end aika)))
      (should= (.getMillis start) (epoch-millis (:start aika)))
      (should= (.getMillis end) (epoch-millis (:end aika)))))

  (it "coerces legacy Joda hakukohde hakuajat from cache to ZonedDateTime"
    (let [start     (DateTime. 2026 5 26 8 0 0 (DateTimeZone/forID "Europe/Helsinki"))
          end       (DateTime. 2026 5 27 15 0 0 (DateTimeZone/forID "Europe/Helsinki"))
          service   (service/map->CachedTarjontaService
                      {:hakukohde-cache (->StaticCache {"hakukohde-oid" {:oid "hakukohde-oid"
                                                                          :hakuajat [{:start start
                                                                                      :end end}]}})})
          hakukohde (tarjonta/get-hakukohde service "hakukohde-oid")
          aika      (first (:hakuajat hakukohde))]
      (should (instance? ZonedDateTime (:start aika)))
      (should (instance? ZonedDateTime (:end aika)))
      (should= (.getMillis start) (epoch-millis (:start aika)))
      (should= (.getMillis end) (epoch-millis (:end aika))))))
