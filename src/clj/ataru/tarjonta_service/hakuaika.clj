(ns ataru.tarjonta-service.hakuaika
  (:require
    [clj-time.core :as t]
    [clj-time.coerce :as c]
    [clj-time.format :as f]
    [clojure.core.match :refer [match]]
    [clojure.string :as string]
    [clojure.set :as set]
    [ataru.constants :refer [hakutapa-jatkuva-haku hakutapa-joustava-haku]])
  (:import (java.util Locale)
           (java.time Instant ZoneId)
           (java.time.format DateTimeFormatter)))

(def ^:private time-formatter (f/formatter "d.M.yyyy HH:mm" (t/time-zone-for-id "Europe/Helsinki")))
(def ^:private basic-date-time-formatter (f/formatter (:date-hour-minute-second f/formatters) (t/time-zone-for-id "Europe/Helsinki")))

(defn get-formatter [fmt-str locale]
  (-> (DateTimeFormatter/ofPattern fmt-str)
      (.withZone (ZoneId/of "Europe/Helsinki"))
      (.withLocale locale)))

(defn get-formatted-date [formatter clj-datetime]
  (-> (.format formatter (Instant/ofEpochMilli (c/to-long clj-datetime)))
      (string/replace " GMT+" " UTC+")                      ; speksin mukaan halutaan UTC, ja ero GMT:n ja UTC:n
                                                            ; välillä pitäisi olla <1s
      (string/replace " am " " AM ")                        ; jostain syystä am pm tulee pienellä
      (string/replace " pm " " PM ")))                      ; vaikka javadocit sanoo muuta

(def finnish-formatter (get-formatter "d.M.yyyy 'klo' HH:mm" (Locale. "fi" "FI")))
(def swedish-formatter (get-formatter "d.M.yyyy 'kl.' HH:mm O" (Locale. "se" "FI")))
(def english-formatter (get-formatter "MMM. d, yyyy 'at' hh:mm a O" (Locale. "en" "US")))

(defn date-timez->localized-date-time [datetime lang]
  (match (keyword lang)
         :fi (get-formatted-date finnish-formatter datetime)
         :sv (get-formatted-date swedish-formatter datetime)
         :else (get-formatted-date english-formatter datetime)))

(def finnish-time-formatter (get-formatter "'klo' HH:mm" (Locale. "fi" "FI")))
(def swedish-time-formatter (get-formatter "'kl.' HH:mm O" (Locale. "se" "FI")))
(def english-time-formatter (get-formatter "'at' hh:mm a O" (Locale. "en" "US")))

(defn date-timez->localized-time [datetime lang]
  (match (keyword lang)
         :fi (get-formatted-date finnish-time-formatter datetime)
         :sv (get-formatted-date swedish-time-formatter datetime)
         :else (get-formatted-date english-time-formatter datetime)))

(defn str->date-time
  [str]
  (f/parse time-formatter str))

(defn basic-date-time-str->date-time
  [str]
  (f/parse basic-date-time-formatter str))

(defn- jatkuva-haku? [haku]
  (string/starts-with? (:hakutapa-uri haku) hakutapa-jatkuva-haku))

(defn- joustava-haku? [haku]
  (string/starts-with? (:hakutapa-uri haku) hakutapa-joustava-haku))

(defn- jatkuva-or-joustava-haku? [haku]
  (or
    (joustava-haku? haku)
    (jatkuva-haku? haku)))

(defn ended?
  [now end]
  (and (some? end)
       (not (t/before? now (c/from-long end)))))

(defn started?
  [now start]
  (and (some? start)
       (t/after? now (c/from-long start))))

(defn hakuaika-on [now start end]
  (and (started? now start)
       (not (ended? now end))))

(defn date-time->localized-date-time [date-time]
  (->> [:fi :sv :en]
       (map (fn [lang] [lang (date-timez->localized-date-time date-time lang)]))
       (into {})))

(defn date-time->localized-time [date-time]
  (->> [:fi :sv :en]
       (map (fn [lang] [lang (date-timez->localized-time date-time lang)]))
       (into {})))

(defn millis->localized-date-time [millis]
  (date-time->localized-date-time (c/from-long millis)))

(defn millis->localized-time [millis]
  (date-time->localized-time (c/from-long millis)))

(defn- last-by-ending
  [hakuajat]
  (first
   (sort-by :end
            #(or (nil? %1)
                 (and (some? %2)
                      (> %1 %2)))
            hakuajat)))

(defn first-by-start
  [hakuajat]
  (first
   (sort-by :start
            #(or (nil? %1)
                 (and (some? %2)
                      (< %1 %2)))
            hakuajat)))

(defn index-hakuajat
  [hakukohteet]
  (reduce (fn [{:keys [uniques by-oid]} {:keys [oid hakuaika hakukohderyhmat]}]
            {:uniques (conj uniques hakuaika)
             :by-oid  (reduce #(merge-with set/union %1 {%2 #{hakuaika}})
                              by-oid
                              (cons oid hakukohderyhmat))})
          {:uniques #{}
           :by-oid  {}}
          hakukohteet))

(defn- select-hakuaika [now hakuajat]
  (or (last-by-ending (filter :on hakuajat))
      (last-by-ending (filter #(ended? now (:end %)) hakuajat))
      (first-by-start (filter #(not (started? now (:start %))) hakuajat))
      (last-by-ending hakuajat)))

(defn select-hakuaika-for-field [now field {:keys [uniques by-oid]}]
  (select-hakuaika
   now
   (if-let [field-hakukohde-and-group-oids (seq (concat (:belongs-to-hakukohteet field)
                                                        (:belongs-to-hakukohderyhma field)))]
     (reduce #(set/union %1 (by-oid %2))
             #{}
             field-hakukohde-and-group-oids)
     uniques)))

(defn hakuaika-with-label
  ([{:keys [start end] :as hakuaika}]
   (assoc hakuaika :label {:start    (millis->localized-date-time start)
                           :end      (millis->localized-date-time end)
                           :end-time (millis->localized-time end)})))

(defn- hakukohteen-hakuajat [haku hakuaika-id]
  (filter #(= hakuaika-id (:hakuaika-id %))
          (:hakuajat haku)))

(defn hakukohteen-hakuaika
  [now haku ohjausparametrit hakukohde]
  (let [hakuaika (->> (if-let [hakuaika-id (:hakuaika-id hakukohde)]
                        (hakukohteen-hakuajat haku hakuaika-id)
                        (:hakuajat hakukohde))
                      (map (fn [hakuaika]
                             (let [start (.getMillis (:start hakuaika))
                                   end   (some-> (:end hakuaika) (.getMillis))]
                               {:start start
                                :end   end
                                :on    (hakuaika-on now start end)})))
                      (select-hakuaika now))]
    (hakuaika-with-label {:start                               (:start hakuaika)
                          :end                                 (:end hakuaika)
                          :on                                  (:on hakuaika)
                          :attachment-modify-grace-period-days (-> ohjausparametrit :PH_LMT :value)
                          :jatkuva-haku?                       (jatkuva-haku? haku)
                          :joustava-haku?                      (joustava-haku? haku)
                          :jatkuva-or-joustava-haku?           (jatkuva-or-joustava-haku? haku)
                          :hakukierros-end                     (-> ohjausparametrit :PH_HKP :date)})))

(defn haun-hakuaika
  [now haku ohjausparametrit]
  (when-let [hakuaika (->> (:hakuajat haku)
                           (map (fn [hakuaika]
                                  (let [start (.getMillis (:start hakuaika))
                                        end   (some-> (:end hakuaika)
                                                      (.getMillis))]
                                    {:start start
                                     :end   end
                                     :on    (hakuaika-on now start end)})))
                           (select-hakuaika now))]
    (hakuaika-with-label
     {:start                               (:start hakuaika)
      :end                                 (:end hakuaika)
      :on                                  (:on hakuaika)
      :attachment-modify-grace-period-days (-> ohjausparametrit :PH_LMT :value)
      :jatkuva-haku?                       (jatkuva-haku? haku)
      :joustava-haku?                      (joustava-haku? haku)
      :jatkuva-or-joustava-haku?           (jatkuva-or-joustava-haku? haku)
      :hakukierros-end                     (-> ohjausparametrit :PH_HKP :date)})))
