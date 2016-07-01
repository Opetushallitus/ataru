(ns ataru.person-service.client-spec
  (:require [ataru.cas.client :as cas]
            [ataru.person-service.client :as client]
            [com.stuartsierra.component :as component]
            [speclj.core :refer :all]))

(def person-search-response
  {:totalCount 2
   :results [{:oidHenkilo "1.2.246.562.24.00000000001"}
             {:oidHenkilo "1.2.246.562.24.00000000002"}]})

(defmacro with-client
  [client & body]
  `(let [system# (component/start-system
                   (component/system-map
                     :cas-client (cas/new-client)
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
        (should= person-search-response oid-resp)))))
