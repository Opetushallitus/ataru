(ns ataru.tarjonta-service.hakuaika
  (:require
   [clj-time.core :as time]
   [clj-time.coerce :as time-coerce]))

(defn- jatkuva-haku? [haku]
  (clojure.string/starts-with? (:hakutapaUri haku) "hakutapa_03#"))

(defn- hakuaika-on [start end]
  (let [now (time/now)]
    (and (time/after? now (time-coerce/from-long start))
         (or (nil? end)
             (time/before? now (time-coerce/from-long end))))))

(defn- hakukohteen-hakuaika [haku hakukohde]
  (some #(when (= (:hakuaikaId hakukohde)
                  (:hakuaikaId %))
           %)
        (:hakuaikas haku)))

(defn get-hakuaika-info [haku ohjausparametrit hakukohde]
  (let [[start end] (if (:kaytetaanHakukohdekohtaistaHakuaikaa hakukohde)
                      [(:hakuaikaAlkuPvm hakukohde)
                       (:hakuaikaLoppuPvm hakukohde)]
                      (let [hakuaika (hakukohteen-hakuaika haku hakukohde)]
                        [(:alkuPvm hakuaika)
                         (:loppuPvm hakuaika)]))]
    {:start                               start
     :end                                 end
     :on                                  (hakuaika-on start end)
     :attachment-modify-grace-period-days (-> ohjausparametrit :PH_LMT :value)
     :jatkuva-haku?                       (jatkuva-haku? haku)
     :hakukierros-end                     (-> ohjausparametrit :PH_HKP :date)}))
