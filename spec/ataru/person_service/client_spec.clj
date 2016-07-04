(ns ataru.person-service.client-spec
  (:require [aleph.http :as http]
            [ataru.person-service.client :as client]
            [cheshire.core :as json]
            [clj-util.cas :as cas-util]
            [clj-util.client :as client-util]
            [com.stuartsierra.component :as component]
            [manifold.deferred :as d]
            [oph.soresu.common.config :refer [config]]
            [ring.util.http-response :as response]
            [speclj.core :refer :all])
  (:import (fi.vm.sade.utils.cas CasClient)
           (scalaz.concurrent Task)
           (java.io ByteArrayInputStream)))

(def person-search-response
  {:totalCount 2
   :results [{:oidHenkilo "1.2.246.562.24.00000000001"}
             {:oidHenkilo "1.2.246.562.24.00000000002"}]})

(def cas-params (cas-util/cas-params
                  (get-in config [:person-service :url])
                  (get-in config [:cas :username])
                  (get-in config [:cas :password])))

(def cas-url (get-in config [:authentication :cas-client-url]))

(def cas-session-id-1 "173219B0D0FDB30124AD4D4696BC1336")
(def cas-session-id-2 "173219B0D0FDB30124AD4D4696BC1337")
(def current-session-id (atom nil))

(def mock-client (proxy [CasClient] [cas-url client-util/client]
                   (fetchCasSession [_]
                     (let [session-id (if
                                        (nil? @current-session-id)
                                        (reset! current-session-id cas-session-id-1)
                                        (reset! current-session-id cas-session-id-2))]
                       (Task/now session-id)))))

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
                              (let [resp# (-> (apply ~(second bindings) args#)
                                              (update :body #(-> %
                                                                 json/generate-string
                                                                 .getBytes
                                                                 ByteArrayInputStream.)))
                                    ret# (d/deferred)]
                                (d/success! ret# resp#)
                                ret#))]
       ~@body)))

(describe "PersonServiceClient"
  (tags :unit)

  (it "should fetch OIDs for a person"
    (with-mock-api [client (fn [url {:keys [query-params headers]}]
                             (should= (str person-service-url "/authentication-service/resources/henkilo") url)
                             (should= {"q" username} query-params)
                             (should= {"Cookie" (str "JSESSIONID=" cas-session-id-1)} headers)
                             (response/ok person-search-response))]
      (let [oid-resp (.resolve-person-oids client "test-user")]
        (should= person-search-response oid-resp))))

  (it "should initialize new CAS session once if first one is invalid"
    (with-mock-api [client (fn [url {:keys [query-params headers]}]
                             (if
                               (= {"Cookie" (str "JSESSIONID=" cas-session-id-1)} headers)
                               (response/found "about:blank")
                               (do
                                 (should= (str person-service-url "/authentication-service/resources/henkilo") url)
                                 (should= {"q" username} query-params)
                                 (should= {"Cookie" (str "JSESSIONID=" cas-session-id-2)} headers)
                                 (response/ok person-search-response))))]
      (let [oid-resp (.resolve-person-oids client "test-user")]
        (should= person-search-response oid-resp)))))
