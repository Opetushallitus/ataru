(ns ataru.time.coerce-spec
  (:require [ataru.time :as time]
            [ataru.time.coerce :as c]
            [speclj.core :refer :all]))

(describe "from-long"
  (tags :unit)

  (around [it]
    (try
      (time/set-fixed-now! (java.time.Instant/parse "2026-06-30T09:00:00Z"))
      (it)
      (finally
        (time/reset-now!))))

  (it "returns current time for nil like Joda's from-long did"
    (should= (time/now) (c/from-long nil)))

  (it "returns zoned date-time for millis"
    (should= 1234567890000 (c/to-long (c/from-long 1234567890000)))))
