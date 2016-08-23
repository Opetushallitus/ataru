(ns ataru.codes-service.postal-code-client-spec
  (:require [aleph.http :as http]
            [ataru.codes-service.postal-code-client :as client]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [manifold.deferred :as d]
            [ring.util.http-response :as response]
            [speclj.core :refer :all])
  (:import [java.io ByteArrayInputStream]))

(defonce postal-code-response (slurp (io/resource "codes_service/postal_code_response.xml")))

(defmacro with-mock-api
  [bindings & body]
  `(let [system# (component/start-system
                   (component/system-map
                     :postal-code-client (client/new-postal-code-client)))
         ~(first bindings) (:postal-code-client system#)]
     (with-redefs [http/get (fn [& args#]
                              (let [resp# (-> (apply ~(second bindings) args#)
                                              (update :body #(-> % .getBytes ByteArrayInputStream.)))
                                    ret# (d/deferred)]
                                (d/success! ret# resp#)
                                ret#))]
       ~@body)))

(describe "postal-code-client"
  (tags :unit)

  (it "should get list of postal codes from codes service"
    (with-mock-api [client (fn [& _]
                             (response/ok postal-code-response))]
      (let [actual (.get-postal-codes client)]
        (should= 2 (count actual))
        (should= {"99400" {:sv "ENONTEKIÖ" :fi "ENONTEKIÖ"}
                  "55120" {:sv "IMATRA" :fi "IMATRA"}}
                 actual))))

  (it "should return postal office names based on postal code"
      (with-mock-api [client (fn [& _]
                               (response/ok postal-code-response))]
        (let [actual (.get-postal-office-name client "99400")]
          (should= {:sv "ENONTEKIÖ" :fi "ENONTEKIÖ"} actual)))))
