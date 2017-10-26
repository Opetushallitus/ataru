(ns ataru.information-request.information-request-store
  (:require [camel-snake-kebab.core :as c]
            [camel-snake-kebab.extras :as t]
            [yesql.core :as sql]
            [ataru.db.db :as db]))

(sql/defqueries "sql/information-request-queries.sql")

(def ^:private ->kebab-case-kw (partial t/transform-keys c/->kebab-case-keyword))
(def ^:private ->snake-case-kw (partial t/transform-keys c/->snake_case_keyword))

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn add-information-request [information-request conn]
  (-> (yesql-add-information-request<! (->snake-case-kw information-request)
                                       {:connection conn})
      (->kebab-case-kw)))

(defn get-information-requests [application-key]
  (->> (exec-db :db yesql-get-information-requests {:application_key application-key})
       (map ->kebab-case-kw)))
