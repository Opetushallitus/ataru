(ns ataru.person-service.client-spec
  (:require [ataru.person-service.client :as client]
            [clj-util.cas :as cas-util]
            [clj-util.client :as client-util]
            [com.stuartsierra.component :as component]
            [oph.soresu.common.config :refer [config]]
            [speclj.core :refer :all])
  (:import (fi.vm.sade.utils.cas CasClient CasParams)
           (scalaz.concurrent Task)))

(def person-search-response
  {:totalCount 2
   :results [{:oidHenkilo "1.2.246.562.24.00000000001"}
             {:oidHenkilo "1.2.246.562.24.00000000002"}]})

(def cas-params (cas-util/cas-params
                  (get-in config [:person-service :url])
                  (get-in config [:cas :username])
                  (get-in config [:cas :password])))

(def cas-url (get-in config [:authentication :cas-client-url]))

(def cas-session-id "173219B0D0FDB30124AD4D4696BC1336")

(def mock-client (proxy [CasClient] [cas-url client-util/client]
                   (fetchCasSession [_]
                     (Task/now cas-session-id))))

(defmacro with-client
  [client & body]
  `(let [system# (component/start-system
                   (component/system-map
                     :cas-client {:client mock-client
                                  :params cas-params}
                     :person-service (component/using
                                       (client/new-client)
                                       [:cas-client])))
         ~client (:person-service system#)]
     ~@body))

(describe "PersonServiceClient"
  (tags :unit)

  (it "should fetch OIDs for a person"
    (with-client client
      (let [oid-resp (.resolve-person-oids client "test-user")]
        (should= cas-session-id oid-resp)))))
