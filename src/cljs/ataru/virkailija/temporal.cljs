(ns ataru.virkailija.temporal
  (:require [cljs-time.format :as f]
            [cljs-time.core :as c]
            [cljs-time.coerce :as coerce]
            [goog.date :as gd]
            [clojure.walk :refer [postwalk]]
            [taoensso.timbre :refer-macros [spy warn]]))

(def ^:private time-formatter (f/formatter "dd.MM.yyyy HH:mm"))

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
       (f/unparse time-formatter)))

(defn time->str [google-date]
  (str (with-dow google-date)
       " "
       (time->short-str google-date)))

(defn time->long [google-date]
  (coerce/to-long google-date))

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
