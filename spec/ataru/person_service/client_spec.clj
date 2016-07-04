(ns ataru.person-service.client-spec
  (:require [aleph.http :as http]
            [ataru.person-service.client :as client]
            [clj-util.cas :as cas-util]
            [clj-util.client :as client-util]
            [com.stuartsierra.component :as component]
            [manifold.deferred :as d]
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

(def person-service-url (get-in config [:person-service :url]))

(def username "test-user")

(defmacro with-mock-api
  [bindings & body]
  `(let [system# (component/start-system
                   (component/system-map
                     :cas-client {:client mock-client
                                  :params cas-params}
                     :person-service (component/using
                                       (client/new-client)
                                       [:cas-client])))
         ~(first bindings) (:person-service system#)]
     (with-redefs [http/get (fn [& args#]
                              (apply ~(second bindings) args#)
                              (let [ret# (d/deferred)]
                                (d/success! ret# person-search-response)
                                ret#))]
       ~@body)))

(describe "PersonServiceClient"
  (tags :unit)

  (it "should fetch OIDs for a person"
    (with-mock-api [client (fn [url {:keys [query-params headers]}]
                             (should= (str person-service-url "/authentication-service/resources/henkilo") url)
                             (should= {"q" username} query-params)
                             (should= {"Cookie" (str "JSESSIONID=" cas-session-id)} headers))]
      (let [oid-resp (.resolve-person-oids client "test-user")]
        (should= person-search-response oid-resp)))))
