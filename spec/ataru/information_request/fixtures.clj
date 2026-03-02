(ns ataru.information-request.fixtures
  (:require [ataru.time :as c]))

(defn information-requests-to-remind [application-key]
  [{:subject         "muistutus1"
    :recipient-target "hakija"
    :message         "Täydennyspyyntö viesti"
    :application-key application-key
    :send-reminder-time (c/minus (c/today-at 6 0) (c/days 1))
    :message-type "information-request"}
   {:subject         "muistutus2"
    :recipient-target "hakija"
    :message         "Täydennyspyyntö viesti"
    :application-key application-key
    :send-reminder-time (c/minus (c/today-at 6 0) (c/days 3))
    :message-type "information-request"}
   {:subject         "muistutus3"
    :recipient-target "hakija"
    :message         "Täydennyspyyntö viesti"
    :application-key application-key
    :send-reminder-time (c/minus (c/today-at 6 0) (c/days 2))
    :message-type "information-request"}
   {:subject         "muistutus4"
    :recipient-target "hakija"
    :message         "Täydennyspyyntö viesti"
    :application-key application-key
    :send-reminder-time (c/minus (c/today-at 6 0) (c/days 13))
    :message-type "information-request"}
   {:subject         "ei muistutusta"
    :recipient-target "hakija"
    :message         "Täydennyspyyntö viesti"
    :application-key application-key
    :send-reminder-time (c/plus (c/today-at 6 0) (c/days 1))
    :message-type "information-request"}])