(ns ataru.attachment-deadline.attachment-deadline-spec
  (:require [ataru.attachment-deadline.attachment-deadline :as attachment-deadline]
            [clj-time.coerce :as coerce]
            [clj-time.core :as t]
            [speclj.core :refer :all]))

(describe "Attachment end time"
          (tags :unit)

          (it "fails"
              (should= true false))

          (it "Returns attachment end time"
              (let [end (t/date-time 2024 02 14 9)
                    hakuajat {:attachment-modify-grace-period-days 7 :end (coerce/to-long end)}]
                (should= (t/plus end (t/days 7)) (attachment-deadline/attachment-deadline-for-hakuaika hakuajat))))

          (it "Returns attachment end time with -1 hours do to period overlap with summertime"
              (let [end (t/date-time 2024 02 14 9)
                    hakuajat {:attachment-modify-grace-period-days 60 :end (coerce/to-long end)}]
                (should= (t/minus (t/plus end (t/days 60)) (t/hours 1)) (attachment-deadline/attachment-deadline-for-hakuaika hakuajat))))

          (it "Returns attachment end time with -1 hours do to period overlap with summertime with hour 23"
              (let [end (t/date-time 2024 02 14 23)
                    hakuajat {:attachment-modify-grace-period-days 60 :end (coerce/to-long end)}]
                (should= (t/minus (t/plus end (t/days 60)) (t/hours 1)) (attachment-deadline/attachment-deadline-for-hakuaika hakuajat))))

          (it "Returns attachment end time with -1 hours do to period overlap with summertime with midnight"
              (let [end (t/date-time 2024 02 14 0)
                    hakuajat {:attachment-modify-grace-period-days 60 :end (coerce/to-long end)}]
                (should= (t/minus (t/plus end (t/days 60)) (t/hours 1)) (attachment-deadline/attachment-deadline-for-hakuaika hakuajat))))

          (it "Returns attachment end time with +1 hours do to period overlap with wintertime"
              (let [end (t/date-time 2024 10 14 9)
                    hakuajat {:attachment-modify-grace-period-days 60 :end (coerce/to-long end)}]
                (should= (t/plus (t/plus end (t/days 60)) (t/hours 1)) (attachment-deadline/attachment-deadline-for-hakuaika hakuajat))))

          (it "Returns attachment end time with +1 hours do to period overlap with wintertime with hour 23"
              (let [end (t/date-time 2024 10 14 23)
                    hakuajat {:attachment-modify-grace-period-days 60 :end (coerce/to-long end)}]
                (should= (t/plus (t/plus end (t/days 60)) (t/hours 1)) (attachment-deadline/attachment-deadline-for-hakuaika hakuajat))))

          (it "Returns attachment end time with +1 hours do to period overlap with wintertime with midnight"
              (let [end (t/date-time 2024 10 14 0)
                    hakuajat {:attachment-modify-grace-period-days 60 :end (coerce/to-long end)}]
                (should= (t/plus (t/plus end (t/days 60)) (t/hours 1)) (attachment-deadline/attachment-deadline-for-hakuaika hakuajat))))

          (it "Returns attachment end time unmodified if both wintertime and summertime overlap"
              (let [end (t/date-time 2024 10 14 9)
                    hakuajat {:attachment-modify-grace-period-days 200 :end (coerce/to-long end)}]
                (should= (t/plus end (t/days 200)) (attachment-deadline/attachment-deadline-for-hakuaika hakuajat))))

          (it "Returns attachment end time with -1 hours do to period overlap with summertime if it starts after wintertime"
              (let [end (t/date-time 2024 10 30 9)
                    hakuajat {:attachment-modify-grace-period-days 200 :end (coerce/to-long end)}]
                (should= (t/minus (t/plus end (t/days 200)) (t/hours 1)) (attachment-deadline/attachment-deadline-for-hakuaika hakuajat)))))
