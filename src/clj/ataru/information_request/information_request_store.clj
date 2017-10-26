(ns ataru.information-request.information-request-store
  (:require [camel-snake-kebab.core :as c]
            [camel-snake-kebab.extras :as t]
            [yesql.core :as sql]))

(sql/defqueries "sql/information-request-queries.sql")

(def ^:private ->kebab-case-kw (partial t/transform-keys c/->kebab-case-keyword))
(def ^:private ->snake-case-kw (partial t/transform-keys c/->snake_case_keyword))

(defn  add-information-request [information-request conn]
  (-> (yesql-add-information-request<! (->snake-case-kw information-request)
                                       {:connection conn})
      (->kebab-case-kw)))
