(ns ataru.tarjonta-service.hakuaika
  (:require
    [clj-time.core :as t]
    [clj-time.coerce :as c]
    [clj-time.format :as f]
    [clojure.core.match :refer [match]]
    [clojure.math :refer [signum]]
    [ataru.config.core :refer [config]]
    [clojure.string :as string]
    [clojure.set :as set]
    [ataru.constants :refer [hakutapa-jatkuva-haku hakutapa-joustava-haku]]))

(def ^:private time-formatter (f/formatter "d.M.yyyy HH:mm" (t/time-zone-for-id "Europe/Helsinki")))
(def ^:private basic-date-time-formatter (f/formatter (:date-hour-minute-second f/formatters) (t/time-zone-for-id "Europe/Helsinki")))
(defn new-formatter [fmt-str]
  (f/formatter fmt-str (t/time-zone-for-id "Europe/Helsinki")))

(def finnish-format (new-formatter "d.M.yyyy 'klo' HH:mm"))
(def swedish-format (new-formatter "d.M.yyyy 'kl.' HH:mm z"))
(def english-format (new-formatter "MMM. d, yyyy 'at' hh:mm a z"))

(defn date-timez->localized-date-time [datetime lang]
  (match (keyword lang)
    :fi (f/unparse finnish-format datetime)
    :sv (f/unparse swedish-format datetime)
    :else (f/unparse english-format datetime)))

(def finnish-time-format (new-formatter "'klo' HH:mm"))
(def swedish-time-format (new-formatter "'kl.' HH:mm z"))
(def english-time-format (new-formatter "'at' hh:mm a z"))

(defn date-timez->localized-time [datetime lang]
  (match (keyword lang)
         :fi (f/unparse finnish-time-format datetime)
         :sv (f/unparse swedish-time-format datetime)
         :else (f/unparse english-time-format datetime)))

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

; TODO: poista tämä häkki kun ihmiskunta on tullut järkiinsä ja tuhonnut kesäajan
(defn- winter-summertime-nullification-adjustment
  [start end]
  (let [start-hour (->> start
                            (f/unparse (new-formatter "HH"))
                            Integer/parseInt)
        end-hour (->> end
                          (f/unparse (new-formatter "HH"))
                          Integer/parseInt)
        day       (t/day end)
        formatted-day (->> end
                           (f/unparse (new-formatter "d"))
                           Integer/parseInt)
        modifier (signum (- start-hour end-hour))]
    (cond
      (== start-hour end-hour)
      0
      (> day formatted-day)
      (* -1 modifier)
      (< day formatted-day)
      (* 1 modifier)
      :else
      modifier)))

(defn attachment-edit-end [hakuaika]
  (let [default-modify-grace-period (-> config
                                        :public-config
                                        (get :attachment-modify-grace-period-days 14))
        modify-grace-period (or (:attachment-modify-grace-period-days hakuaika) default-modify-grace-period)
        hakuaika-end (some-> hakuaika
                             :end
                             c/from-long)
        attachment-end (some-> hakuaika-end
                               (t/plus (t/days modify-grace-period)))]
   (when attachment-end
     (t/plus attachment-end (t/hours (winter-summertime-nullification-adjustment hakuaika-end attachment-end))))))

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
