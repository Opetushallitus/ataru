(ns ataru.applications.application-util
  (:require
    [ataru.person-service.person-service :as person-service]
  )
)


(defn enrich-persons-from-onr [person-service applications]
  (let [persons (person-service/get-persons person-service (map #(get % :person-oid) applications))]
    (map #(let [person        (get persons (get % :person-oid))
                parsed-person (person-service/parse-person % person)]
            (assoc %
                   :sukunimi      (get parsed-person :last-name)
                   :etunimet      (get parsed-person :first-name)
                   :henkilotunnus (get parsed-person :ssn)))
         applications)))
