(ns ataru.applications.application-store
  (:require [ataru.schema.clj-schema :as schema]
            [camel-snake-kebab.core :as t :refer [->snake_case ->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [schema.core :as s]
            [oph.soresu.common.db :refer [exec]]
            [taoensso.timbre :refer [spy debug]]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/application-queries.sql")

(defonce default-application-request
  {:limit 100
   :sort :by-date
   :lang "fi"})

(defn insert-application [application]
  (first (exec :db add-application-query<!
               {:form_id (:form application)
                :key (str (java.util.UUID/randomUUID))
                :lang (:lang application)
                :content {:answers (:answers application)}
                :state "received"})))

(defn unwrap-application [application]
  (assoc (transform-keys ->kebab-case-keyword (dissoc application :content))
         :answers (-> application :content :answers)))

(s/defn retrieve-applications :- [schema/Application]
  [application-request :- schema/ApplicationRequest]
  (mapv unwrap-application
        (exec :db (case (:sort application-request)
                    :by-date application-query-by-modified
                    application-query-by-modified)
              (dissoc (transform-keys ->snake_case (merge default-application-request application-request))
                      :sort))))
