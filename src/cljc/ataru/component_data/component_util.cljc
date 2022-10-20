(ns ataru.component-data.component-util
  (:require
   [clojure.set :refer [union]]
   [ataru.component-data.base-education-module :refer [base-education-questions]]
   [ataru.component-data.component :refer [lupatiedot-kk-questions]]
   [ataru.component-data.base-education-module-higher :refer [higher-education-base-education-questions]]
   [ataru.component-data.person-info-module :refer [person-info-questions]]))

(def answer-to-always-include?
  (union
    #{"hakukohteet"}
    higher-education-base-education-questions
    base-education-questions
    person-info-questions
    lupatiedot-kk-questions))