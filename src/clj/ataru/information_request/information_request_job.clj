(ns ataru.information-request.information-request-job
  (:require [ataru.background-job.email-job :as email-job]
            [clojure.java.jdbc :as jdbc]
            [ataru.db.db :as db]
            [ataru.information-request.information-request-store :as information-request-store]))

(defn- send-email-step [state runner]
  (let [result (email-job/send-email-step state runner)]
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
      (information-request-store/set-information-request-state (:id state)
                                                               "processed"
                                                               conn))
    result))

(def job-definition {:steps {:initial send-email-step}
                     :type  (-> *ns* ns-name str)})
