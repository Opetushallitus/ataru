(ns ataru.statistics.statistics-service
  (:require [ataru.statistics.statistics-store :as store]
            [ataru.cache.cache-service :as cache]
            [clj-time.core :as time]
            [clj-time.format :as time-format]))

(def category-formatters {:month (time-format/formatter "yyyy-MM-dd")
                          :week  (time-format/formatter "yyyy-MM-dd HH:00")
                          :day   (time-format/formatter "yyyy-MM-dd HH:mm")})

(defn- inc-or-1
  [m keys]
  (if (get-in m keys)
    (update-in m keys inc)
    (assoc-in m keys 1)))

(defn- get-and-parse-application-stats
  [start-time time-period]
  (let [group-by-fn  (time-period category-formatters)
        applications (store/get-application-stats start-time)]
    (reduce
      (fn [acc application]
        (let [form-key       (-> application
                                 :key
                                 (keyword))
              category       (time-format/unparse group-by-fn (:created-time application))
              acc-with-total (inc-or-1 acc [:TOTAL :counts category])]
          (if (form-key acc-with-total)
            (inc-or-1 acc-with-total [form-key :counts category])
            (assoc acc-with-total form-key {:form-name (:form-name application)
                                            :counts    {category 1}}))))
      {:TOTAL {:form-name "Yhteensä" :counts {}}}
      applications)))

(defn- earlier-at-midnight
  [period-fn period-num date-time]
  (-> date-time
      (time/minus (period-fn period-num))
      (time/with-time-at-start-of-day)))

(defn get-application-stats
  [cache-service time-period]
  (let [cache-key  (keyword (str "statistics-" (name time-period)))
        entry-key  :applications
        now        (time/now)
        start-time (case time-period
                     :month (earlier-at-midnight time/months 1 now)
                     :week (earlier-at-midnight time/weeks 1 now)
                     :day (earlier-at-midnight time/days 1 now))]
    (cache/cache-get-or-fetch cache-service cache-key entry-key #(get-and-parse-application-stats start-time time-period))))
