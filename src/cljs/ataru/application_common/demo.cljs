(ns ataru.application-common.demo
  (:require [cljs-time.core :as time]))

(defn demo-allowed?
  [demo-validity-start demo-validity-end now]
  (and
    (some? demo-validity-start)
    (some? demo-validity-end)
    (let [first-valid-moment   (time/at-midnight demo-validity-start)
          first-invalid-moment (time/at-midnight (time/plus demo-validity-end (time/days 1)))
          valid-interval       (time/interval first-valid-moment first-invalid-moment)
          today-at-midnight    (time/at-midnight now)]
      (time/within? valid-interval today-at-midnight))))
