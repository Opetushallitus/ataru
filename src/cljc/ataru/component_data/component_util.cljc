(ns ataru.component-data.component-util
  (:require
   [ataru.component-data.base-education-module :as bem :refer [base-education-questions]]
   [ataru.component-data.component :refer [lupatiedot-questions]]
   [ataru.component-data.higher-education-base-education-module :as hebem :refer [higher-education-base-education-questions]]
   [ataru.component-data.person-info-module :as pim :refer [person-info-questions]]))

(def answer-to-always-include?
  (clojure.set/union
    #{"hakukohteet"}
    higher-education-base-education-questions
    base-education-questions
    person-info-questions
    lupatiedot-questions))