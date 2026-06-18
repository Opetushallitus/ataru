(ns ataru.statistics.statistics-service
  (:require [ataru.statistics.statistics-store :as store]
            [ataru.cache.cache-service :as cache]
            [ataru.time :as time]
            [ataru.time.format :as time-format]))

(def category-formatters {:month (time-format/formatter "yyyy-MM-dd")
                          :week  (time-format/formatter "yyyy-MM-dd HH:00")
                          :day   (time-format/formatter "yyyy-MM-dd HH:mm")})

(defn- inc-or-1
  [m keys]
  (if (get-in m keys)
    (update-in m keys inc)
    (assoc-in m keys 1)))

(defn- earlier-at-midnight
  [period-fn period-num date-time]
  (-> date-time
      (time/minus (period-fn period-num))
      (time/with-time-at-start-of-day)))

(defn get-and-parse-application-stats
  [time-period]
  (let [now        (time/now)
        start-time (case time-period
                     :month (earlier-at-midnight time/months 1 now)
                     :week (earlier-at-midnight time/weeks 1 now)
                     :day (earlier-at-midnight time/days 1 now))
        group-by-fn  (time-period category-formatters)
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

(defn get-application-stats
  [statistics-month-cache
   statistics-week-cache
   statistics-day-cache
   time-period]
  (case time-period
    :month (cache/get-from statistics-month-cache :month)
    :week  (cache/get-from statistics-week-cache :week)
    :day   (cache/get-from statistics-day-cache :day)))
