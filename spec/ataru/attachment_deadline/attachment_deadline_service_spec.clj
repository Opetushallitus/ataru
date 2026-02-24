(ns ataru.attachment-deadline.attachment-deadline-service-spec
  (:require [ataru.attachment-deadline.attachment-deadline-protocol :as attachment-deadline]
            [ataru.attachment-deadline.attachment-deadline-service :as attachment-deadline-service]
            [ataru.ohjausparametrit.mock-ohjausparametrit-service :refer [->MockOhjausparametritService]]
            [ataru.time.coerce :as coerce]
            [ataru.time :as t]
            [speclj.core :refer :all]))

(def attachment-deadline-service (attachment-deadline-service/->AttachmentDeadlineService (->MockOhjausparametritService)))

(defn- in-helsinki-date-time
  [year month day hour]
  (-> (t/date-time year month day)
      (t/to-time-zone (t/time-zone-for-id "Europe/Helsinki"))
      (t/with-time (t/local-time hour 0))))

(describe "Attachment end time"
          (tags :unit)

          (it "Returns attachment end time"
              (let [end (in-helsinki-date-time 2024 2 14 9)
                    hakuajat {:attachment-modify-grace-period-days 7 :end (coerce/to-long end)}]
                (should= (in-helsinki-date-time 2024 2 21 9)
                         (attachment-deadline/attachment-deadline-for-hakuaika attachment-deadline-service nil nil hakuajat))))

          (it "Returns attachment end time within same day with summertime"
              (let [end (in-helsinki-date-time 2024 2 14 9)
                    hakuajat {:attachment-modify-grace-period-days 60 :end (coerce/to-long end)}]
                (should= (in-helsinki-date-time 2024 4 14 9)
                         (attachment-deadline/attachment-deadline-for-hakuaika attachment-deadline-service nil nil hakuajat))))

          (it "Returns attachment end time within same day with summertime at 1:00"
              (let [end (in-helsinki-date-time 2024 2 15 1)
                    hakuajat {:attachment-modify-grace-period-days 60 :end (coerce/to-long end)}]
                (should= (in-helsinki-date-time 2024 4 15 1)
                         (attachment-deadline/attachment-deadline-for-hakuaika attachment-deadline-service nil nil hakuajat))))

          (it "Returns attachment end time within same day with summertime at midnight"
              (let [end (in-helsinki-date-time 2024 2 14 0)
                    hakuajat {:attachment-modify-grace-period-days 60 :end (coerce/to-long end)}]
                (should= (in-helsinki-date-time 2024 4 14 0)
                         (attachment-deadline/attachment-deadline-for-hakuaika attachment-deadline-service nil nil hakuajat))))

          (it "Returns attachment end time within same day with wintertime"
              (let [end (in-helsinki-date-time 2024 10 14 9)
                    hakuajat {:attachment-modify-grace-period-days 60 :end (coerce/to-long end)}]
                (should= (in-helsinki-date-time 2024 12 13 9)
                         (attachment-deadline/attachment-deadline-for-hakuaika attachment-deadline-service nil nil hakuajat))))

          (it "Returns attachment end time within same day wintertime at 1:00"
              (let [end (in-helsinki-date-time 2024 10 14 23)
                    hakuajat {:attachment-modify-grace-period-days 60 :end (coerce/to-long end)}]
                (should= (in-helsinki-date-time 2024 12 13 23)
                         (attachment-deadline/attachment-deadline-for-hakuaika attachment-deadline-service nil nil hakuajat))))

          (it "Returns attachment end time within same day with wintertime at midnight"
              (let [end (in-helsinki-date-time 2024 10 14 0)
                    hakuajat {:attachment-modify-grace-period-days 60 :end (coerce/to-long end)}]
                (should= (in-helsinki-date-time 2024 12 13 0)
                         (attachment-deadline/attachment-deadline-for-hakuaika attachment-deadline-service nil nil hakuajat))))

          (it "Returns attachment end time unmodified if both wintertime and summertime overlap"
              (let [end (in-helsinki-date-time 2024 10 14 9)
                    hakuajat {:attachment-modify-grace-period-days 200 :end (coerce/to-long end)}]
                (should= (in-helsinki-date-time 2025 5 2 9)
                         (attachment-deadline/attachment-deadline-for-hakuaika attachment-deadline-service nil nil hakuajat))))

          (it "Returns attachment end time with -1 hours do to period overlap with summertime if it starts after wintertime"
              (let [end (in-helsinki-date-time 2024 10 30 9)
                    hakuajat {:attachment-modify-grace-period-days 200 :end (coerce/to-long end)}]
                (should= (in-helsinki-date-time 2025 5 18 9)
                         (attachment-deadline/attachment-deadline-for-hakuaika attachment-deadline-service nil nil hakuajat))))

          (it "Returns attachment end time calculated from application submit time"
              (let [application-submitted (t/now)]
                (should= (-> application-submitted
                             (t/to-time-zone (t/time-zone-for-id "Europe/Helsinki"))
                             (t/plus (t/days 15))
                             (t/with-time (t/local-time 16 15)))
                         (attachment-deadline/attachment-deadline-for-hakuaika
                           attachment-deadline-service application-submitted {:oid "hakemuskohtainen-raja-käytössä"} nil))))

          (it "Returns attachment end time calculated from hakuaika end time"
              (let [end (t/now)
                    hakuajat {:end (coerce/to-long end)}]
                (should= (-> end
                             (t/to-time-zone (t/time-zone-for-id "Europe/Helsinki"))
                             (t/plus (t/days 20))
                             (t/with-time (t/local-time 15 14)))
                         (attachment-deadline/attachment-deadline-for-hakuaika
                           attachment-deadline-service nil {:oid "hakukohtainen-raja-käytössä"} hakuajat)))))
