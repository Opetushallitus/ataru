(ns ataru.virkailija.temporal
  (:require [cljs-time.format :as f]
            [cljs-time.core :as c]
            [cljs-time.coerce :as coerce]
            [clojure.walk :refer [postwalk]]))

(def ^:private time-formatter-leading-zeros (f/formatter "dd.MM.yyyy HH:mm" "Europe/Helsinki"))

(def ^:private time-formatter (f/formatter "d.M.yyyy HH:mm" "Europe/Helsinki"))

(def ^:private date-formatter (f/formatter "dd.MM.yyyy"))

(def days-finnish
  ["Su" "Ma" "Ti" "Ke" "To" "Pe" "La"])

(defn with-dow [google-date]
  (days-finnish (.getDay (c/to-default-time-zone google-date))))

(defonce formatters (mapv f/formatters [:date-time :date-time-no-ms]))

(defn str->googdate [timestamp-value]
  (->> (for [formatter formatters]
           (try (f/parse formatter timestamp-value)
                (catch :default _
                  nil)))
       (filter some?)
       first))

(defn time->short-str [google-date]
  (->> google-date
       c/to-default-time-zone
       (f/unparse time-formatter-leading-zeros)))

(defn time->date [google-date]
  (->> google-date
       c/to-default-time-zone
       (f/unparse date-formatter)))

(defn time->str [google-date]
  (str (with-dow google-date)
       " "
       (time->short-str google-date)))

(defn time->long [google-date]
  (coerce/to-long google-date))

(defn datetime-now []
  (f/unparse (f/formatters :date-time-no-ms) (c/now)))

(defn millis->str
  [millis]
  (->> millis
       (coerce/from-long)
       (c/to-default-time-zone)
       (f/unparse-local time-formatter)))

(defn parse-times [expr]
  (let [f (fn [[k v]]
            (if-let [date (str->googdate v)]
              [k date]
              [k v]))]
    (postwalk
      (fn [x]
        (if (map? x)
          (into {} (map f x))
          x))
      expr)))
