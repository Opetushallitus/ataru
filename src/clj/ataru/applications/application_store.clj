(ns ataru.applications.application-store
  (:require [camel-snake-kebab.core :as t :refer [->snake_case ->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [oph.soresu.common.db :refer [exec]]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/application-queries.sql")

(defn insert-application [application]
  (first (exec :db add-application-query<!
               {:form_id (:form application)
                :key (str (java.util.UUID/randomUUID))
                :lang (:lang application)
                :content {:answers (:answers application)}
                :state "received"})))
