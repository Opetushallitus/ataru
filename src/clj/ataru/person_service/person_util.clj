(ns ataru.person-service.person-util
  (:require [ataru.date :as date]
            [ataru.util :as util]
            [clojure.string :as cs]))

(defn person-info-from-application [application]
  (let [answers (util/answers-by-key (util/get-answers-from-application application))
        birth-date (-> answers :birth-date :value)]
    (merge {:first-name     (-> answers :first-name :value)
            :preferred-name (-> answers :preferred-name :value)
            :last-name      (-> answers :last-name :value)
            :birth-date     birth-date
            :nationality    (-> answers :nationality :value)}
           (when-not (cs/blank? birth-date)
             (let [minor (date/minor? birth-date)]
               (when (boolean? minor)
                 {:minor (date/minor? birth-date)})))
           (when-not (cs/blank? (-> answers :ssn :value))
             {:ssn (-> answers :ssn :value)})
           (when-not (cs/blank? (-> answers :gender :value))
             {:gender (-> answers :gender :value)})
           (when-not (cs/blank? (-> answers :language :value))
             {:language (-> answers :language :value)}))))