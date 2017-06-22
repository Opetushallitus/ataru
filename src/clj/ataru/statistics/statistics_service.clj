(ns ataru.statistics.statistics-service
  (:require [ataru.statistics.statistics-store :as store]
            [clj-time.core :as time]
            [clj-time.format :as time-format]))

(def category-formatters {:month (time-format/formatter "yyyy-MM-dd")
                          :week  (time-format/formatter "yyyy-MM-dd-HH")
                          :day   (time-format/formatter "yyyy-MM-dd-HH-mm")})

(defn- get-and-parse-application-stats
  [start-time time-period]
  (let [group-by-fn                     (time-period category-formatters)
        applications                    (store/get-application-stats start-time)]
    (reduce
      (fn [acc application]
        (let [form-id (-> application
                          :form-id
                          (str)
                          (keyword))
              category (time-format/unparse group-by-fn (:created-time application))]
          (if-let [category-map (form-id acc)]
            (if (get category-map category)
              (update-in acc [form-id category] inc)
              (assoc-in acc [form-id category] 1))
            (assoc-in acc [form-id category] 1))))
      {}
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
    (.cache-get-or-fetch cache-service cache-key entry-key #(get-and-parse-application-stats start-time time-period))))
