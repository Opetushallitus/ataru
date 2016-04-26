(ns lomake-editori.temporal
  (:require [cljs-time.format :as f]
            [cljs-time.core :as c]
            [goog.date :as gd]
            [taoensso.timbre :refer-macros [spy warn]]))

(def ^:private time-formatter (f/formatter "dd.MM.yyyy HH:mm"))

(def days-finnish
  ["Sunnuntai" "Maanantai" "Tiistai" "Keskiviikko" "Torstai" "Perjantai" "Lauantai"])

(def months-finnish
  ["Tammikuu" "Helmikuu" "Maaliskuu" "Huhtikuu" "Toukokuu" "Kesäkuu" "Heinäkuu" "Elokuu" "Syyskuu" "Lokakuu" "Marraskuu" "Joulukuu"])

(defn dow->dayname [dow]
  (days-finnish dow))

(defn with-dow [google-date]
  (days-finnish (.getDay google-date)))

(def formatters (mapv f/formatters [:date-time-no-ms :date-time]))

(defn str->googdate [timestamp-value]
  {:pre [(some? timestamp-value)]}
  (first (for [formatter formatters]
           (try (f/parse formatter timestamp-value)
                (catch :default _
                  (warn "Could not parse " timestamp-value)
                  nil)))))

(defn coerce-timestamp [kw]
  (fn [element]
    (update-in element [kw] str->googdate)))

(defn- time->str [google-date]
  (->> google-date
       c/to-default-time-zone
       (f/unparse time-formatter)
       (str (with-dow google-date)
            "na ")))
