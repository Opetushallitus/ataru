(ns ataru.tarjonta-service.hakuaika
  (:require
   [clj-time.core :as t]
   [clj-time.coerce :as c]
   [clj-time.format :as f]
   [clojure.core.match :refer [match]]
   [ataru.config.core :refer [config]]))

(def ^:private time-formatter (f/formatter "d.M.yyyy HH:mm" (t/time-zone-for-id "Europe/Helsinki")))
(defn new-formatter [fmt-str]
  (f/formatter fmt-str (t/time-zone-for-id "Europe/Helsinki")))

(def finnish-format (new-formatter "d.M.yyyy 'klo' HH:mm"))
(def swedish-format (new-formatter "d.M.yyyy 'kl' HH:mm z"))
(def english-format (new-formatter "MMM. d, yyyy 'at' hh:mm a z"))

(defn date-timez->localized-date-time [datetime lang]
  (match (keyword lang)
    :fi (f/unparse finnish-format datetime)
    :sv (f/unparse swedish-format datetime)
    :else (f/unparse english-format datetime)))

(defn str->date-time
  [str]
  (f/parse time-formatter str))

(defn- jatkuva-haku? [haku]
  (clojure.string/starts-with? (:hakutapaUri haku) "hakutapa_03#"))

(defn- hakuaika-on [start end]
  (let [now (t/now)]
    (and (t/after? now (c/from-long start))
         (or (nil? end)
             (t/before? now (c/from-long end))))))

(defn- hakukohteen-hakuaika [haku hakukohde]
  (some #(when (= (:hakuaikaId hakukohde)
                  (:hakuaikaId %))
           %)
        (:hakuaikas haku)))

(defn date-time->localized-date-time [date-time]
  (->> [:fi :sv :en]
       (map (fn [lang] [lang (date-timez->localized-date-time date-time lang)]))
       (into {})))

(defn millis->localized-date-time [millis]
  (date-time->localized-date-time (c/from-long millis)))


(defn attachment-edit-end [end hakuaika-period default-modify-grace-period]
  (let [hakuaika-end (some-> end c/from-long)]
    (some-> hakuaika-end (t/plus (t/days (or hakuaika-period
                                             default-modify-grace-period))))))

(defn hakuaika-with-label
  ([{:keys [start end] :as hakuaika} ]
  (assoc hakuaika :label {:start                 (millis->localized-date-time start)
                          :end                   (millis->localized-date-time end)})))

(defn get-hakuaika-info [haku ohjausparametrit hakukohde]
  (let [[start end] (if (:kaytetaanHakukohdekohtaistaHakuaikaa hakukohde)
                      [(:hakuaikaAlkuPvm hakukohde)
                       (:hakuaikaLoppuPvm hakukohde)]
                      (let [hakuaika (hakukohteen-hakuaika haku hakukohde)]
                        [(:alkuPvm hakuaika)
                         (:loppuPvm hakuaika)]))]
    (hakuaika-with-label {:start                               start
                          :end                                 end
                          :on                                  (hakuaika-on start end)
                          :attachment-modify-grace-period-days (-> ohjausparametrit :PH_LMT :value)
                          :jatkuva-haku?                       (jatkuva-haku? haku)
                          :hakukierros-end                     (-> ohjausparametrit :PH_HKP :date)})))
